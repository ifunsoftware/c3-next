#!/bin/bash

VIRGO_PID=$(ps aux | grep java | grep jmxremote | sed -e 's/[ ][ ]*/ /g' | cut -d ' ' -f 2)

if [ "$VIRGO_PID" -eq "$VIRGO_PID" ] 2>/dev/null ; then echo "Found virgo process. Killing $VIRGO_PID" && kill $VIRGO_PID; fi

