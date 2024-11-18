FROM openjdk:11-jre-slim


RUN apk update && apk upgrade && apk --update add curl && rm -rf /tmp/* /var/cache/apk/*

ENV VERSION 0.12.0
ENV JAR jmx_prometheus_httpserver-$VERSION-jar-with-dependencies.jar

# Add JMX Prometheus Exporter
ENV JMX_EXPORTER_VERSION=0.16.1
RUN curl -L https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_httpserver/$VERSION/$JAR -o /opt/jmx_exporter/$JAR

# Add configuration
COPY prometheus-config.yaml /opt/prometheus-config.yaml

EXPOSE 8080

CMD ["java", "-javaagent:/opt/jmx_prometheus_javaagent.jar=8080:/opt/prometheus-config.yaml"]
