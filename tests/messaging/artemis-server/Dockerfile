
FROM redhat/ubi8:latest

ARG AMQ_VERSION="6.1.4"
ARG JRE_VERSION="17"

USER root

# sockets
EXPOSE 8001 8002 8003 
# amqp
EXPOSE 61616 5672
# web console
EXPOSE 8161 
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
 
RUN /opt/artemis/bin/artemis create /opt/artemis/artemis-broker --user admin --password admin --silent --require-login 
 
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

RUN cat <<EOF > /etc/systemd/system/jprogram.service
[Unit]
Description=Java PLC Listener Service
After=syslog.target network.target

[Service]
Type=forking
ExecStart=/opt/jprogram/start_all.sh
WorkingDirectory=/opt/jprogram
KillMode=none
SendSIGKILL=no
User=root
Group=root
TimeoutSec=240

[Install]
WantedBy=multi-user.target
EOF


RUN mkdir /opt/jprogram
COPY listener/java-listener.jar /opt/jprogram/listener.jar
COPY listener/start_listener.sh /opt/jprogram/start_listener.sh
COPY listener/start_all.sh /opt/jprogram/start_all.sh
COPY listener/listener-config.json /opt/jprogram/config.json  
COPY listener/listeners.txt /opt/jprogram/listeners.txt
 
RUN mkdir /opt/artemis/artemis-broker/web
COPY bootstrap.xml /opt/artemis/artemis-broker/etc
COPY broker.xml /opt/artemis/artemis-broker/etc
COPY artemis-prometheus-metrics-plugin-2.2.0.jar /opt/artemis/artemis-broker/lib
COPY metrics.war /opt/artemis/artemis-broker/web

RUN systemctl enable artemis.service

ENTRYPOINT ["/usr/sbin/init"]
