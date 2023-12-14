#!/bin/sh
echo reminder, to use this against "local" builds you must push your changes to your branch and change the antora-playbook.yml to specify that branch instead of main
rm -rf .cache build
#docker run -u $(id -u) -v $PWD:/antora:Z --rm -t antora/antora /bin/sh -c "yarn global add @antora/lunr-extension && antora --cache-dir=./.cache/antora antora-playbook.yml"
docker run -u 0:0 -v $PWD:/antora:Z --rm -t antora/antora /bin/sh -c "yarn global add @antora/lunr-extension && antora --cache-dir=./.cache/antora antora-playbook.yml"
