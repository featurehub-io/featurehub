#!/bin/sh
cd featuremesh
echo "moving /root/.m2/repository to home $HOME"
mkdir -p $HOME/.m2
mv /root/.m2/repository $HOME/.m2/repository
echo "checking user home"
ls -l $HOME/.m2
ls -l $HOME/.m2/repository
echo "copying docker creds so jib can find them"
mkdir .docker
ln -s $HOME/.docker/config.json .docker/config.json

echo "installing tiles and building images and pushing to repository"
mvn -Duser.home=$HOME clean install -f pom-first.xml && \
 mvn -Duser.home=$HOME -Ddocker-cloud-build=true clean install -f pom.xml && \
 cd deploy && \
 mvn -Duser.home=$HOME -Ddocker-cloud-build=true clean install

