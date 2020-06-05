FROM alpine:latest AS builder

# install kubectl
# install helm
# install groovy

WORKDIR /workspace
ENV VERSION=3.0.0-alpha.2
ENV BASE_URL="https://get.helm.sh"
ENV TAR_FILE="helm-v${VERSION}-linux-amd64.tar.gz"

RUN apk add --update --no-cache curl ca-certificates unzip

RUN    curl -L ${BASE_URL}/${TAR_FILE} | tar xvz
RUN mv linux-amd64/helm /workspace/helm && chmod +x /workspace/helm

#    mv linux-amd64/helm /usr/bin/helm && \
#

ENV KUBECTL_VER=1.5.1
RUN curl -LO https://storage.googleapis.com/kubernetes-release/release/v${KUBECTL_VER}/bin/linux/amd64/kubectl && mv kubectl /workspace/kubectl && chmod u+x /workspace/kubectl
#RUN chmod u+x kubectl && mv kubectl /bin/kubectl

RUN wget https://dl.bintray.com/groovy/maven/apache-groovy-binary-3.0.0-beta-2.zip && unzip apache-groovy-binary-3.0.0-beta-2.zip && mv groovy-3.0.0-beta-2 /workspace

FROM adoptopenjdk:11-jre-hotspot

WORKDIR /root/

COPY --from=builder /workspace/helm /usr/bin/helm
COPY --from=builder /workspace/kubectl /bin/kubectl
COPY --from=builder /workspace/groovy-3.0.0-beta-2 /usr/bin/groovy

