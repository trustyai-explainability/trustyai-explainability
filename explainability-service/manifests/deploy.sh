oc delete project $(oc project -q);
oc new-project $1;
oc create -f pvc.yaml
oc apply -f kfdef.yaml
