# GePI: Gene and Protein Interactions

## Purpose of this software

The core of GePI is a web application for the user-friendly retrieval of descriptions of biomolecular interactions from the scientific literature, [PubMed](https://pubmed.ncbi.nlm.nih.gov/) (PM) and the [PubMed Central](https://www.ncbi.nlm.nih.gov/pmc/) (PMC) [open access subset](https://www.ncbi.nlm.nih.gov/pmc/tools/openftlist/). To this end, [JCoRe](https://github.com/JULIELab/jcore-base) pipeline components are used to form a number of [UIMA](https://uima.apache.org/) pipelines for the processing of PM and PMC in order to extract the interactions.

## Running the web application

The GePI web application is a Java servlet application built on the [Apache Tapestry](https://tapestry.apache.org/) web framework. It can be run in any servlet container like [Apache Tomcat](https://tomcat.apache.org/), [Eclipse Glassfish](https://glassfish.org/) or [Eclipse Jetty](https://www.eclipse.org/jetty/). GePI has always been run within Jetty. For modularization and deployment simplicity, GePI is preferred to run in a [Docker](https://www.docker.com/) environment. This is not a strict requirement but the GePI documentation describes Docker deployments.

Note: From hereon, the directory `gepi` refers to the Maven project root directory where this README file lies in, not the `gepi` Git root directory.

The `gepi` directory contains a `Dockerfile` that specifies two build stages that can be used to run the GePI web application, named `development` and `production`. The `development` stage does not contain the application code itself but is mean to use a bind mount to refer to the code on the host. The advantage of this approach is that development can be done normally but the deployment environment is very similar to the production environment.

The `production` stage expects that the complete GePI project has been built in its latest code version. The web archive (WAR) file of the web application at `gepi-webapp/target` is then deployed into Jetty that is located within the Docker image.

### Using the `development` Docker image

Run the following commands to create a `development` container:

```bash
DOCKER_BUILDKIT=1 docker build -t gepi:${project.version} --target development .
docker run -dp 8080:8080 -v {/path/to/gepi/directory}:/var/gepi/dev -e GEPI_CONFIGURATION=<path to config file> gepi:${project.version}
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

gepi.neo4j.bolt.url=bolt://<host>:<port>
```

## GePI development

Important note: ***Do not edit the `README.md` file in the module roots*** if there exists a `readme-raw` subdirectory. The file in the root is just a Maven-filtered copy of the `readme-raw/README.md` file. The Maven filtering replaces Maven properties like the project version in the `readme-raw/README.md` file and puts the result in the module root, overriding the previous `README.md` file.

### Update version

Update the new version number in the following places:
* `pom.xml` files (tip: use `mvn versions:set -DnewVersion=<new version>` and `mvn versions:commit` to remove the backup files)
* `README.md` (by executing `mvn clean package -DskipTests=true` to filter the `readme-raw/README.md` file to automatically set the current version to the `README.md` file)
* `AppModule.java` in `gepi-webapp`
* the Docker image version in the `docker-compose.yml`
* the DB version in `gene-database.xml` in the `gepi-concept-database` module
* in `gepi-indexing-base` execute `python ../../../../jcore-misc/jcore-scripts/createMetaDescriptors.py -c -i -r manual -v 1.0 .` given that `jcore-misc` has been cloned to the same directory as GePI
  * this updates the description file for the use with the JCoRe pipeline builder