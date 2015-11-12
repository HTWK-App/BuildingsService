#!/bin/bash
git pull

activator clean docker:stage
cp Dockerfile target/docker/
cp newrelic-java-3.17.0.zip target/docker/newrelic.zip

echo "Remember to run Docker-Compose on HTWK-App Server or, for local tests:\n\n"
echo "cd target/docker/\n"
echo "docker build -t rmeissn/buildings:latest ./\n"
echo "docker run -d -p 9000:9000 rmeissn/buildings:latest\n\n"
echo "Don't forget to delete old docker images/containers!\n"
