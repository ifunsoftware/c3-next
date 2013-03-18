#!/bin/bash

function find_virgo {
    local virgo_pid=$(ps aux | grep java | grep jmxremote | sed -e 's/[ ][ ]*/ /g' | cut -d ' ' -f 2);
    echo ${virgo_pid}
}

VIRGO_PID=$(find_virgo)

if [ "$VIRGO_PID" -eq "$VIRGO_PID" ] 2>/dev/null ; then
    echo "Found virgo process. Killing $VIRGO_PID" && kill $VIRGO_PID;
    sleep 5

    VIRGO_PID=$(find_virgo)
    if [ "$VIRGO_PID" -eq "$VIRGO_PID" ] 2>/dev/null ; then
        echo "Virgo is still alive. Terminating" && kill -9 $VIRGO_PID
    fi;
fi

