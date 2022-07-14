# syntax=docker/dockerfile:1

# This files defines three build layers, two of which are supposed to be used to run the GePI webapp.
# The first usable stage is "development". It uses the "dependencies" layer which exists mainly for caching reasons.
# The development stage is meant to include the actual code as a bind mount: the webapp is run in the container
# but the data lies on the host.
# The second usable stage is "production". For this one, the whole GePI project must have been freshly built
# to create the WAR file in gepi-webapp/target. This is then deployed into Jetty's webapps/ directory. Thus,
# the webapp is contained in the production image and does not rely on data on the host.

FROM maven:alpine as dependencies
WORKDIR /var/gepi/dev
COPY maven-settings-docker.xml ./
COPY pom.xml ./
COPY gepi-concept-database/pom.xml gepi-concept-database/pom.xml
COPY gepi-core/pom.xml gepi-core/pom.xml
COPY gepi-indexing/pom.xml gepi-indexing/pom.xml
COPY gepi-indexing/gepi-indexing-base/pom.xml gepi-indexing/gepi-indexing-base/pom.xml
COPY gepi-indexing/gepi-indexing-debug/pom.xml gepi-indexing/gepi-indexing-debug/pom.xml
COPY gepi-indexing/gepi-indexing-pmc/pom.xml gepi-indexing/gepi-indexing-pmc/pom.xml
COPY gepi-indexing/gepi-indexing-pubmed/pom.xml gepi-indexing/gepi-indexing-pubmed/pom.xml
COPY gepi-indexing/gepi-indexing-testdata gepi-indexing/gepi-indexing-testdata
COPY gepi-test-data gepi-test-data
COPY gepi-webapp/pom.xml gepi-webapp/pom.xml
# Install the test data so we can copy it for the development image. Otherwise, it will be a missing dependency when
# using jetty:run.
RUN mvn --settings maven-settings-docker.xml --projects gepi-test-data clean install
RUN --mount=type=cache,target=/root/.m2 mvn --settings maven-settings-docker.xml --projects gepi-webapp --also-make dependency:go-offline

FROM jetty:10.0.11-jdk11-openjdk as development
USER root
RUN apt-get update -y && apt-get install -y maven
USER jetty
COPY --from=dependencies --chown=jetty:jetty /usr/share/maven/ref/repository /var/lib/jetty/.m2/repository
COPY --from=dependencies --chown=jetty:jetty /usr/share/maven/ref/repository/de/julielab/gepi-test-data /var/lib/jetty/.m2/repository/de/julielab/gepi-test-data
CMD ["mvn", "-f", "/var/gepi/dev", "--projects", "gepi-webapp", "--also-make", "jetty:run"]

FROM jetty:10.0.9-jre11-slim-openjdk as production
USER root
RUN apt-get update -y && apt-get install -y python3.9 && apt-get install -y python3-pip && pip install pandas==1.3.2 openpyxl==3.0.9 xlsxwriter==3.0.1
USER jetty
#RUN java -jar "$JETTY_HOME/start.jar" --add-to-startd=https
COPY gepi-webapp/target/gepi-webapp.war /var/lib/jetty/webapps/ROOT.war
