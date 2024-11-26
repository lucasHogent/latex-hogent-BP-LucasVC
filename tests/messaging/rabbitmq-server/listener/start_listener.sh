#!/bin/sh

# Print a log message indicating the script is starting
echo "$(date) - Starting Java Listener Service..." >> /opt/jprogram/listener.log

# Set the path for the listener JAR file (assuming listener.jar is already copied to /opt/jprogram)
LISTENER_JAR="/opt/jprogram/listener.jar"

# Set any necessary JVM options (optional)
JVM_OPTS="-Xms512m -Xmx1024m"

# Run the listener in the background, redirect output to listener.log
/usr/bin/java $JVM_OPTS -jar $LISTENER_JAR $1 >> /opt/jprogram/listener.log 2>&1 &

# Capture the PID of the running listener process
LISTENER_PID=$!

# Print a message indicating the listener has started
echo "$(date) - Java Listener started with PID $LISTENER_PID" >> /opt/jprogram/listener.log

# Wait indefinitely (or you could add other logic to monitor the process if needed)
wait $LISTENER_PID
