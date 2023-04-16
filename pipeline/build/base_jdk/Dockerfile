FROM --platform=$BUILDPLATFORM eclipse-temurin:11-jre-alpine

# make the non-root user & make appropriate directories
RUN (delgroup ping | true) && adduser -g 999 -u 999 -D k8suser && mkdir -p /db /target /etc/app-config /etc/common-config




