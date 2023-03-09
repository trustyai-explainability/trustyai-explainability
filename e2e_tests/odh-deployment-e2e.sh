#! /bin/bash

# cleanup
for PROJECT in trustyai-e2e trustyai-e2e-modelmesh
do
  oc project $PROJECT
  oc delete $(oc get kfdef -o name)
  oc delete $(oc get deployment -o name)
  oc delete $(oc get pod -o name)
  oc delete $(oc get cm -o name)
  oc delete $(oc get service -o name)
  oc delete $(oc get inferenceservices -o name)
  oc delete $(oc get servingruntime -o name)
  oc delete $(oc get pvc -o name)
  oc delete $(oc get secrets -o name)
done

# init =================================================================================================================
ODH_NAMESPACE=trustyai-e2e
MM_NAMESPACE=trustyai-e2e-modelmesh
oc new-project $ODH_NAMESPACE
oc project $ODH_NAMESPACE

# clone the target branch
[ -d "odh-manifests" ] && rm -Rf odh-manifests
git clone --branch "$2" "https://github.com/$1/odh-manifests.git"

# deploy a minimal ODH install  ========================================================================================
oc apply -f odh-minimal.yaml

# deploy TrustyAI  =====================================================================================================
[ -f "trustyai.yaml" ] && rm trustyai.yaml
cat odh-manifests/trustyai-service/README.md |\
 sed -n "/\`\`\`/,/\`\`\`/p" |\
  tail -n +2 | sed '$d' |\
  sed  "s/uri:.*/uri: https:\/\/api.github.com\/repos\/$1\/odh-manifests\/tarball\/$2/" \
  > trustyai.yaml

oc apply -f trustyai.yaml
oc apply -f trustyai-route.yaml
oc apply -f trusty-pvc.yaml

# wait for TrustyAI to spin up
while [[ -z "$(oc get pods | grep trustyai-service | grep 1/1)" ]]
do
  echo "Waiting on trustyai pod to spin up"
  sleep 5
done


## deploy ModelMesh ====================================================================================================
oc new-project $MM_NAMESPACE
oc label namespace $MM_NAMESPACE "modelmesh-enabled=true" --overwrite=true || echo "Failed to apply modelmesh-enabled label."
oc project $MM_NAMESPACE

oc apply -f secret.yaml
oc apply -f odh-mlserver-0.x.yaml
oc apply -f model.yaml

# wait to spin up ======================================================================================================
while [[ -z "$(oc get pods | grep modelmesh-serving | grep 5/5)" ]]
do
  echo "Waiting on modelserving runtime pods to spin up"
  sleep 5
done

# get + send data to model route  ======================================================================================
oc project $MM_NAMESPACE
INFER_ROUTE=$(oc get route example-sklearn-isvc --template={{.spec.host}}{{.spec.path}})
for i in {1..5};
do
  curl -k https://$INFER_ROUTE/infer -d @data.json
done

echo "\n Waiting for requests to appear in TrustyAI pod logs..."
sleep 30

# see if payloads are in logs  =========================================================================================
oc project $ODH_NAMESPACE
if [[ -z "$(oc logs $(oc get pods -o name | grep trustyai-service) | grep "Received partial input payload")" ]];
then
  echo "ERROR: No payload communication between ModelMesh + TrustyAI"
  exit
fi

# grab + run data generator  ===========================================================================================
[ -d "logger" ] && rm -Rf logger
cp -r ../explainability-service/demo/logger logger
TRUSTY_ROUTE=$(oc get route/trustyai-service-route --template={{.spec.host}})
cd logger

sed "s/sleep(random.randint(1, 3))/sleep(0.01)/" partial.py > partial-fast-gen.py
SERVICE_ENDPOINT=http://$TRUSTY_ROUTE/consumer/kserve/v2 python3 partial-fast-gen.py & sleep 60 ; kill $!
echo "\n"

# setup a metric  chedule===============================================================================================
curl --location http://$TRUSTY_ROUTE/metrics/spd/request \
  --header 'Content-Type: application/json' \
  --data '{
      "modelId": "example-model-1",
      "protectedAttribute": "input-2",
      "favorableOutcome": {
          "type": "DOUBLE",
          "value": 1.0
      },
      "outcomeName": "output-0",
      "privilegedAttribute": {
          "type": "DOUBLE",
          "value": 0.0
      },
      "unprivilegedAttribute": {
          "type": "DOUBLE",
          "value": 1.0
      }
  }'


# print out prometheus route ===========================================================================================
echo "Go to https://$(oc get route odh-model-monitoring --template={{.spec.host}}) and see if 'trusty_spd' is present"
echo "If so, e2e is successful!"


# clean up =============================================================================================================
[ -d "odh-manifests" ] && rm -Rf odh-manifests
[ -d "logger" ] && rm -Rf logger
[ -f "odh-mlserver-0.x.yaml" ] && rm odh-mlserver-0.x.yaml
[ -f "trustyai.yaml" ] && rm trustyai.yaml