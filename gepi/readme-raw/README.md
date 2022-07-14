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
docker build -t gepi:${project.version} --target development .
docker run -dp 8080:8080 -v {/path/to/gepi/directory}:/var/gepi/dev gepi:${project.version}
```

The first command builds an image of the `development` stage. This will also build the `dependencies` stage where all the Java dependencies of the GePI application are downloaded and cached. This will take a while on the first execution but should be faster afterwards thanks to caching.

The second command created the Docker container. The `-v` option creates a bind mount that connects the local code to the container. Development can proceed normally and changes will be reflected in the container.

### Using the `production` Docker image

To run the `production` container, run

```bash
mvn clean package --projects gepi-webapp --also-make
docker build -t gepi:${project.version} --target production .
docker run -dp 8080:8080 gepi:${project.version}
```

These commands
* build the WAR archive
* create the Docker image
* create and run a Docker container that forwards the internal port 8080 to the host port 8080

Navigate to `localhost:8080` in your browser and you should see the landing page of GePI.
In actual production, use `dp 80:8080` to bind the Jetty web server in the container to the default HTTP port on the host.
