# Dockerfile for simulating the PLC

# Use the official Python image as a base
FROM python:3.9-slim

# Set the working directory
WORKDIR /app/plc

EXPOSE 8001 8002 8003

# Copy the application files to the container
COPY plc-simulator.py plc-config.ini ./

# Run the Python script
CMD ["python", "plc-simulator.py"]