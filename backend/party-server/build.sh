#!/bin/sh
mvn -Ddocker-cloud-build clean package jib:dockerBuild

