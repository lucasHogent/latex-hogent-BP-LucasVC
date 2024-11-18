cd /d "%~dp0"
docker build -t plc-simulator .
docker run -d --name  plc-simulator --net plc-amq-network plc-simulator