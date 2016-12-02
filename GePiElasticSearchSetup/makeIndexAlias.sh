#!/bin/bash

# A POSIX variable
OPTIND=1         # Reset in case getopts has been used previously in the shell.

# Initialize our own variables:
index=""
alias=""
host=""
port=""
remove=0

while getopts "i:a:h:p:r" opt; do
    case "$opt" in
    i)  index=$OPTARG
        ;;
    a)  alias=$OPTARG
	;;
    h)  host=$OPTARG
        ;;
    p)  port=$OPTARG
        ;;
    r)  remove=1
	;;
    esac
done

shift $((OPTIND-1))

[ "$1" = "--" ] && shift

# Define variables derived from input parameters
action="add"

if [ -z "$host" ]; then
	host="localhost";
fi
if [ -z "$port" ]; then
	port="9200";
fi
if [ $remove -eq 1 ]; then
	action="remove";
fi

curl -XPOST http://$host:$port/_aliases -d '
{
    "actions": [
        { "'"$action"'": {
            "alias": "'"$alias"'",
            "index": "'"$index"'"
        }}
    ]
}
'
