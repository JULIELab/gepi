#!/bin/bash
# This script checks if there are documents in the JeDIS Postgres database which are missing from the
# GePI ElasticSearch interaction index. Such documents should be reset for new processing in the JeDIS database.
# Takes one parameter:
# 1. Path to a file that defines the following environment variables:
# - DBNAME_PUBMED: Name of the Postgres database where the PubMed JeDIS data for GePI is located.
# - USER_PUBMED: Username for the PubMed JeDIS (Postgres) database.
# - PASSWORD_PUBMED: Password for the PubMed JeDIS (Postgres) database.
# - HOST_PUBMED: Host of the PubMed JeDIS (Postgres) database.
# - PORT_PUBMED: Port of the PubMed JeDIS (Postgres) database.
# - DBNAME_PMC: Name of the Postgres database where the PMC JeDIS data for GePI is located.
# - USER_PMC: Username for the PMC JeDIS (Postgres) database.
# - PASSWORD_PMC: Password for the PMC JeDIS (Postgres) database.
# - HOST_PMC: Host of the PMC JeDIS (Postgres) database.
# - PORT_PMC: Port of the PubMed JeDIS (Postgres) database.
# - ES_INDEX: Name of the GePI ElasticSearch interaction index
# - (Optional) ES_URL: The URL to ElasticSearch. Defaults to http://localhost:9201
source ~/.gepi-validation
export PGPASSWORD=$PASSWORD_PUBMED
echo "Writing PubMed IDs with EventMentions in the JeDIS database to pmid_pg.txt"
psql -qtA -h $HOST_PUBMED -p $PORT_PUBMED -U $USER_PUBMED $DBNAME_PUBMED -c "SELECT pmid FROM _data_xmi.documents WHERE gnormplusbiosem\$de_julielab_jcore_types_eventmention IS NOT NULL" > pmid_pg.txt
export PGPASSWORD=$PASSWORD_PMC
echo "Writing PMC IDs with EventMentions in the JeDIS database to pmcid_pg.txt"
psql -qtA -h $HOST_PMC -p $PORT_PMC -U $USER_PMC $DBNAME_PMC -c "SELECT pmcid FROM _data_xmi.documents WHERE gnormplusbiosem\$de_julielab_jcore_types_eventmention IS NOT NULL" > pmcid_pg.txt

# This script pulls the document IDs from ElasticSearch and Postgres in an effort to make sure that every
# document in the JeDIS database (Postgres) arrived in ElasticSearch.
if [ -z "$ES_URL" ]; then
  ES_URL="http://localhost:9201"
fi
HEADER="-H Content-Type:application/json"
curl -XPOST $ES_URL/$ES_INDEX/_search $HEADER -d '{
	"query": {
      "match": {
        "source": "pubmed"
      }
  },
  "size": 0,
	"aggs": {
		"pmids": {
			"terms": {
				"field": "pmid",
				"size": 10000000
			}
		}
	}
}' > pmid_es_docid_aggregation.json
curl -XPOST $ES_URL/$ES_INDEX/_search $HEADER -d '{
	"query": {
      "match": {
        "source": "pmc"
      }
  },
  "size": 0,
	"aggs": {
		"pmcids": {
			"terms": {
				"field": "pmcid",
				"size": 10000000
			}
		}
	}
}' > pmcid_es_docid_aggregation.json
grep -oE 'key":"[0-9]+' pmid_es_docid_aggregation.json | grep  -oE '[0-9]+' > pmid_es.txt
grep -oE 'key":"PMC[0-9]+' pmcid_es_docid_aggregation.json | grep  -oE 'PMC[0-9]+' > pmcid_es.txt

echo "PubMed: Got `wc -l pmid_pg.txt` IDs from Postgres and `wc -l pmid_es.txt` from ElasticSearch"
echo "PMC: Got `wc -l pmcid_pg.txt` IDs from Postgres and `wc -l pmcid_es.txt` from ElasticSearch"

cat pmid_es.txt pmid_pg.txt | sort | uniq -u > pmid_missing.txt
cat pmcid_es.txt pmcid_pg.txt | sort | uniq -u > pmcid_missing.txt

echo "Missing PubMed: Got `wc -l pmid_missing.txt` unique doc IDs; assuming those are missing from ElasticSearch"
echo "Missing PMC: Got `wc -l pmcid_missing.txt` unique doc IDs; assuming those are missing from ElasticSearch"

