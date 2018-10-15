#!/bin/bash

java -Xmx4g -cp "target/*" de.julielab.concepts.db.application.ConceptDatabaseApplication $* -c src/main/resources/gene-database.xml
