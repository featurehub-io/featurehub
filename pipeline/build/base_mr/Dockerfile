FROM adoptopenjdk:11-jre-hotspot-focal

# MR related stuff
RUN groupadd -g 999 k8sgroup && useradd -g 999 -u 999 k8suser
RUN mkdir -p /db /target /etc/app-config /etc/common-config
COPY mr.properties /etc/common-config/common.properties
COPY log4j2.xml /etc/common-config/log4j2.xml
RUN chown 999:999 /db /target /etc/app-config /etc/common-config

# now for nginx
RUN apt-get update && apt-get install -y nginx && mkdir -p /var/lib/nginx/body /var/lib/nginx/proxy /var/lib/nginx/fastcgi
RUN chown -R www-data:www-data /var/lib/nginx/body /var/lib/nginx/proxy /var/lib/nginx /var/log/nginx /usr/share/nginx  /var/lib/nginx/fastcgi
COPY default_site /etc/nginx/sites-enabled/default
RUN chown www-data:www-data /etc/nginx/sites-enabled/default

EXPOSE 8085
EXPOSE 8701






