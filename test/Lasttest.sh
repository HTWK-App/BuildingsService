#!/bin/bash

locust --host=http://127.0.0.1:9000 &
xdg-open http://127.0.0.1:8089
