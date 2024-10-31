cd /d "%~dp0"
docker build -t activemq-bidirectional-listener .
docker run -p 8161:8161 -p 61616:61616 -p 8001:8001 -p 8002:8002 -p 8003:8003 activemq-bidirectional-listener