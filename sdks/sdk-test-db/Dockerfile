FROM featurehub/party-server:1.3.5-RC1
RUN mkdir -p /db
COPY sdk-test-db.mv.db /db/party-h2.db.mv.db
RUN chown -R 999:999 /db
