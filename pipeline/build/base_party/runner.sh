#/bin/sh
/target/nats-server &
java -jar /target/party-server-1.1-SNAPSHOT.jar -Rio.featurehub.edge.Application -P/etc/common-config/common.properties
