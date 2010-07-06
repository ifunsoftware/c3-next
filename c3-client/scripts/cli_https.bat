@echo off
set CLASSPATH=*;../target/*
java -Djavax.net.ssl.trustStore=c3.keystore Shell -t ws -ignoreSSLHostname %*