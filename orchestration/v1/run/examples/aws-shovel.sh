#!/bin/bash

python3.6 run-logged-aws-playlist.py \
--mode benchmark \
--playlist-file playlists/shovel-10-100-queues.json \
--aws-config-file /path/to/my/aws-config.json \
--loadgen-instance c4.4xlarge \
--gap-seconds 120 \
--repeat 1 \
--parallel 3 \
--config-count 1 \
--config-tag c1 \
--technology rabbitmq \
--instance c4.4xlarge \
--volume ebs-gp2 \
--volume-size 100 \
--filesystem xfs \
--tenancy default \
--core-count 8 \
--threads-per-core 2 \
--cluster-size 1 \
--version 3.8.2 \
--generic-unix-url https://github.com/rabbitmq/rabbitmq-server/releases/download/v3.8.2/rabbitmq-server-generic-unix-3.8.2.tar.xz \
--tags shovel-10-100 \
--federation-enabled true