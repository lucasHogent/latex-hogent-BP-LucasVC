docker network create plc-amq-network
docker stop rabbitmq
docker rm rabbitmq
docker image rm rabbitmq
docker build -t rabbitmq .   
docker run --privileged=true -d --name rabbitmq --net plc-amq-network -p 15692:15692 -p 15672:15672 -p 5672:5672 -p 8022:8020  rabbitmq 
