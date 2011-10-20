#!/bin/sh
export CLASSPATH=*:../target/*
java -Djavax.net.ssl.trustStore=c3.keystore Shell -t ws -ignoreSSLHostname "$@"