#!/bin/sh
echo gcloud container clusters get-credentials --project="featurehub" --zone="${CLOUDSDK_COMPUTE_ZONE}" "${CLOUDSDK_CONTAINER_CLUSTER}"
gcloud container clusters get-credentials --project="featurehub" --zone="${CLOUDSDK_COMPUTE_ZONE}" "${CLOUDSDK_CONTAINER_CLUSTER}"
echo kubectl delete pod deploy-container -n ${ENV} || exit 0
kubectl delete pod deploy-container -n ${ENV} || exit 0
