FROM ruby:2.7.6-bullseye

MAINTAINER info@featurehub.io
ENV BUNDLER_VERSION 2.3.15
ARG DEBIAN_FRONTEND=noninteractive

RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y wget tzdata apt-transport-https

# set up nsswitch
COPY conf/nsswitch.conf /etc/nsswitch.conf

RUN echo 'gem: --no-document' >> ~/.gemrc && \
    gem install bundler -v ${BUNDLER_VERSION}  --force

#ENV BUNDLE_PATH /bundle
WORKDIR /app/featurehub-ruby
COPY Gemfile Gemfile.lock /app/featurehub-ruby/
RUN cd /app/featurehub-ruby && bundle install
ADD . /app/featurehub-ruby

EXPOSE 4567

CMD bundle exec ruby webhook_app.rb
