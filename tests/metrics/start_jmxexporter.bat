docker pull prom/prometheus

#docker volume create prometheus-data

docker run \
    -p 9080:9080 \
    -v prometheus.yml:/etc/prometheus/prometheus.yml \
    prom/prometheus
  
