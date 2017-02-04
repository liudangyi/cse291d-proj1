#!/usr/bin/env bash

# DON'T run this script directly!
# You should build the image first, then run server/client in two windows.

docker build -t cse291d-proj1 .

docker run --rm -it --name server cse291d-proj1 \
    bash -c 'cd /app && make pingpong-server'
docker run --rm -it --name client --link server cse291d-proj1 \
    bash -c 'cd /app && make pingpong-client'
