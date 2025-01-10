package com.example;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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

public class ArtemisSocketBridge {
    private static String ArtemisHost;
    private static Integer ArtemisPort;
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
            // "C:\\Users\\lucas784\\Desktop\\Bachelorproef\\latex-hogent-BP-LucasVC\\tests\\messaging\\Artemis-server\\listener-config.json");

            File configFile = new File("/opt/jprogram/config.json");

            // Parse JSON file into Config object
            ObjectMapper mapper = new ObjectMapper();
            Config config = mapper.readValue(configFile, Config.class);

            ArtemisHost = config.Artemis.host;
            ArtemisPort = config.Artemis.port;
            login = config.Artemis.login;
            passcode = config.Artemis.login;
            queueNameSend = config.Artemis.queueSend;
            queueNameRecv = config.Artemis.queueRecv;
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

    // Connect to Artemis
    private static Connection connectToArtemis() throws JMSException {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("tcp://" + ArtemisHost + ":" + ArtemisPort);
        Connection connection = factory.createConnection(login, passcode);
        connection.start();
        return connection;
    }

    // Create a TCP server listener
    private static void createListener(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Listening on port " + port);

            // Setup Artemis connection
            Connection ArtemisConnection = connectToArtemis();
            Session session = ArtemisConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(session.createQueue(queueNameSend));
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            MessageConsumer consumer = session.createConsumer(
                    session.createQueue(queueNameRecv), "channel = '" + channel + "'");

            Socket clientSocket = serverSocket.accept();

            // Listen to Artemis messages
            consumer.setMessageListener(msg -> {
                try {
                    if (msg instanceof TextMessage) {
                        String body = ((TextMessage) msg).getText();
                        logMessage("WCS->PLC from queue ", port, body);
                        System.out.println("Received message from Artemis: " + body);
                        try {
                            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                            writer.println(body);
                            logMessage("WCS->PLC ", port, body);
                        } catch (IOException e) {
                            System.err.println("Error sending message to client: " + e.getMessage());
                        }

                    }

                } catch (JMSException e) {
                    System.err.println("Error processing Artemis message: " + e.getMessage());
                }
            });

            // Main loop to accept and handle SOCKET client connections
            while (true) {
                try (InputStream inputStream = clientSocket.getInputStream();) {

                    System.out.println("Socket connection established on port " + port);
                    clientSocket.setTcpNoDelay(true);

                    byte[] buffer = new byte[1024];
                    int bytesRead;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {

                        String data = new String(buffer, 0, bytesRead, "UTF-8");

                        data = bytesToHex(data.getBytes());

                        System.out.println("Received data on port " + port + ": " + data);
                        logMessage("Incoming SOCKET PLC->WCS ", port, data);

                        // Send the received data to Artemis
                        TextMessage message = session.createTextMessage(data);
                        message.setStringProperty("channel", String.valueOf(channel));
                        producer.send(message);
                        System.out.println("Sent message to AMQ channel " + channel);

                    }

                } catch (IOException e) {
                    System.err.println("Error handling connection on port " + port + ": " + e.getMessage());
                } catch (JMSException e) {
                    System.err.println("Error with Artemis: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
