#!/bin/sh
# use this when featurehub is loaded into kind
export INGRESS_IP=$(kubectl get ingress featurehub -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
./run.sh
