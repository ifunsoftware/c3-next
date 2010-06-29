@echo off
java -Djavax.net.ssl.trustStore=c3.keystore -jar ../target/c3-client-1.0.*.jar -t ws -ignoreSSLHostname %*