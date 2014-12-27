#!/bin/sh
#
# This script builds C3 artifacts, and then creates and starts new Docker C3 image.
#

# Abort on first error
set -e 

# Build app binaries
#mvn install

# Stop all currently running containers with c3-next image
#docker ps | grep "c3-next:latest" | awk '{ print $1 }' | xargs docker stop

# Prepare a new Docker image
#docker build -t c3-next c3-deploy/target/docker/

get_docker_ip() {
    DOCKER_PROTO=$(echo $DOCKER_HOST | cut -d ':' -f 1)
    if [ "$DOCKER_PROTO" = "tcp" ] ; then 
	echo $(echo $DOCKER_HOST | cut -d ':' -f 2 | sed -e 's/\///g')
    else
	if [ "$(uname)" == "Darwin" ]; then
        	# Mac OS X platform
        	# Assuming using boot2docker
       		boot2docker ip 2> /dev/null
    	else
        	# Probably Linux platform
        	echo "127.0.0.1"
    	fi
    fi
}

# Run Docker container with new binaries
docker run -d -p 8080:8080 -p 7375:7375 -p 8443:8443 -p 8022:22 c3-next \
    && echo "[Success] C3 container is started. Web CLI will be available shortly at http://$(get_docker_ip):8080/manage/" \
    || echo "[Error] Failed to start C3 container"
