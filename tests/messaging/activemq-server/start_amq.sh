docker network create plc-amq-network
docker build -t amq .   
docker run --privileged=true -d --name amq --net plc-amq-network -p 16161:16161 -p 8161:8161 -p 8001:8001 -p 8002:8002 -p 8003:8003 -p 5672:5672 -p 1099:1099 -p 8020:8020  amq
