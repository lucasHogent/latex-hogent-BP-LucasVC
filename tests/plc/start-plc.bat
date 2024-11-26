cd /d "%~dp0"

docker stop plc-simulator
docker rm plc-simulator
docker image rm plc-simulator
docker build -t plc-simulator .
docker run -d --name  plc-simulator --net plc-amq-network plc-simulator