#! /bin/bash

# init =================================================================================================================
ODH_NAMESPACE=trustyai-e2e
MM_NAMESPACE=trustyai-e2e-modelmesh
oc new-project $ODH_NAMESPACE
oc new-project $MM_NAMESPACE

for PROJECT in $ODH_NAMESPACE $MM_NAMESPACE
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
oc apply -f trustyai-route.yaml -n $ODH_NAMESPACE
oc apply -f trusty-pvc.yaml

# wait for TrustyAI to spin up
while [[ -z "$(oc get pods | grep trustyai-service | grep 1/1)" ]]
do
  echo "Waiting on trustyai pod to spin up"
  sleep 5
done


## deploy ModelMesh ====================================================================================================
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

oc project $MM_NAMESPACE
INFER_ROUTE=$(oc get route example-sklearn-isvc --template={{.spec.host}}{{.spec.path}})
while [[ -z "$(curl -k https://$INFER_ROUTE/infer -d @data.json | grep example-sklearn-isvc)" ]]
do
  echo "Wait for modelserving endpoint to begin serving..."
  sleep 5
done

# get + send data to model route  ======================================================================================
for i in {1..5};
do
  curl -k https://$INFER_ROUTE/infer -d @data.json
done

echo
echo "Waiting for requests to appear in TrustyAI pod logs..."
sleep 10

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
echo "TrustyAI Route at $TRUSTY_ROUTE"
cd logger

sed "s/sleep(random.randint(1, 3))/sleep(0.01)/" partial.py > partial-fast-gen.py
SERVICE_ENDPOINT=http://$TRUSTY_ROUTE/consumer/kserve/v2 nohup python3 partial-fast-gen.py &
GENERATOR_PID=$!
echo "Data Generator (PID $GENERATOR_PID) is running; waiting 60 seconds to generate some data..."
echo
cd ..
sleep 60

# see if example model payloads are in logs ============================================================================
oc project $ODH_NAMESPACE
if [[ -z "$(oc logs $(oc get pods -o name | grep trustyai-service) | grep "Received partial input payload")" ]];
then
  echo "ERROR: No payload communication between Data Generator + TrustyAI"
  exit
fi


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
echo
echo
echo "Go to https://$(oc get route odh-model-monitoring --template={{.spec.host}}) and see if 'trusty_spd' is present"
echo "If so, e2e is successful!"
echo "Data generator is still running. Hit enter to kill generator and begin test cleanup:" && read -n 1

# clean up =============================================================================================================
[ -d "odh-manifests" ] && rm -Rf odh-manifests
[ -d "logger" ] && rm -Rf logger
[ -f "trustyai.yaml" ] && rm trustyai.yaml
[ -f "nohup.out" ] && rm nohup.out
kill -9 $GENERATOR_PID