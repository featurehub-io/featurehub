#!/bin/sh
export PATH=$PATH:/usr/bin/groovy/bin
echo upgrading namespace
cd /app/resources
echo kubectl config set-context gce --namespace=${ENV}
kubectl config set-context gce --namespace=${ENV}
kubectl config get-contexts
kubectl get secrets
#kubectl config set-context `kubectl config current-context` --namespace=${ENV}
echo helm upgrade featurehub featurehub -f featurehub/${ENV}-values.yaml --debug -i
# this is always causing it to go into pending-install instead of deployed
helm upgrade featurehub featurehub -f featurehub/${ENV}-values.yaml --debug -i
