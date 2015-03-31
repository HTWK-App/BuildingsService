#!/bin/bash
activator clean
activator docker:dockerGenerateContext

cp Dockerfile target/docker/
cp newrelic-java-3.14.0.zip target/docker/newrelic.zip
cp NewRelicLicense target/docker/license

cd target/docker/

sudo docker build -t rmeissn/buildings ./

