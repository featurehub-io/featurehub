#!/bin/sh
cd "${0%/*}"
docker run -p 8097:80 -v $PWD/generic-oauth-image-icon/html:/usr/share/nginx/html -v $PWD/generic-oauth-image-icon/conf.d:/etc/nginx/conf.d nginx
