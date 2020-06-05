#!/bin/sh
ATTEMPTS=0
until [ $ATTEMPTS -eq 10 ]; do
  echo kubectl get pod deploy-container -o jsonpath='{.status.containerStatuses[0].state.terminated.reason}' -n ${ENV}
  result=`kubectl get pod deploy-container -o jsonpath='{.status.containerStatuses[0].state.terminated.reason}' -n ${ENV}`
  echo "result is $result"
  if test "$result" = 'Error'; then
    echo "FAILED DEPLOY"
    kubectl logs -n ${ENV} deploy-container
    exit 2
  fi
  if test "$result" = 'Completed'; then
    echo "SUCCESSFUL DEPLOY"
    kubectl logs -n ${ENV} deploy-container
    exit 0
  fi
  ATTEMPTS=$(($ATTEMPTS + 1))
  echo Attempts is $ATTEMPTS
  sleep 3
done
echo "failed timeout"
exit 2
