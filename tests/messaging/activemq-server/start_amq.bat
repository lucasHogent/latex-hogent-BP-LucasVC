docker network create plc-amq-network
docker stop amq
docker rm amq
docker image rm amq
docker build -t amq .   
docker run --privileged=true -d --name amq --net plc-amq-network -p 16161:16161 -p 8161:8161 -p 8001:8001 -p 8002:8002 -p 8003:8003 -p 5672:5672 -p 8021:8020  amq
