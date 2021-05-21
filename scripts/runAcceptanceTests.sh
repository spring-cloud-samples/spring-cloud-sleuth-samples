#!/usr/bin/env bash

set -o errexit

echo -e "\n\nRunning tests for Brave\n\n"
./mvnw clean install

echo -e "\n\nRunning tests for OTel\n\n"
./mvnw clean install -Potel
