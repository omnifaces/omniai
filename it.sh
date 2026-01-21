#!/usr/bin/env bash
set -a
source .env.local
set +a

mvn clean verify -DskipITs=false -Dmaven.javadoc.skip=true "$@"
