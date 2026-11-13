#!/bin/bash

pushd ../../deployments/k8s/deployments

kubectl apply -f api-gateway.yaml -f chat-api.yaml -f ingestion-worker.yaml -f web-frontend.yaml

popd