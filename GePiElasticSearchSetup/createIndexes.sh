#!/bin/bash

MEDLINE_INDEX_SETTINGS=`cat medlineIndexSettings.json`
SUGGESTION_SEARCH_INDEX_SETTINGS=`cat suggestionsSearchIndexSettings.json`
SUGGESTION_COMPLETION_INDEX_SETTINGS=`cat suggestionsCompletionIndexSettings.json`

# A POSIX variable
OPTIND=1         # Reset in case getopts has been used previously in the shell.

# Initialize our own variables:
medlinename=""
host=""
port=""

while getopts "m:h:p:" opt; do
    case "$opt" in
    m)  medlinename=$OPTARG
        ;;
    h)  host=$OPTARG
        ;;
    p)  port=$OPTARG
        ;;
    esac
done

shift $((OPTIND-1))

[ "$1" = "--" ] && shift

if [ -z "$host" ]; then
	host="localhost";
fi
if [ -z "$port" ]; then
	port="9200";
fi
if [ -z "$medlinename" ]; then
	medlinename="documents";
fi


echo "Creating index '$medlinename'."
curl -XPOST "http://$host:$port/$medlinename" -d "$MEDLINE_INDEX_SETTINGS"
echo ""


echo "Creating index 'suggestions' (edge-analyzed)".
curl -XPOST "http://$host:$port/suggestions" -d "$SUGGESTION_SEARCH_INDEX_SETTINGS"
echo ""

echo "Creating index 'suggestions' (completion strategy)".
curl -XPOST "http://$host:$port/suggestions_completion" -d "$SUGGESTION_COMPLETION_INDEX_SETTINGS"
echo ""
