#!/bin/bash

pushd ../../apps

docker build -t finalproject/api-gateway:local -f ./api-gateway/Dockerfile ./api-gateway
docker build -t finalproject/chat-api:local -f ./chat-api/Dockerfile ./chat-api
docker build -t finalproject/ingestion-worker:local -f ./ingestion-worker/Dockerfile ./ingestion-worker
docker build -t finalproject/web-frontend:local -f ./web-frontend/Dockerfile ./web-frontend

popd