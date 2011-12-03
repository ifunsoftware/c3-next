#!/bin/bash

if [ "$VIRGO_HOME" == "" ]; then
	echo "VIRGO_HOME is not set"
	exit 1
fi;

if [ "$VIRGO_USER" == "" ]; then
	echo "VIRGO_HOME is not set"
	exit 1
fi;

VIRGO_CTL=/etc/init.d/virgoctl

$VIRGO_CTL stop
sleep 10

rm -rf $VIRGO_HOME/repository/usr/*
rm $VIRGO_HOME/pickup/c3*.plan

WORKDIR=$(dirname $(readlink -f $0))

cp $WORKDIR/server/lib* $VIRGO_HOME/repository/usr
chown -c $VIRGO_USER $VIRGO_HOME/repository/usr/*

cp $WORKDIR/server/pickup/* $VIRGO_HOME/pickup
chown -c $VIRGO_USER $VIRGO_HOME/pickup/*

$VIRGO_CTL start
