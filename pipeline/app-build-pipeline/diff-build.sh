#!/bin/sh
# todo
# - have a file  that if it  exists everything will build
# - another step needs to do this stuff and on success push it to maven deploy on merge to master
# - we need a different pom to ensure that front end is built and tested if it  (a) changes or (b) dependencies change.
# figure out what changed in comparison to master
packages=`git diff --name-only master | xargs -n1 dirname | grep "[backend|sdks|admin\-frontend]\/.*" | cut -d/ -f 2 | uniq | xargs -I % echo "backend/%" | xargs echo`
echo Packages to build are $packages
if [ -z $packages ]
then
  echo "Nothing to do"
  exit 0;
fi
# change ~/.m2 file to point to repository in workspace
echo "Installing tiles"
cd backend && mvn --no-transfer-progress install -f pom-first.xml
echo "Installing packages"
cd .. && mvn -T4C --no-transfer-progress -pl --also-make-dependents --batch-mode $packages install
