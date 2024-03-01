# GePI: Gene and Protein Interactions

## Purpose of this software

The core of GePI (**Ge**ne and **P**rotein **I**nteractions) is a web application for the user-friendly retrieval of descriptions of biomolecular interactions from the scientific literature, [PubMed](https://pubmed.ncbi.nlm.nih.gov/) (PM) and the [PubMed Central](https://www.ncbi.nlm.nih.gov/pmc/) (PMC) [open access subset](https://www.ncbi.nlm.nih.gov/pmc/tools/openftlist/). To this end, [JCoRe](https://github.com/JULIELab/jcore-base) pipeline components are used to form a number of [UIMA](https://uima.apache.org/) pipelines for the processing of PM and PMC in order to extract the interactions.

The GePI web application is available at https://gepi.coling.uni-jena.de/.

## Reference for this work

GePI is described in

Erik Faessler, Udo Hahn, Sascha Schäuble, GePI: large-scale text mining, customized retrieval and flexible filtering of gene/protein interactions, *Nucleic Acids Research*, Volume 51, Issue W1, 5 July 2023, Pages W237–W242, https://doi.org/10.1093/nar/gkad445

## Overview of databases and processes to create a GePI instance from scratch

A complete, running GePI web application requires the following databases, resources, pipelines and tools to be available.

Databases:
* a [Neo4j](https://neo4j.com/) database (community edition) in version 4.x to store data about genes/proteins and static relationships between them like protein clusters, gene groups, GO annotations, alternative IDs (UniProt, Hugo, ...) etc. in the [gene concept database](#building-the-neo4j-gene-concept-database),
* an [ElasticSearch](https://www.elastic.co/de/elasticsearch) in version 7.x to index interactions for efficient retrieval,
* a PostgreSQL database to hold literature documents and annotations.

Resources:
* database files for genes, gene groups, GO terms that are imported into Neo4j,
* PubMed and PMC XML files.

[UIMA pipelines](#automatic-extraction-of-interactions-from-the-literature):
* interaction extraction NLP pipeline,
* extraction indexing pipeline for indexing into ElasticSearch.

Tools:
* the CoStoSys executable JAR to import PubMed/PMC XML files into the PostgreSQL database,
* (optional) the [JCoRe Pipeline Builder CLI](https://github.com/JULIELab/jcore-pipeline-modules/tree/master/jcore-pipeline-builder-cli) to view or alter the UIMA pipelines.

The dependencies between the main parts are depicted in the image below. The PostgreSQL database has been left out for simplicity.

![Schematic display of GePIs main components and how they depend on each other.](./readme-raw/gepi-webapp.png)


## Building the Neo4j gene concept database
The gene concept database integrates a number of separate resources that have links to each other. They are imported into a specific graph model in a [Neo4j](https://neo4j.com/) graph database.
The database is then used in two functions:
1. data is exported for usage in the NLP interaction extraction pipeline,
2. the GePI web server communicates with the concept database for ID conversion, group and family resolution and other purposes required to run GePI.

For details about building the concept database please refer to the README in the `gepi-concept-database` directory.

## Automatic extraction of interactions from the literature
GePI leverages two kinds of document processing pipelines.
1. NLP preprocessing for basic linguistic annotations - e.g. sentences, tokens, abbreviations - and the primary semantic items of interest, i.e. genes/proteins, families and groups and interactions between them. These pipelines are located in the `gepi-preprocessing` folder.
2. Transformation of the linguistic annotations into an ElasticSearch index for efficient retrieval. The respective pipelines can be found in the `gepi-indexing` directory.

The pipelines are built using [UIMA](https://uima.apache.org/) pipelines comprised of [JCoRe](https://github.com/JULIELab/jcore-base) components. They can be most easily be viewed and edited via the [JCoRe Pipeline Builder CLI](https://github.com/JULIELab/jcore-pipeline-modules/tree/master/jcore-pipeline-builder-cli) that has been used to create the pipelines.

All data is managed with [JeDIS](https://julielab.de/jedis/). This means that text documents as well as UIMA annotations are stored in a PostgreSQL database. The JCoRe readers and writers used in the pipelines read from and write into the database in a specific format that is specified through [CoStoSys](https://github.com/JULIELab/costosys). To use the existing pipelines, no specific knowledge of JeDIS or CoStoSys is required. A PostgreSQL database is needed, though, and the CoStoSys configuration files - named `costosys.xml` and placed in the `config/` subdirectories of the pipelines - must be adapted to use that database.

See the respective pipeline subdirectories for more information.

## Running the web application



The GePI web application is a Java servlet application built on the [Apache Tapestry](https://tapestry.apache.org/) web framework. It can be run in any servlet container like [Apache Tomcat](https://tomcat.apache.org/), [Eclipse Glassfish](https://glassfish.org/) or [Eclipse Jetty](https://www.eclipse.org/jetty/). GePI has always been run within Jetty. For modularization and deployment simplicity, GePI is preferred to run in a [Docker](https://www.docker.com/) environment. This is not a strict requirement but the GePI documentation describes Docker deployments.

Note: From hereon, the directory `gepi` refers to the Maven project root directory where this README file lies in, not the `gepi` Git root directory.

The `gepi` directory contains a `Dockerfile` that specifies two build stages that can be used to run the GePI web application, named `development` and `production`. The `development` stage does not contain the application code itself but is mean to use a bind mount to refer to the code on the host. The advantage of this approach is that development can be done normally but the deployment environment is very similar to the production environment.

The `production` stage expects that the complete GePI project has been built in its latest code version. The web archive (WAR) file of the web application at `gepi-webapp/target` is then deployed into Jetty that is located within the Docker image.

### Using the `development` Docker image

Run the following commands to create a `development` container:

```bash
DOCKER_BUILDKIT=1 docker build -t gepi-dev:${project.version} --target development .
docker run -dp 8080:8080 -v {/path/to/gepi/directory}:/var/gepi/dev -e GEPI_CONFIGURATION=<path to config file> --name gepi-dev gepi-dev:${project.version}
```

The first command builds an image of the `development` stage. This will also build the `dependencies` stage where all the Java dependencies of the GePI application are downloaded and cached. This will take a while on the first execution but should be faster afterwards thanks to caching.

The second command created the Docker container. The `-v` option creates a bind mount that connects the local code to the container. Development can proceed normally and changes will be reflected in the container. The `-e` parameter sets the configuration file to use. This file is necessary to connect to ElasticSearch and Neo4j for interaction and gene concept data. Refer to the [configuration section](#configuring-the-web-application) for details.

<div style="border: solid 2px black; width: 550px; padding:1em; margin-left: 100px">Hint: When the ElasticSearch or Neo4j servers are running on the host machine, use <code>host.docker.internal</code> to connect to them.</div>

Navigate to `http://localhost:8080` in your browser, and you should see the GePI start page.
 

### Using the `production` Docker image

To run the `production` container, run

```bash
mvn clean package --projects gepi-webapp --also-make
DOCKER_BUILDKIT=1 docker build -t gepi:${project.version} --target production .
docker run -dp 8080:8080 --name gepi gepi:${project.version}
```

These commands
* build the WAR archive
* create the Docker image
* create and run a Docker container that forwards the internal port 8080 to the host port 8080

Navigate to `localhost:8080` in your browser and you should see the landing page of GePI.
In actual production, use `dp 80:8080` to bind the Jetty web server in the container to the default HTTP port on the host.

### Configuring the web application

Configuration of the GePI web application happens through a configuration file. The format of the file follows default Java properties files, i.e. key-value pairs separated with `=`. In Tapestry, the keys are called *symbols*. The available configuration symbols are

```properties
elasticquery.clustername=<name of the ES cluster to connect to>
elasticquery.url=<actually not a url but the host IP>
elasticquery.port=9200
gepi.documents.index.name=<interaction index name in ES>
elasticquery.sockettimeout=-1

gepi.neo4j.bolt.url=bolt://<host>:<port>
```

### Running the Docker image in Production

A production environment has a few requirements that are of lesser importance during development. This section explains requirements and solutions that may come up during GePI deployment with the Docker container. While detailed explanations come below, the full Docker `run` command we use for deployment looks like the following:
```
docker run -dp 80:8080 -p 443:8443 -v /host/path/to/certificate.p12:/var/lib/jetty/etc/keystore.p12 -v /host/path/to/configuration.properties:/gepi-webapp-configuration.properties --add-host=host.docker.internal:host-gateway --name gepi -e GEPI_CONFIGURATION=/gepi-webapp-configuration.properties gepi:${project.version} jetty.sslContext.keyStorePassword=<changeit>
```
Alternatively, the `docker-compose-webapp.yml` file can be used with a few additions.

#### Activating HTTPS

In production, most the `HTTPS` will probably be used. By default, GePI sets the Tapestry configuration to always use either HTTP or HTTPS, not mixing the protocols. Using HTTPS requires a keystore containing a valid certificate for the domain GePI is offered at, and the private key that was used to sign the certificate signing request that was sent to the certificate authority.
GePI was developed and deployed using Jetty so the following instructions have been tested with Jetty, version 10.x. 
Create they keystore with openSSL with the command below. This will prompt for a password. A password must be specified because it appears that Jetty will throw NPEs otherwise (not tested with GePI). For the sake of these instructions, we assume the password is `changeit`. For our deployment, we used the certificate including the certificate chain.
```
openssl pkcs12 -export -in <your certificate /w chain>.cer -inkey <your private CSR key>.pem \
               -out <your gepi domain>.p12 -name <your gepi domain> \
               -CAfile ca.crt -caname root
```
The resulting P12 keystore can directly be used with Jetty. Jetty needs either to be told the location of the keystore or the keystore just needs to be put into Jetty's default keystore location. We use the second option in our GePI deployment. To add the keystore file to a new container, use a `Docker volume`:
`-v /host/path/to/certificate.p12:/var/lib/jetty/etc/keystore.p12`. The Dockerfile already contains commands to add SSL and HTTPS to Jetty. Finally, the keystone password must be given to Jetty. There are several possibilities to do this. For a Docker container, the easiest way is to specify the password on container creation, if the Docker host is not accessible by unauthorized persons. Append `jetty.sslContext.keyStorePassword=changeit` to the Docker `run` command to pass the password to Jetty.

#### Connecting to Services Running on the Docker Host

Running the container in a production environment can be done either via the `docker-compose.yml` file or as an isolated service without other docker components. In the latter case, the Neo4j and ElasticSearch servers may run on the same or on other machines. If they run on the host of the Docker container, the container host can be addressed as `host.docker.internal`. On Linux machines, this will only work if the `--add-host=host.docker.internal:host-gateway` parameter is specified in the Docker `run` command.

#### Providing an external configuration file 
By default, the project-internal configuration file `configuration.properties.jetty` is used to configure GePI. This file can be edited before image creation which makes the specification of an external configuration file unnecessary. However, if the official GePI image from Docker Hub is used or the image should not be re-created, an external configuration file can be helpful since it does not require the container to change. The GePI configuration file can be given in the environment variable `GEPI_CONFIGURATION` which is passed to the Docker `run` command using the `-e` switch: `-e GEPI_CONFIGURATION=/gepi-webapp-configuration.properties`. Of course, the configuration file must be placed at this location first. A Docker volume can bind an external file into the container: `-v /host/path/to/configuration.properties:/gepi-webapp-configuration.properties`.


## GePI development

Important note: ***Do not edit the `README.md` file in the module roots*** if there exists a `readme-raw` subdirectory. The file in the root is just a Maven-filtered copy of the `readme-raw/README.md` file. The Maven filtering replaces Maven properties like the project version in the `readme-raw/README.md` file and puts the result in the module root, overriding the previous `README.md` file.

### Update version

Update the new version number in the following places:
* `pom.xml` files (tip: use `mvn versions:set -DnewVersion=<new version>` and `mvn versions:commit` to remove the backup files)
* `README.md` (by executing `mvn clean package -DskipTests=true` to filter the `readme-raw/README.md` file to automatically set the current version to the `README.md` file)
* `AppModule.java` in `gepi-webapp`
  * set `PRODUCTION_MODE` to true for releases
* the Docker image version in the `docker-compose.yml`
* the DB version in `gene-database.xml` in the `gepi-concept-database` module
* execute `python ../../jcore-misc/jcore-scripts/createMetaDescriptors.py -c -i -r manual -v 1.0 gepi-indexing/gepi-indexing-base` given that `jcore-misc` has been cloned to the same base directory as GePI
  * this updates the description file for the use with the JCoRe pipeline builder