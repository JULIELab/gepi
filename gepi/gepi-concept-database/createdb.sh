#!/bin/bash

java -Xmx80g -cp "target/*" de.julielab.concepts.db.application.ConceptDatabaseApplication $* -c src/main/resources/gene-database.xml
