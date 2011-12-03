#!/bin/sh
WORKDIR=$(dirname $(readlink -f $0))
export CLASSPATH=$WORKDIR/../lib/*
java Shell -t ws "$@"