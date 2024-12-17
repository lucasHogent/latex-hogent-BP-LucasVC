
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
EXPOSE 8020 

RUN yum update -y
RUN yum install -y systemd sudo java-${JRE_VERSION}-openjdk wget procps
RUN java -version

RUN echo "JAVA_HOME=$(readlink -f /usr/bin/java | sed "s:bin/java::")" | sudo tee -a /etc/profile
RUN source /etc/profile

RUN wget https://archive.apache.org/dist/activemq/${AMQ_VERSION}/apache-activemq-${AMQ_VERSION}-bin.tar.gz
RUN sudo tar -zxvf apache-activemq-${AMQ_VERSION}-bin.tar.gz -C /opt
RUN rm apache-activemq-${AMQ_VERSION}-bin.tar.gz
RUN ln -s /opt/apache-activemq-${AMQ_VERSION} /opt/activemq

RUN mkdir /opt/jmx_exporter  
RUN wget https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.16.1/jmx_prometheus_javaagent-0.16.1.jar -O /opt/jmx_exporter/jmx_prometheus_javaagent.jar


# Set environment variables for ActiveMQ credentials
ENV ACTIVEMQ_USER=admin
ENV ACTIVEMQ_PASSWORD=admin

COPY listener/activemq-listener.jar /opt/jprogram/listener.jar
COPY listener/start_listener.sh /opt/jprogram/start_listener.sh
COPY listener/start_all.sh /opt/jprogram/start_all.sh
COPY listener/listener-config.json /opt/jprogram/config.json  
COPY listener/listeners.txt /opt/jprogram/listeners.txt
COPY setenv opt/activemq/bin/setenv
COPY jmx-config.yml /opt/jmx_exporter/config.yml

RUN cat <<EOF > /etc/systemd/system/activemq.service
[Unit]
Description=activemq message queue
After=network.target

[Service]
PIDFile=/opt/activemq/data/activemq.pid
ExecStart=/opt/activemq/bin/activemq start
ExecStop=/opt/activemq/bin/activemq stop
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

ENV JAVA_HOME="/usr/lib/jvm/jre-"${JRE_VERSION}
 
RUN sed -i 's/127.0.0.1/0.0.0.0/g' /opt/activemq/conf/jetty.xml
COPY activemq.xml /opt/activemq/conf/activemq.xml

RUN systemctl enable activemq.service
RUN systemctl enable jprogram.service  

ENTRYPOINT ["/usr/sbin/init"]
