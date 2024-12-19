
docker run ^
  --volume=/:/rootfs:ro ^
  --volume=/var/run:/var/run:ro ^
  --volume=/sys:/sys:ro ^
  --volume=/var/lib/docker/:/var/lib/docker:ro ^
  --volume=/dev/disk/:/dev/disk:ro ^
  --net plc-amq-network -p 8080:8080 ^
  --detach=true ^
  --name=cadvisor  ^
  --privileged ^
  --device=/dev/kmsg ^
  gcr.io/cadvisor/cadvisor:v0.49.1