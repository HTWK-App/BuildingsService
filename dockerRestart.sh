#!/bin/bash

sudo docker stop buildings1
sudo docker run -d -p 9000:9000 --name buildings1 rmeissn/buildings
