#!/bin/bash

VIRGO_PID=$(ps aux | grep java | grep jmxremote | cut -d ' ' -f 3)

if [ "$VIRGO_PID" -eq "$VIRGO_PID" ] 2>/dev/null ; then echo "Found virgo process. Killing $VIRGO_PID" && kill $VIRGO_PID; fi

