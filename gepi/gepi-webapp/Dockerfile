# syntax=docker/dockerfile:1

FROM jetty:10.0.9-jre11-slim-openjdk
#RUN java -jar "$JETTY_HOME/start.jar" --add-to-startd=https
COPY target/gepi-webapp.war /var/lib/jetty/webapps/ROOT.war
