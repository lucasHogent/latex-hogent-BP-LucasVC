import stomp
import socket
import json
import os
import time
from datetime import datetime

# Load configuration
with open('/app/config.json', 'r') as f:
    config = json.load(f)

host = config['activemq']['host']
port = config['activemq']['port']
login = config['activemq']['login']
passcode = config['activemq']['passcode']
queue = config['activemq']['queue']
ports = config['ports']
log_path = config['logPath']

# Ensure log directory exists
if not os.path.exists(log_path):
    os.makedirs(log_path)

# Log function to write incoming/outgoing messages
def log_message(direction, port, message):
    log_file = os.path.join(log_path, 'messages.log')
    timestamp = datetime.now().isoformat()
    log_entry = f"{timestamp} | {direction} | Port {port} | Message: {message}\n"
    with open(log_file, 'a') as f:
        f.write(log_entry)

# Connect to ActiveMQ
def connect_to_activemq():
    conn = stomp.Connection([(host, port)])
    conn.connect(login, passcode, wait=True)
    return conn

# Send message to ActiveMQ
def send_to_activemq(conn, port, data):
    headers = {
        'destination': queue,
        'content-type': 'text/plain',
        'port': str(port)  # Add custom property for filtering
    }
    conn.send(body=data, headers=headers)
    print(f"Sent message to ActiveMQ on port {port}")

# Subscribe to ActiveMQ
def subscribe_to_activemq(conn, port, client_socket):
    def message_listener(msg):
        body = msg.body
        log_message("Outgoing", port, body)
        client_socket.sendall(body.encode('utf-8'))

    headers = {
        'destination': queue,
        'ack': 'auto',
        'selector': f"port = '{port}'"
    }
    conn.subscribe(headers=headers, listener=message_listener)
    print(f"Subscribed to ActiveMQ on port {port}")

# Create TCP server for incoming socket connections
def create_listener(port):
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind(('0.0.0.0', port))
    server.listen(5)
    print(f"Listening on port {port}")

    conn = connect_to_activemq()

    while True:
        client_socket, addr = server.accept()
        print(f"Connection established on port {port} from {addr}")

        try:
            # Handle incoming data on socket
            data = client_socket.recv(1024).decode('utf-8')
            print(f"Received data on port {port}: {data}")
            log_message("Incoming", port, data)

            # Send data to ActiveMQ
            send_to_activemq(conn, port, data)

            # Subscribe to ActiveMQ and send back received messages to the client
            subscribe_to_activemq(conn, port, client_socket)

        except Exception as e:
            print(f"Error handling connection on port {port}: {e}")
        finally:
            client_socket.close()

# Main function to start the listeners for all configured ports
def main():
    for port in ports:
        create_listener(port)

if __name__ == '__main__':
    main()
