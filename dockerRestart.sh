#!/bin/bash

docker stop buildings1
docker run -d -p 9000:9000 --name buildings1 rmeissn/buildings
