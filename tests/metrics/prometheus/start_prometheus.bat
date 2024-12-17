docker stop prometheus
docker rm prometheus
docker image rm prometheus
docker build -t prometheus .
docker run -d --name prometheus --net plc-amq-network -p 9080:9090 prometheus