# GePI Gene Concept Database
The gene concept database is a Neo4j 4.x database server with the [JulieLab Neo4j Concepts Server Plugin](https://github.com/JULIELab/julielab-neo4j-server-plugins/tree/master/julielab-neo4j-plugins-concepts) installed.

To import all data into Nep4j, the database server requires a significant amount of memory. We can't say how much memory is required exactly. 80GB are enough in any case, lower numbers might still work.
After the import the database can be run with lower memory availability. Performance-wise it is best to allocate an amount of memory to the database that at least equals the size of the final database which is around 55GB. Then, the database data can be held in memory which results in a significant performance boost in contrast to reading from disk every time.

The following steps set up the concept database:
1. Run the `getDatabaseImportResources.sh` script to download the used gene databases, e.g. [NCBI Gene](https://www.ncbi.nlm.nih.gov/gene), [FamPlex](https://github.com/sorgerlab/famplex), [HGNC Groups and Families](https://www.genenames.org/data/genegroup/#!/), ID mappings and more. See the script for the full list. This step also does some ID conversion of FamPlex.
2. Build the `gepi-concept-database` project by running `mvn clean package`.
3. Download Neo4j 4.x and install the [Neo4j Concepts Server Plugin](https://github.com/JULIELab/julielab-neo4j-server-plugins/tree/master/julielab-neo4j-plugins-concepts).
4. Start the Neo4j server. Make sure it can be reached. When connecting to Neo4j on a remote host, the Neo4j configuration must be set to listen to `0.0.0.0`.
5. Edit the `connection` element of `src/main/resources/gene-database.xml` to refer to the running Neo4j server.
6. Run `java -jar target/gepi-concept-database-*.jar -c src/main/resources/gene-database.xml -a` to perform all steps set in the `gene-database.xml` configuration file. Alternatively, you may use the `createdb.sh` script. This will take several hours.
