import socket
import configparser
import logging
import os
import random
import threading
import queue
import time

retry_interval = None
send_interval = None


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


def send_messages(socket_conn, host, port, stop_event):
    """Send messages from the queue to the socket and ensure delivery."""
    while not stop_event.is_set():
        try:
            if socket_conn is None:
                logging.info(f"Attempting to connect to {host}:{port}")
                socket_conn = socket.create_connection((host, port))
                logging.info(f"Connected to {host}:{port}")

            message = generate_random_message()
            full_message = bytes.fromhex(message.replace(" ", ""))
            
            # Send message
            socket_conn.sendall(full_message)
            logging.info(f"Sent to {port}: {message}")
            print(f"Sent to {port}: {message}")
            
             
            time.sleep(send_interval)  # Simulate delay
        except queue.Empty:
            continue  # No message to send; check stop_event
        except Exception as e:
            logging.error(f"Error sending to {port}: {e}")
            print(f"Error sending to {port}: {e}")
            time.sleep(retry_interval) 
            if socket_conn:
                try:
                    socket_conn.close()
                except Exception as close_error:
                    logging.warning(f"Error closing socket: {close_error}")
                finally:
                    socket_conn = None
            


def receive_messages(socket_conn, port, buffer_size, stop_event):
    """Receive messages from the socket."""
    while not stop_event.is_set():
        try:
            data = socket_conn.recv(buffer_size)
            if data:
                logging.info(f"Received from {port}: {data}")
                print(f"Received from {port}: {data}")
                
        except socket.error as e:
            logging.error(f"Error receiving from {port}: {e}")
            break


def handle_connection(host, port, buffer_size, stop_event):
    """Handle socket connection, send and receive messages."""

    print(host, port)
    while not stop_event.is_set():
        try:
            # Create socket and connect to the server
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as socket_conn:
                try:
                    socket_conn.connect((host, port))
                    socket_conn.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)  
                    logging.info(f"Connected to {host}:{port}")
                    print(f"Connected to {host}:{port}")

                    # Create threads for sending and receiving messages
                    send_thread = threading.Thread(target=send_messages, args=(socket_conn, host, port, stop_event))
                    receive_thread = threading.Thread(target=receive_messages, args=(socket_conn, port, buffer_size, stop_event))

                    # Start threads
                    send_thread.start()
                    receive_thread.start()

                    # Wait for threads to finish
                    send_thread.join()
                    receive_thread.join()
                except (socket.error, socket.timeout) as e:
                    print(f"Connection failed for port {host}:{port}: {e}. Retrying in {retry_interval} seconds...")
                    time.sleep(retry_interval)
        except (ConnectionRefusedError, socket.timeout) as e:
            logging.error(f"Could not connect to {host}:{port} - {e}. Retrying in {retry_interval} seconds...")
            print(f"Retrying connection to {host}:{port} in {retry_interval} seconds...")
            time.sleep(retry_interval)

def main():
    """Main entry point for the client."""
    # Load configurations
    config = configparser.ConfigParser()
    #config.read("c:/Users/lucas784/Desktop/Bachelorproef/latex-hogent-BP-LucasVC/tests/plc/plc-config.ini")
    config.read("plc-config.ini")

    print("Loaded sections:", config.sections())
    # Setup logging
    log_file_path = config["LOGGING"]["LOG_FILE"]
    setup_logging(log_file_path)

    # Retrieve configuration values
    host = config["SOCKET"]["HOST"]
    ports = [int(port.strip()) for port in config["SOCKET"]["PORTS"].split(",")]
    buffer_size = int(config["SOCKET"]["BUFFER_SIZE"])

    global retry_interval, send_interval
    retry_interval = float(config["INTERVAL"]["RETRY"])
    send_interval = float(config["INTERVAL"]["SEND"])

    logging.info(f"Starting client for host {host} with ports {ports}")
  
    stop_event = threading.Event()

    try:

        # Start handling connections for each port
        connection_threads = []
        for port in ports:
            connection_thread = threading.Thread(target=handle_connection, args=(host, port, buffer_size, stop_event))
            connection_threads.append(connection_thread)
            connection_thread.start()

        for connection_thread in connection_threads:
            connection_thread.join()

    except KeyboardInterrupt:
        logging.info("Shutting down client...")
        stop_event.set()  # Signal all threads to stop

    finally:
        logging.info("Client stopped.")


if __name__ == "__main__":
    main()
