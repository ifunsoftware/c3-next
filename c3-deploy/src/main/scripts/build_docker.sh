#!/bin/bash

docker build -t ifunsoftware/c3-next:snapshot target/docker

docker images | grep '^<none' | awk '{ print $3 }' | xargs docker rmi

