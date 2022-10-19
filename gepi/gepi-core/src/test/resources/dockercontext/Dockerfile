FROM docker.elastic.co/elasticsearch/elasticsearch:7.9.1
ENV xpack.security.enabled=false
ADD elasticsearch-mapper-preanalyzed-7.9.1-SNAPSHOT.zip /
RUN ["bin/elasticsearch-plugin", "install", "file:///elasticsearch-mapper-preanalyzed-7.9.1-SNAPSHOT.zip"]