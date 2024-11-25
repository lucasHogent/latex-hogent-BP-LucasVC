
FROM redhat/ubi8:latest

ARG AMQ_VERSION="6.1.4"
ARG JRE_VERSION="17"

USER root

# sockets
EXPOSE 8001 8002 8003 
# amqp
EXPOSE 61616 
# web console
EXPOSE 5672 
# prometheus
EXPOSE 8021 

RUN yum update -y
RUN yum install -y systemd sudo java-${JRE_VERSION}-openjdk wget procps libaio

RUN java -version
RUN export JAVA_HOME=/usr/lib/jvm/java-${JRE_VERSION}-openjdk
RUN export PATH=$JAVA_HOME/bin:$PATH
RUN echo "JAVA_HOME=$(readlink -f /usr/bin/java | sed "s:bin/java::")" | sudo tee -a /etc/profile
RUN source /etc/profile

RUN sudo wget https://archive.apache.org/dist/activemq/activemq-artemis/2.32.0/apache-artemis-2.32.0-bin.tar.gz

RUN sudo tar -xvzf apache-artemis-2.32.0-bin.tar.gz -C /opt
RUN sudo rm -rf apache-artemis-2.32.0-bin.tar.gz 

RUN ln -s /opt/apache-artemis-2.32.0/ /opt/artemis
 
RUN /opt/artemis/bin/artemis create /opt/artemis/artemis-broker --user artemis --password artemis --silent --require-login 
 
RUN cat <<EOF > /etc/systemd/system/artemis.service
[Unit]
Description=artemis-broker service
After=network.target

[Service]
Type=forking
ExecStart=/opt/artemis/artemis-broker/bin/artemis-service start 
ExecStop=/opt/artemis/artemis-broker/bin/artemis-service stop 
ExecReload=/opt/artemis/artemis-broker/bin/artemis-service restart
Restart=on-failure
WorkingDirectory=/opt/artemis/artemis-broker/
User=root
Group=root

[Install]
WantedBy=multi-user.target
EOF
 
RUN mkdir /opt/artemis/artemis-broker/web
COPY bootstrap.xml /opt/artemis/artemis-broker/etc
COPY broker.xml /opt/artemis/artemis-broker/etc
COPY artemis-prometheus-metrics-plugin-2.2.0.jar /opt/artemis/artemis-broker/lib
COPY metrics.war /opt/artemis/artemis-broker/web

RUN systemctl enable artemis.service

ENTRYPOINT ["/usr/sbin/init"]
