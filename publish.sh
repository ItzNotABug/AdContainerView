#!/usr/bin/env zsh

set -e

DRY_RUN=false
[[ "$1" == "--dry-run" ]] && DRY_RUN=true

if $DRY_RUN; then
    echo "Dry run: validating task graph..."
    ./gradlew clean build publishAndReleaseToMavenCentral --dry-run
else
    echo "Building & publishing to Maven Central..."
    ./gradlew clean build publishAndReleaseToMavenCentral --no-configuration-cache
fi
