#!/bin/sh
echo gcloud container clusters get-credentials --project="featurehub" --zone="${CLOUDSDK_COMPUTE_ZONE}" "${CLOUDSDK_CONTAINER_CLUSTER}"
gcloud container clusters get-credentials --project="featurehub" --zone="${CLOUDSDK_COMPUTE_ZONE}" "${CLOUDSDK_CONTAINER_CLUSTER}"
echo kubectl run -n ${ENV} deploy-container --generator=run-pod/v1 --env=ENV=${ENV} --serviceaccount=deployment-manager --image=gcr.io/featurehub/deploy-container:${BUILD_TAG} --restart=Never
kubectl run -n ${ENV} deploy-container --generator=run-pod/v1 --env=ENV=${ENV} --serviceaccount=deployment-manager --image=gcr.io/featurehub/deploy-container:${BUILD_TAG} --restart=Never
echo waiting for completion
chmod u+x wait_for_completion.sh
sh wait_for_completion.sh

