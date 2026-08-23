#!/usr/bin/env bash
# ponytail: wraps mvn in a Maven+JDK21 container since the host has no
# Java/Maven installed; mounts the docker socket so Testcontainers (used by
# integration tests) can start sibling containers, and a named volume for
# the local .m2 repo so dependencies aren't re-downloaded every run.
set -euo pipefail
export MSYS_NO_PATHCONV=1
HOSTDIR="$(pwd -W 2>/dev/null || pwd)"
docker run --rm \
  -v "${HOSTDIR}:/workspace" \
  -v franquicias-m2-repo:/root/.m2 \
  -v //var/run/docker.sock:/var/run/docker.sock \
  -w /workspace \
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
  -e TESTCONTAINERS_RYUK_DISABLED=true \
  maven:3.9-eclipse-temurin-21 mvn "$@"
