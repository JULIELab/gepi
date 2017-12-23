#!/bin/bash

java -Xmx4g -cp "target/*" de.julielab.concepts.db.application.ConceptDatabaseApplication $1 src/main/resources/gene-database.xml
