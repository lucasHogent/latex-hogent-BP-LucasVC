
FROM redhat/ubi8:latest

ENV RABBITMQ_VERSION=3.12.1 ERLANG_VERSION=26.2.5.5

# Update the system and install dependencies
RUN yum update -y && yum install -y wget

RUN cd /tmp/
RUN curl -sfL -O https://github.com/rabbitmq/erlang-rpm/releases/download/v26.2.5.5/erlang-26.2.5.5-1.el8.x86_64.rpm
RUN dnf install -y ./erlang-26.2.5.5-1.el8.x86_64.rpm


## primary RabbitMQ signing key
RUN rpm --import 'https://github.com/rabbitmq/signing-keys/releases/download/3.0/rabbitmq-release-signing-key.asc'
## modern Erlang repository
RUN rpm --import 'https://github.com/rabbitmq/signing-keys/releases/download/3.0/cloudsmith.rabbitmq-erlang.E495BB49CC4BBE5B.key'
## RabbitMQ server repository
RUN rpm --import 'https://github.com/rabbitmq/signing-keys/releases/download/3.0/cloudsmith.rabbitmq-server.9F4587F226208342.key'

COPY rabbitmq.repo /etc/yum.repos.d/
COPY rabbitmq.conf /etc/rabbitmq/

RUN mkdir /opt/jprogram
COPY listener/rabbitmq-listener.jar /opt/jprogram/listener.jar
COPY listener/start_listener.sh /opt/jprogram/start_listener.sh
COPY listener/start_all.sh /opt/jprogram/start_all.sh
COPY listener/listener-config.json /opt/jprogram/config.json  
COPY listener/listeners.txt /opt/jprogram/listeners.txt

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

RUN yum install -y socat logrotate

## install RabbitMQ and zero dependency Erlang
RUN dnf install -y erlang rabbitmq-server

RUN systemctl enable rabbitmq-server

RUN rabbitmq-plugins enable rabbitmq_management rabbitmq_prometheus

EXPOSE 15672 15692
EXPOSE 5672
EXPOSE 8001 8002 8003

ENTRYPOINT ["/usr/sbin/init"]
