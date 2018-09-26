FROM docker.elastic.co/elasticsearch/elasticsearch:5.4.0
ENV xpack.security.enabled=false
ADD elasticsearch-mapper-preanalyzed-5.4.0.zip /
RUN ["bin/elasticsearch-plugin", "install", "file:///elasticsearch-mapper-preanalyzed-5.4.0.zip"]