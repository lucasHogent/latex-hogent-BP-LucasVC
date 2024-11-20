docker build -t prometheus .
docker run -d --name prometheus --net plc-amq-network -p 9080:9090 prometheus