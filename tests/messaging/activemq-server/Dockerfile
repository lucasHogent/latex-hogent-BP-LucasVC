# Dockerfile
FROM webcenter/activemq:latest

# Expose ActiveMQ console port and custom ports
EXPOSE 8161 61616 8001 8002 8003

# Set environment variables for ActiveMQ
ENV ACTIVEMQ_ADMIN_LOGIN=admin
ENV ACTIVEMQ_ADMIN_PASSWORD=admin

# Copy JavaScript listener script and configuration file
COPY listener.js /opt/jprogram/plc/listener.js
COPY listener-config.json /opt/jprogram/plc/config.json

# Create log directory and set permissions
RUN mkdir -p /opt/jprogram/plc/log && chmod -R 755 /opt/jprogram/plc/log

# Start ActiveMQ and the JavaScript listener script
CMD ["sh", "-c", ""]
