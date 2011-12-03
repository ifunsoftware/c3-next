#!/bin/sh
WORKDIR=$(dirname $(readlink -f $0))
export CLASSPATH=$WORKDIR/../lib/*
java -Djavax.net.ssl.trustStore=c3.keystore Shell -t ws -ignoreSSLHostname "$@"