#!/bin/bash

if [ "$VIRGO_HOME" == "" ]; then
	echo "VIRGO_HOME is not set"
	exit 1
fi;

REVISION="$1"

if [ "$REVISION" == "" ]; then
	echo "Revision is not set"
	exit 1
fi;

VIRGO_CTL=/etc/init.d/virgoctl

rm -rf /tmp/c3-tmp
mkdir /tmp/c3-tmp

cd /tmp/c3-tmp
wget http://$C3_BUILD_SERVER/builds/c3/$REVISION/c3-all-1.0.$REVISION.zip
unzip c3-all-1.0.$REVISION.zip

$VIRGO_CTL stop
sleep 10

rm -rf $VIRGO_HOME/repository/usr/*
rm $VIRGO_HOME/pickup/c3*.plan

cp jars/* $VIRGO_HOME/repository/usr
chown -c $VIRGO_USER $VIRGO_HOME/repository/usr/*

cp plan/c3.plan $VIRGO_HOME/pickup
chown -c $VIRGO_USER $VIRGO_HOME/pickup/*

rm -rf /tmp/c3-tmp

$VIRGO_CTL start
sleep 5
