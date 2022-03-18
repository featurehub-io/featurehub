FROM quay.io/keycloak/keycloak:15.0.2

ENV DB_VENDOR=h2
ENV KEYCLOAK_PASSWORD=admin
ENV KEYCLOAK_USER=admin
COPY generic-oauth-image-icon/html/tech-leaders.png /opt/jboss/keycloak/themes/keycloak/welcome/resources/
USER root
RUN mkdir -p /opt/jboss/keycloak/standalone
ADD keycloak-db /opt/jboss/keycloak/standalone/data
RUN chmod -R ugo+x /opt/jboss/keycloak/standalone/data
