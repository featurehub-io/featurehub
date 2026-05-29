#!/bin/zsh
# zsh has no mapfile/readarray
IFS=$'\n' folders=$(cd .. && find . -name 'pom.xml' -maxdepth 2 | xargs grep -l "<artifactId>dacha-api</artifactId>" | grep -v "./dacha-api/")
for f in "${(f)folders}"
do
  folder=$(dirname $f)
  echo "processing ${folder}"
  cd ..
  cd ${folder}
  mvn -o install
done

