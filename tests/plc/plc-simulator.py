import socket
import configparser
import logging
import os
import random
import threading
import queue
import time


def setup_logging(log_file_path):
    """Set up logging configuration."""
    os.makedirs(os.path.dirname(log_file_path), exist_ok=True)
    logging.basicConfig(
        filename=log_file_path,
        level=logging.INFO,
        format="%(asctime)s - %(levelname)s - %(message)s",
    )


def generate_random_message():
    """Generates a random hexadecimal message."""
    message_template = [
        "02", "00", "1d", "20",
        f"{random.randint(0, 255):02x}", f"{random.randint(0, 255):02x}", "20", "20", "00", "00", "20", "20",
        f"{random.randint(0, 255):02x}", f"{random.randint(0, 255):02x}", "20", "20",
        f"{random.randint(0, 255):02x}", "20", "20", "20", "20", "20", "20", "20", "20", "20",
        "20", "20", "20", "20", "20", "03"
    ]
    return " ".join(message_template)


def send_messages(socket_conn, port, message_queue, stop_event):
    """Send messages from the queue to the socket."""
    while not stop_event.is_set():
        try:
            # message = message_queue.get(timeout=1)  # Wait for a message from the queue
            message = generate_random_message()
            socket_conn.send(bytes.fromhex(message.replace(" ", "")))
            logging.info(f"Sent to {port}: {message}")
            print(f"Sent to {port}: {message}")
            time.sleep(0.1)  # Simulate delay
        except queue.Empty:
            continue  # No message to send; check stop_event


def receive_messages(socket_conn, port, buffer_size, stop_event):
    """Receive messages from the socket."""
    while not stop_event.is_set():
        try:
            data = socket_conn.recv(buffer_size)
            if data:
                message = data.hex()
                logging.info(f"Received from {port}: {message}")
                print(f"Received from {port}: {message}")
        except socket.error as e:
            logging.error(f"Error receiving from {port}: {e}")
            break


def handle_connection(host, port, buffer_size, message_queue, stop_event):
    """Handle socket connection, send and receive messages."""

    print(host, port)
    while not stop_event.is_set():
        try:
            # Create socket and connect to the server
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as socket_conn:
                socket_conn.connect((host, port))
                socket_conn.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)  # Disable Nagle's algorithm
                logging.info(f"Connected to {host}:{port}")
                print(f"Connected to {host}:{port}")

                # Create threads for sending and receiving messages
                send_thread = threading.Thread(target=send_messages, args=(socket_conn, port, message_queue, stop_event))
                receive_thread = threading.Thread(target=receive_messages, args=(socket_conn, port, buffer_size, stop_event))

                # Start threads
                send_thread.start()
                receive_thread.start()

                # Wait for threads to finish
                send_thread.join()
                receive_thread.join()

        except (ConnectionRefusedError, socket.timeout) as e:
            logging.error(f"Could not connect to {host}:{port} - {e}. Retrying in 5 seconds...")
            print(f"Retrying connection to {host}:{port} in 5 seconds...")
            time.sleep(5)


def generate_and_enqueue_messages(message_queue, stop_event):
    """Generate and enqueue random messages."""
    while not stop_event.is_set():
        message = generate_random_message()
        message_queue.put(message)
        logging.info(f"Enqueued message: {message}")
        print(f"Enqueued message: {message}")
        time.sleep(5)  # Simulate delay between message generations


def main():
    """Main entry point for the client."""
    # Load configurations
    config = configparser.ConfigParser()
    config.read("plc-config.ini")

    # Setup logging
    log_file_path = config["LOGGING"]["LOG_FILE"]
    setup_logging(log_file_path)

    # Retrieve configuration values
    host = config["SOCKET"]["HOST"]
    ports = [int(port.strip()) for port in config["SOCKET"]["PORTS"].split(",")]
    buffer_size = int(config["SOCKET"]["BUFFER_SIZE"])

    logging.info(f"Starting client for host {host} with ports {ports}")

    # Create a Queue and a stop_event
    message_queue = queue.Queue()
    stop_event = threading.Event()

    try:
        # Start message generation thread
        # message_thread = threading.Thread(target=generate_and_enqueue_messages, args=(message_queue, stop_event))
        # message_thread.start()

        # Start handling connections for each port
        connection_threads = []
        for port in ports:
            connection_thread = threading.Thread(target=handle_connection, args=(host, port, buffer_size, message_queue, stop_event))
            connection_threads.append(connection_thread)
            connection_thread.start()

        # Wait for all threads to finish (indefinitely running unless interrupted)
        # message_thread.join()
        for connection_thread in connection_threads:
            connection_thread.join()

    except KeyboardInterrupt:
        logging.info("Shutting down client...")
        stop_event.set()  # Signal all threads to stop

    finally:
        logging.info("Client stopped.")


if __name__ == "__main__":
    main()
