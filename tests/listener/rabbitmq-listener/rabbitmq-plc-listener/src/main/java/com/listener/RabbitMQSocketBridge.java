package com.listener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeoutException;

import javax.jms.JMSException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class RabbitMQSocketBridge {
    private static String rabbitmqHost;
    private static Integer rabbitmqPort;
    private static String login;
    private static String passcode;
    private static String SEND_QUEUE;
    private static String RECV_QUEUE;
    private static String logPath;
    private static Integer COM_CHANNEL;
    private static Integer port;

    public static void main(String[] args) {
        // TEST
        // args = new String[1];
        // args[0] = "1";

        System.out.println("args " + args[0].trim());
        try {
            COM_CHANNEL = Integer.parseInt(args[0].trim());

            if (COM_CHANNEL == null || COM_CHANNEL == 0) {
                throw new Exception("No valid channel");
            }
            // Path to the JSON configuration file
            // File configFile = new File(
            // "C:\\Users\\lucas784\\Desktop\\Bachelorproef\\latex-hogent-BP-LucasVC\\tests\\messaging\\rabbitmq-server\\listener\\listener-config.json");

            File configFile = new File("/opt/jprogram/config.json");

            // Parse JSON file into Config object
            ObjectMapper mapper = new ObjectMapper();
            Config config = mapper.readValue(configFile, Config.class);

            rabbitmqHost = config.broker.host;
            rabbitmqPort = config.broker.port;
            login = config.broker.login;
            passcode = config.broker.login;
            SEND_QUEUE = config.broker.queueSend;
            RECV_QUEUE = config.broker.queueRecv;
            port = config.channelPorts.get(COM_CHANNEL.toString());
            logPath = config.logPath;

            // Ensure log directory exists
            ensureLogDirectory(logPath);

            // Start listeners for all configured ports
            createListener(port);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Ensure log directory exists
    private static void ensureLogDirectory(String path) throws IOException {
        Files.createDirectories(Paths.get(path));
    }

    // Method to convert a byte array to a hex string
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);

            if (hex.length() == 1) {
                hexString.append('0'); // Add leading zero for single-digit hex values
            }
            hexString.append(hex).append(" ");
        }
        return hexString.toString().trim(); // Trim any trailing spaces
    }

    // Log messages
    private static void logMessage(String direction, int port, String message) {
        try (FileWriter writer = new FileWriter(logPath + "/channel" + COM_CHANNEL + ".log", true)) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(new Date());
            String logEntry = String.format("%s | %s | Port %d | Message: %s%n", timestamp, direction, port, message);
            writer.write(logEntry);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Connect to RabbitMQ
    private static Connection connectToRabbitMQ() throws JMSException, IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitmqHost);
        factory.setPort(rabbitmqPort);
        // factory.setVirtualHost(virtualHost);
        factory.setUsername(login);
        factory.setPassword(passcode);

        return factory.newConnection();
    }

    // Create a TCP server listener
    private static void createListener(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Listening on port " + port);

            // Setup rabbitmq connection
            Connection rabbitMQConnection = connectToRabbitMQ();
            Channel channel = rabbitMQConnection.createChannel();

            channel.queueDeclare(SEND_QUEUE, false, false, false, null);
            channel.queueDeclare(RECV_QUEUE, false, false, false, null);

            Socket clientSocket = serverSocket.accept();

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");

                try {
                    PrintWriter writer = new PrintWriter(serverSocket.accept().getOutputStream(), true);
                    writer.println("Message from Rabbitmq: " + message);
                    logMessage("WCS->PLC ", port, message);
                } catch (IOException e) {
                    System.err.println("Error sending message to client: " + e.getMessage());
                }
            };
            channel.basicConsume(RECV_QUEUE, true, deliverCallback, consumerTag -> {
            });

            // Main loop to accept and handle client connections
            while (true) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));
                        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {

                    System.out.println("Socket connection established on port " + port);
                    clientSocket.setTcpNoDelay(true);
                    StringBuilder buffer = new StringBuilder(); // Buffer to accumulate incoming data

                    int charRead;
                    while ((charRead = reader.read()) != -1) {
                        char currentChar = (char) charRead;
                        buffer.append(currentChar);

                        if (currentChar == '\u0003') {
                            String fullMessage = buffer.toString();
                            buffer.setLength(0); // Clear the buffer after extracting the message

                            // Ensure the message starts with 02 and ends with 03
                            int startIndex = fullMessage.indexOf('\u0002');
                            int endIndex = fullMessage.indexOf('\u0003');
                            if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
                                // Extract the message, including 02 and 03
                                String data = fullMessage.substring(startIndex, endIndex + 1);
                                data = bytesToHex(data.getBytes()); // Convert to hex if needed

                                System.out.println("Received data on port " + port + ": " + data);
                                logMessage("Incoming SOCKET PLC->WCS ", port, data);

                                // Send the received data to RabbitMQ
                                channel.basicPublish("", SEND_QUEUE, null,
                                        fullMessage.substring(startIndex, endIndex + 1).getBytes());

                                writer.println("Acknowledged: " + data);
                            }
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error handling connection on port " + port + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
