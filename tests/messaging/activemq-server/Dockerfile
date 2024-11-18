
FROM redhat/ubi8:latest

ARG AMQ_VERSION="6.1.4"
ARG JRE_VERSION="17"

USER root

EXPOSE 8161 61616 5672
EXPOSE 8001 8002 8003 5672

COPY ../java-listener/java-listener.jar /opt/jprogram/listener.jar
COPY start_listener.sh /opt/jprogram/start_listener.sh
COPY start_all.sh /opt/jprogram/start_all.sh
COPY listener-config.json /opt/jprogram/config.json  
COPY listeners.txt /opt/jprogram/listeners.txt

RUN yum update -y
RUN yum install -y systemd sudo java-${JRE_VERSION}-openjdk wget procps
RUN java -version

RUN echo "JAVA_HOME=$(readlink -f /usr/bin/java | sed "s:bin/java::")" | sudo tee -a /etc/profile
RUN source /etc/profile

RUN wget https://archive.apache.org/dist/activemq/${AMQ_VERSION}/apache-activemq-${AMQ_VERSION}-bin.tar.gz
RUN sudo tar -zxvf apache-activemq-${AMQ_VERSION}-bin.tar.gz -C /opt
RUN rm apache-activemq-${AMQ_VERSION}-bin.tar.gz

RUN ln -s /opt/apache-activemq-${AMQ_VERSION} /opt/activemq

# Set environment variables for ActiveMQ credentials
ENV ACTIVEMQ_USER=admin
ENV ACTIVEMQ_PASSWORD=admin

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
Description=Java Listener Service
After=network.target

[Service]
ExecStart=/opt/jprogram/start_all.sh
User=root
Group=root

[Install]
WantedBy=multi-user.target
EOF

ENV JAVA_HOME="/usr/lib/jvm/jre-"${JRE_VERSION}
 

# Replace JAVA_HOME in /opt/activemq/bin/env
RUN sed -i 's|^#JAVA_HOME=".*"|JAVA_HOME="/usr/lib/jvm/jre-'${JRE_VERSION}'"|g' /opt/activemq/bin/setenv

RUN sed -i 's/127.0.0.1/0.0.0.0/g' /opt/activemq/conf/activemq.xml
RUN sed -i 's/127.0.0.1/0.0.0.0/g' /opt/activemq/conf/jetty.xml

RUN sudo systemctl enable activemq.service
RUN sudo systemctl enable jprogram.service 


ENTRYPOINT ["/usr/sbin/init"]
