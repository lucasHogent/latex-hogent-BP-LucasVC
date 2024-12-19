docker network create plc-amq-network
docker stop rabbitmq
docker rm rabbitmq
docker image rm rabbitmq
docker build -t rabbitmq .   
docker run --privileged=true -d --name rabbitmq ^
--net plc-amq-network -p 15672:15672 -p 15692:15692 ^
-p 8001:8001 -p 8002:8002 -p 8003:8003 ^
-p 8004:8004 -p 8005:8005 -p 8006:8006 ^
-p 8007:8007 -p 8008:8008 -p 8009:8009 ^
-p 8010:8010 -p 8011:8011 -p 8012:8012 ^
-p 8014:8014 -p 8015:8015 -p 8016:8016 ^
-p 8017:8017 -p 8018:8018 -p 8019:8019 ^
-p 5672:5672  rabbitmq 
