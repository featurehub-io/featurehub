FROM debian:bullseye

ENV FLUTTER_VERSION="master"

# image mostly inspired from https://github.com/GoogleCloudPlatform/cloud-builders-community/blob/770e0e9/flutter/Dockerfile

LABEL io.featurehub.flutter.name="debian linux image for Flutter web" \
      io.featurehub.flutter.license="MIT" \
      io.featurehub.flutter.vcs-type="git" \
      io.featurehub.flutter.vcs-url="https://github.com/featurehubio/featurehub/pipeline/flutter_web_dev"

WORKDIR /

RUN apt-get update -y && apt-get upgrade
RUN apt-get install -y \
  git \
  wget \
  curl \
  unzip \
  lcov \
  lib32stdc++6 \
  libglu1-mesa

ARG cachebust=1
# Install Flutter.
ENV FLUTTER_ROOT="/opt/flutter"
ENV FLUTTER="/opt/flutter"
ARG FLUTTER_VERSION=3.7.10
RUN git clone --depth=1 --branch $FLUTTER_VERSION https://github.com/flutter/flutter "${FLUTTER_ROOT}"
ENV PATH="${FLUTTER_ROOT}/bin:${PATH}"

# reset to known point as docker image is always 0.0, this is a stable version for us. Disable analytics and crash reporting on the builder.
RUN cd $FLUTTER && flutter precache
RUN flutter config  --no-analytics --enable-web

# Perform an artifact precache so that no extra assets need to be downloaded on demand.
RUN flutter precache

# Accept licenses.
RUN yes "y" | flutter doctor --android-licenses

# Perform a doctor run.
RUN flutter doctor -v
ENV PATH $PATH:/flutter/bin/cache/dart-sdk/bin:/flutter/bin

COPY build.sh /opt/app/build.sh
RUN chmod ugo+x /opt/app/build.sh


