cd /d "%~dp0"
docker build -t plc-simulator .
docker run plc-simulator