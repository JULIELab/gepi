FROM docker.elastic.co/elasticsearch/elasticsearch:7.17.8
ENV xpack.security.enabled=false
ADD elasticsearch-mapper-preanalyzed-7.17.8.zip /
RUN ["bin/elasticsearch-plugin", "install", "file:///elasticsearch-mapper-preanalyzed-7.17.8.zip"]