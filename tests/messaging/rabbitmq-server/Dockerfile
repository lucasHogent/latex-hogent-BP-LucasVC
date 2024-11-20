FROM redhat/ubi8:latest

ARG AMQ_VERSION="6.1.4"
ARG JRE_VERSION="17"

USER root

EXPOSE 8161 61616 5672 9020
EXPOSE 8001 8002 8003 5672 8020

RUN yum update -y
RUN yum install -y systemd sudo java-${JRE_VERSION}-openjdk wget procps 
RUN java -version

RUN echo "JAVA_HOME=$(readlink -f /usr/bin/java | sed "s:bin/java::")" | sudo tee -a /etc/profile
RUN source /etc/profile

RUN mkdir /opt/jmx_exporter  
RUN wget https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.16.1/jmx_prometheus_javaagent-0.16.1.jar -O /opt/jmx_exporter/jmx_prometheus_javaagent.jar

COPY java-listener.jar /opt/jprogram/listener.jar
COPY start_listener.sh /opt/jprogram/start_listener.sh
COPY start_all.sh /opt/jprogram/start_all.sh
COPY listener-config.json /opt/jprogram/config.json  
COPY listeners.txt /opt/jprogram/listeners.txt
COPY jmx-config.yml /opt/jmx_exporter/config.yml

ENV JAVA_HOME="/usr/lib/jvm/jre-"${JRE_VERSION}

ENTRYPOINT ["/usr/sbin/init"]
