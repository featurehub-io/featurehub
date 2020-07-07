FROM node:14.5.0-stretch

RUN apt-get update && apt-get install -y nginx
COPY default_site /etc/nginx/sites-enabled/default

COPY . /app
RUN cp -Rv /app/todo-frontend /var/www/html/
RUN cd /app && npm install --production && \
  mkdir -p node_modules/client-typescript-core/dist node_modules/client-typescript-eventsource/dist && \
  mv client-typescript-core/* node_modules/client-typescript-core/dist && \
  mv client-typescript-eventsource/* node_modules/client-typescript-eventsource/dist
COPY docker-run.sh /
RUN chmod ugo+x /docker-run.sh
EXPOSE 5000
ENTRYPOINT /docker-run.sh




