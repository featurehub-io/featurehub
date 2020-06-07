FROM adoptopenjdk:11-jre-hotspot-bionic

RUN mkdir /db && mkdir /target && mkdir /etc/app-config && mkdir /etc/common-config
COPY mr.properties /etc/common-config/common.properties
COPY log4j2.xml /etc/common-config/log4j2.xml
RUN apt-get update && apt-get install -y nginx
COPY default_site /etc/nginx/sites-enabled/default





