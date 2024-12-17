docker stop grafana 
docker rm grafana 
docker image rm grafana   

docker run -d ^
  --name grafana ^
  --net plc-amq-network ^
  -p 3000:3000 ^
  grafana/grafana-oss