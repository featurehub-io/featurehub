#!/bin/bash
function macosApi() {
  echo "fixing $1"
#  sed 's/using IO.FeatureHub.MR.Model;/using IO.FeatureHub.MR.Model;\nusing MREnvironment = IO.FeatureHub.MR.Model.Environment;\n/g' $1 >../$1
  sed 's/ Environment/ MREnvironment/g' $1 >1.cs
  sed 's/<Environment/<MREnvironment/g' 1.cs >2.cs
  sed 's/MREnvironmentFeaturesResult/EnvironmentFeaturesResult/g' 2.cs >3.cs
  sed 's/using IO.FeatureHub.MR.Model;/using IO.FeatureHub.MR.Model;\nusing MREnvironment = IO.FeatureHub.MR.Model.Environment;\n/g' 3.cs >../$1
}
function macosModel() {
  echo "fixing $1"
#  sed 's/namespace IO.FeatureHub.MR.Model/using MREnvironment = IO.FeatureHub.MR.Model.Environment;\nnamespace IO.FeatureHub.MR.Model\n/g' $1 >../$1
  sed 's/namespace IO.FeatureHub.MR.Model/using MREnvironment = IO.FeatureHub.MR.Model.Environment;\nnamespace IO.FeatureHub.MR.Model/g' $1 >../$1
}
function linuxSed() {
  echo "fixing $1"
  sed 's/using IO.FeatureHub.MR.Model;/using IO.FeatureHub.MR.Model;\nusing MREnvironment = IO.FeatureHub.MR.Model.Environment;\n/g' $1
}
cd src/IO.FeatureHub.MR/API
if [[ $OSTYPE == 'darwin'* ]]; then
  echo 'macOS'
  mkdir -p x
  mv * x
  cd x
  find . -type f -print | while read file; do macosApi "$file"; done
  cd ..
  rm -rf x
  cd ../Model
  mkdir -p x
  mv * x
  cd x
  find . -type f -print | while read file; do macosModel "$file"; done
  cd ..
  rm -rf x
else
  echo 'linux'
  find . -type f -print | while read file; do linuxSed "$file"; done
fi
