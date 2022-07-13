# syntax=docker/dockerfile:1

FROM jetty:10.0.9-jre11-slim-openjdk
USER root
RUN apt-get update -y && apt-get install -y python3.9 && apt-get install -y python3-pip && pip install pandas==1.3.2 openpyxl==3.0.9 xlsxwriter==3.0.1
USER jetty
#RUN java -jar "$JETTY_HOME/start.jar" --add-to-startd=https
COPY target/gepi-webapp.war /var/lib/jetty/webapps/ROOT.war
