#!/bin/bash
HOST=$1
PORT=$2
if [ -z "$HOST" ]; then
	HOST="localhost";
fi
if [ -z "$PORT" ]; then
	PORT="9200";
fi

echo "Connecting to ElasticSearch at http://$HOST:$PORT"

curl -XPOST http://$HOST:$PORT/medline/docs/_search -d '{
	"query": {
		"match_all":{}
	},
	"size":0,
	"aggs":{
		"pmidAgg":{
			"terms":{
				"field":"pubmedID",
				"size":0
			}
		}
	}
}' > pmidAgg.json
grep -E -o '"key":"[0-9]+"' pmidAgg.json | sed -E 's/[^0-9]*([0-9]+).*/\1/' > pmids.lst