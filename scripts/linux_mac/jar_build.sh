#!/bin/bash

pushd ../../apps

rm -r ./api-gateway/target
rm -r ./chat-api/target
rn -r ./ingestion-worker/target

pushd  ./chat-api
./mvnw -o -Dmaven.repo.local="$HOME/.m2/repository" -Dmaven.test.skip=true package
popd

pushd ./ingestion-worker
./mvnw -o -Dmaven.repo.local="$HOME/.m2/repository" -Dmaven.test.skip=true package
popd

pushd ./api-gateway
./mvnw -o -Dmaven.repo.local="$HOME/.m2/repository" -Dmaven.test.skip=true package
popd

popd