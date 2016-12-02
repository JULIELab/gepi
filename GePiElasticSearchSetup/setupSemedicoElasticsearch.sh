#############################################
#
# NOTE: Works for ElasticSearch version 2.1.1
#
##############################################

if [ $# -eq 0 ]; then
    echo "Usage: $0 <path to fresh downloaded and extracted ElasticSearch directory>"
    exit 1
fi

if [ ! -d "$1" ]; then
    echo "ElasticSearch directory '$1' does not exist.";
    exit 1
fi

ES_PID=`pgrep -f ElasticSearch`
if [ -n "$ES_PID" ]; then
		echo "ElasticSearch is already running at PID $ES_PID. Please shutdown the old installation first."
		exit 1
fi

ELASTICSEARCH_DIR=$1

INDEX_ANALYSER_SETTINGS=indexAnalyzerSettings.yml
CLUSTER_NAME="cluster.name: semedicoDev"
MANDATORY_PLUGINS="plugin.mandatory: elasticsearch-mapper-preanalyzed,analysis-icu"


#mkdir $ELASTICSEARCH_DIR/plugins
#mkdir $ELASTICSEARCH_DIR/plugins/elasticsearch-mapper-preanalyzed
#echo "Copying plugin 'elasticsearch-mapper-preanalyzed' into plugins directory."
#scp -r dawkins:/export/data/essentials/semedico/elasticsearch/elasticsearch-mapper-preanalyzed/elasticsearch-mapper-preanalyzed-0.1.1.jar $ELASTICSEARCH_DIR/plugins/elasticsearch-mapper-preanalyzed/.

echo "Setting cluster name to $CLUSTER_NAME."
echo $CLUSTER_NAME >> $ELASTICSEARCH_DIR/config/elasticsearch.yml

echo "Setting mandatory plugins: $MANDATORY_PLUGINS."
echo $MANDATORY_PLUGINS >> $ELASTICSEARCH_DIR/config/elasticsearch.yml

echo "Appending index analyzer settings to elasticsearch.yml."
cat $INDEX_ANALYSER_SETTINGS >> $ELASTICSEARCH_DIR/config/elasticsearch.yml

echo "Adding ElasticSearch configuration"
cat configuration.yml >> $ELASTICSEARCH_DIR/config/elasticsearch.yml

ES_MAPPER_PREANALZED=elasticsearch-mapper-preanalyzed-2.1.1.zip
echo "Installing 'elasticsearch-mapper-preanalyzed' plugin from JULIE NFS"
scp dawkins:/var/data/semedico/elasticsearch/$ES_MAPPER_PREANALZED .
$ELASTICSEARCH_DIR/bin/plugin install file:$ES_MAPPER_PREANALZED
rm $ES_MAPPER_PREANALZED

echo "Installing ICU plugin."
$ELASTICSEARCH_DIR/bin/plugin install analysis-icu

echo "Installing DeleteByQuery plugin."
$ELASTICSEARCH_DIR/bin/plugin install delete-by-query

echo "Installing ElasticSearch HQ site plugin."
$ELASTICSEARCH_DIR/bin/plugin install royrusso/elasticsearch-HQ

echo "Installing ElasticSearch kopf site plugin."
$ELASTICSEARCH_DIR/bin/plugin install lmenezes/elasticsearch-kopf

echo "Copying resources like ICU rules and stopwords."
scp -r dawkins:/export/data/essentials/semedico/elasticsearch/resources/ $ELASTICSEARCH_DIR

echo "Starting up ElasticSearch for index configuration."
$ELASTICSEARCH_DIR/bin/elasticsearch -d -p pid
sleep 15

./createIndexes.sh

sleep 10

echo "Shutting down ElasticSearch."
kill `cat pid`

echo "Done."
