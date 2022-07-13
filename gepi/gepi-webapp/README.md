# GePi WebApp

NOTE: Edits to the README.md file must be done to the file in the `readme-raw` folder! The README.md file in the project root is a Maven-filtered copy of the file in `readme-raw` for automatic insertion of the current version. 

## Webapp Deployment

GePi is basically a Java servlet web application. Realized with Apache Tapestry, it follows the conventions of packaging into a WAR file which is then deployed into a servlet container, e.g. Jetty or Tomcat.

### Deploying as a Docker Container

Requisites:
* JDK for Java >= 11
* Docker

The easiest way to get a running GePi webapp is the usage of Docker. For this purpose, a Dockerfile resides within the `gepi-webapp` folder. It starts from an official Jetty container and just puts the GePi WAR file into the appropriate folder. To create a Docker image, the following steps are performed:

* build the WAR archive
* create the Docker image

To build the WAR, use the Maven build tool. From within the `gepi-webapp` folder, use `mvn clean package -f ../pom.xml --projects gepi-webapp --also-make`
<div style="border: 4px solid #77C0CA; margin-left:50px; border-radius: 20px; width: 400px;padding: 10px">Of course you can also build the project in the GePi root directory. Then you just need to omit the <textit style="background-color:#F3F6F8">-f ../pom.xml</textit> portion.</div>

After a successful Maven build, navigate to the `gepi-webapp` directory and execute the following command: `docker build -t gepi:0.8.0-SNAPSHOT .`

To create an app container, type `docker run -dp 8080:8080 gepi:0.8.0-SNAPSHOT`. Navigate to `localhost:8080` in your browser and you should see the landing page of GePi.

Note however, the searching will only work if the Neo4j and ElasticSearch services are available at the locations specified in `src/main/resources/configuration.properties.jetty`.

## Required Python Packages for Result Excel Sheet Creation

* openpyxl v3.0.9
* xlsxwriter v3.0.1