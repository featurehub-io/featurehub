FROM google/dart

WORKDIR /app/cli
ADD cli/pubspec.yaml /app/cli/
COPY app_mr_layer /app/app_mr_layer
RUN find /app
RUN ls -la
RUN pub get
ADD cli /app/cli
RUN pub get --offline
CMD []
ENTRYPOINT ["/usr/bin/dart", "lib/main.dart"]
