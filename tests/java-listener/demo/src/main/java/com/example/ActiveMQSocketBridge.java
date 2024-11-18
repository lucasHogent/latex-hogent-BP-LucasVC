package com.example;

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

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ActiveMQSocketBridge {
    private static String activemqHost;
    private static Integer activemqPort;
    private static String login;
    private static String passcode;
    private static String queueNameSend;
    private static String queueNameRecv;
    private static String logPath;
    private static Integer channel;
    private static Integer port;
    private static Boolean debug;

    public static void main(String[] args) {

        System.out.println("args " + args[0].trim());
        try {
            channel = Integer.parseInt(args[0].trim());

            if (channel == null || channel == 0) {
                throw new Exception("No valid channel");
            }
            // Path to the JSON configuration file
            // File configFile = new File(
            // "C:\\Users\\lucas784\\Desktop\\Bachelorproef\\latex-hogent-BP-LucasVC\\tests\\messaging\\activemq-server\\listener-config.json");

            File configFile = new File("/opt/jprogram/config.json");

            // Parse JSON file into Config object
            ObjectMapper mapper = new ObjectMapper();
            Config config = mapper.readValue(configFile, Config.class);

            activemqHost = config.activemq.host;
            activemqPort = config.activemq.port;
            login = config.activemq.login;
            passcode = config.activemq.login;
            queueNameSend = config.activemq.queueSend;
            queueNameRecv = config.activemq.queueRecv;
            port = config.channelPorts.get(channel.toString());
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
        try (FileWriter writer = new FileWriter(logPath + "/channel" + channel + ".log", true)) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(new Date());
            String logEntry = String.format("%s | %s | Port %d | Message: %s%n", timestamp, direction, port, message);
            writer.write(logEntry);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Connect to ActiveMQ
    private static Connection connectToActiveMQ() throws JMSException {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("tcp://" + activemqHost + ":" + activemqPort);
        Connection connection = factory.createConnection(login, passcode);
        connection.start();
        return connection;
    }

    // Create a TCP server listener
    private static void createListener(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Listening on port " + port);

            // Setup ActiveMQ connection
            Connection activeMQConnection = connectToActiveMQ();
            Session session = activeMQConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(session.createQueue(queueNameSend));
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            MessageConsumer consumer = session.createConsumer(
                    session.createQueue(queueNameRecv), "channel = '" + channel + "'");

            // Listen to ActiveMQ messages
            consumer.setMessageListener(msg -> {
                try {
                    if (msg instanceof TextMessage) {
                        String body = ((TextMessage) msg).getText();
                        logMessage("WCS->PLC from queue ", port, body);
                        System.out.println("Received message from ActiveMQ: " + body);
                        try {
                            PrintWriter writer = new PrintWriter(serverSocket.accept().getOutputStream(), true);
                            writer.println("Message from AMQ: " + body);
                            System.out.println("Forwarded AMQ message to client: " + body);
                            logMessage("AMQ->SOCKET ", port, body);
                        } catch (IOException e) {
                            System.err.println("Error sending message to client: " + e.getMessage());
                        }

                    }

                } catch (JMSException e) {
                    System.err.println("Error processing ActiveMQ message: " + e.getMessage());
                }
            });

            // Main loop to accept and handle client connections
            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                        BufferedReader reader = new BufferedReader(
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

                                // Send the received data to ActiveMQ
                                TextMessage message = session.createTextMessage(data);
                                message.setStringProperty("channel", String.valueOf(channel));
                                producer.send(message);
                                System.out.println("Sent message to AMQ channel " + channel);

                                // Optionally send a response back to the client
                                writer.println("Acknowledged: " + data);
                                System.out.println("Response sent to client (optional)");
                            }
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error handling connection on port " + port + ": " + e.getMessage());
                } catch (JMSException e) {
                    System.err.println("Error with ActiveMQ: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
