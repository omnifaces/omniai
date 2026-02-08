#!/usr/bin/env bash
set -a
source .env.local
set +a

mvn clean verify -DskipUTs=true -DskipITs=false -Dmaven.javadoc.skip=true -DargLine="-Djava.util.logging.config.file=src/test/resources/logging.properties" "$@"
