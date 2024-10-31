# async_socket_client.py

import asyncio
import configparser
import random
import logging
import os


def setup_logging(log_file_path):
    """Set up logging configuration."""
    logging.basicConfig(
        filename=log_file_path,  # Log to the specified file path
        level=logging.INFO,      # Log level
        format='%(asctime)s - %(levelname)s - %(message)s'  # Log format
    )


def generate_random_message():
    """Generates a random hexadecimal message in the specified format."""
    message_template = [
        "02", "00", "1d", "20",        # Fixed parts of the message
        f"{random.randint(0, 255):02x}", f"{random.randint(0, 255):02x}", "20", "20", "00", "00", "20", "20",
        f"{random.randint(0, 255):02x}", f"{random.randint(0, 255):02x}", "20", "20",
        f"{random.randint(0, 255):02x}", "20", "20", "20", "20", "20", "20", "20", "20", "20",
        "20", "20", "20", "20", "20", "03"  # End with 03 as specified
    ]
    # Join list elements with a space to form the final message
    return " ".join(message_template)

async def handle_connection(host, port, buffer_size):
    """Asynchronous function to handle a single socket connection with send and receive capability."""
    while True:  # Loop for retrying connections
        reader, writer = None, None  # Initialize to None

        try:
            # Attempt to open the connection
            reader, writer = await asyncio.open_connection(host, port)
            logging.info(f"Connected to {host}:{port}")
            print(f"Connected to {host}:{port}")
            
            async def send_messages():
                """Coroutine to handle sending random hexadecimal messages."""
                while True:
                    message = generate_random_message()
                    writer.write(bytes.fromhex(message.replace(" ", "")))  # Convert to bytes
                    await writer.drain()
                    print(f"Sent to {port}: {message}")
                    await asyncio.sleep(5)  # Send every 5 seconds or adjust as needed

            async def receive_messages():
                """Coroutine to handle receiving messages."""
                while True:
                    data = await reader.read(buffer_size)
                    if data:
                        print(f"Received from {port}: {data.hex()}")  # Print in hexadecimal format
                    await asyncio.sleep(1)  # Adjust delay based on expected traffic

            # Run both send and receive coroutines concurrently
            await asyncio.gather(send_messages(), receive_messages())
            break  # Exit the loop if the connection is successful
            
        except (ConnectionRefusedError, asyncio.TimeoutError) as e:
            error_message = f"Could not connect to {host}:{port} - {e}. Retrying in 5 seconds..."
            logging.error(error_message)  # Log the error
            print(error_message)  # Print the error
            await asyncio.sleep(5)  # Wait for 5 seconds before retrying

        finally:
            # Close the writer if it was successfully created
            if writer is not None:
                writer.close()
                await writer.wait_closed()

async def main():
    # Load configurations
    config = configparser.ConfigParser()
    config.read("plc-config.ini")

    # Set up logging using the log file path from the config
    log_file_path = config["LOGGING"]["LOG_FILE"]
    log_dir = os.path.dirname(log_file_path)
    if not os.path.exists(log_dir):
        os.makedirs(log_dir)
    setup_logging(log_file_path) 

    host = config["SOCKET"]["HOST"]
    ports = [int(p.strip()) for p in config["SOCKET"]["PORTS"].split(",")]
    buffer_size = int(config["SOCKET"]["BUFFER_SIZE"])

    # Create tasks for each port connection
    tasks = [handle_connection(host, port, buffer_size) for port in ports]

    # Run all tasks concurrently
    await asyncio.gather(*tasks)

if __name__ == "__main__":
    asyncio.run(main())
