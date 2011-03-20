#!/bin/sh
export CLASSPATH=*:../target/*
java Shell -t ws "$@"