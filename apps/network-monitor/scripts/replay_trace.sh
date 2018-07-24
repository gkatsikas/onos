#!/bin/bash

ERROR=1
SUCCESS=0

PROGRAM=$0
PORT=$1
TRACE=${2-/home/tom/traces/OUT.pcap}

usage()
{
	echo "Usage: "$PROGRAM" <network interface nane> <path/to/a/trace>"
	echo "e.g.,  "$PROGRAM" eno1 mytrace.pcap"
	exit $ERROR
}

check_input()
{
	if [[ -z $PORT ]]; then
		echo "Please provide a network interface through which the trace will be injected"
		usage
	fi

	if [[ (-z $TRACE) || (! -f $TRACE) ]]; then
		echo "Please provide a valid trace to be injected"
		usage
	fi
}

replay_trace()
{
	sudo tcpreplay -i $PORT $TRACE
}

[[ $# < 1 ]] && usage || :
replay_trace

exit $SUCCESS
