#!/bin/bash
if [ -d testdata-db ]; then
	rm -r testdata-db
fi
if [ -d ../gepi-indexing/gepi-indexing-testdata/resources/es-consumer-resources ]; then
	rm -r ../gepi-indexing/gepi-indexing-testdata/resources/es-consumer-resources
fi
java -cp "target/*" de.julielab.concepts.db.application.ConceptDatabaseApplication -c src/main/resources/gene-test-database.xml -a
rm -r testdata-db
gzip es-consumer-resources/*
mv es-consumer-resources ../gepi-indexing/gepi-indexing-testdata/resources/
