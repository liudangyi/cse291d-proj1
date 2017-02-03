#!/usr/bin/env bash

docker build -t cse291d-proj1 .

docker run --rm -it --name server cse291d-proj1 \
    bash -c 'cd /app && make pingpong-server'
docker run --rm -it --name client --link server cse291d-proj1 \
    bash -c 'cd /app && make pingpong-client'
