#!/bin/bash
# this generates the APIs from the codebase currently installed in Maven. It allows us to use the "latest"
# versions of the SDKs in the code
mvn clean generate-sources
