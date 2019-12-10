#!/usr/bin/env bash
GIT_ROOT="$(git rev-parse --show-toplevel)"
docker run --network host \
  -v $GIT_ROOT/local-config/:/config \
  $1
