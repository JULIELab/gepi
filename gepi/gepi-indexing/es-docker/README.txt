This creates a docker image for a development ElasticSearch
instance that has the preanalyzed plugin installed:
docker build . -t elasticsearch-preanalyzed:<tag>

Create and run a container based on the image:
docker run -d --name elasticsearch-preanalyzed -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" elasticsearch-preanalyzed:<tag>

And, finally, create an index:
curl -XPUT http://localhost:9200/gepi_0.2 -H 'Content-Type: application/json' -d @../gepi-indexing-base/src/main/resources/elasticSearchMapping.json

The index must then be filled with documents. Refer to *GePIN Development* section of the main GePIN README.md in the project root for details.