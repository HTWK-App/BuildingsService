#!/bin/bash
activator clean docker:stage

cp Dockerfile target/docker/
cp newrelic-java-3.17.0.zip target/docker/newrelic.zip

cd target/docker/

docker build -t rmeissn/buildings ./

