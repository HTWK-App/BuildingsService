#!/bin/bash
git pull

activator clean docker:stage
cp Dockerfile target/docker/
cp newrelic-java-3.17.0.zip target/docker/newrelic.zip

cd target/docker/
docker build -t rmeissn/buildings:latest ./

cd ../../../
docker-compose up -d --no-deps buldingsService
docker rmi rmeissn/buildings:current
docker tag rmeissn/buildings:latest rmeissn/buildings:current
