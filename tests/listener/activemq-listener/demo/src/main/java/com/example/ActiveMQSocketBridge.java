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

        // TEST:
        // args = new String[1];
        // args[0] = "1";
        // System.out.println("args " + args[0].trim());

        try {
            channel = Integer.parseInt(args[0].trim());

            if (channel == null || channel == 0) {
                throw new Exception("No valid channel");
            }
            // Path to the JSON configuration file
            // TEST
            // File configFile = new File(
            // "C:\\Users\\lucas784\\Desktop\\Bachelorproef\\latex-hogent-BP-LucasVC\\tests\\messaging\\artemis-server\\listener\\listener-config.json");

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

            Socket clientSocket = serverSocket.accept();

            MessageConsumer consumer = session.createConsumer(
                    session.createQueue(queueNameRecv), "channel = '" + channel + "'");

            // Listen to ActiveMQ messages
            consumer.setMessageListener(msg -> {
                try {
                    if (msg instanceof TextMessage) {
                        String body = ((TextMessage) msg).getText();
                        try {
                            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                            writer.println(body);
                            logMessage("WCS->PLC ", port, body);
                        } catch (IOException e) {
                            System.err.println("Error sending message to client: " + e.getMessage());
                        }

                    }

                } catch (JMSException e) {
                    System.err.println("Error processing ActiveMQ message: " + e.getMessage());
                }
            });

             while (true) {
                try (InputStream inputStream = clientSocket.getInputStream();) {

                    System.out.println("Socket connection established on port " + port);
                    clientSocket.setTcpNoDelay(true);

                    byte[] buffer = new byte[1024];
                    int bytesRead;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {

                        String data = new String(buffer, 0, bytesRead, "UTF-8");

                        data = bytesToHex(data.getBytes(), bytesRead);

                        System.out.println("Received data on port " + port + ": " + data);
                        logMessage("Incoming SOCKET PLC->WCS ", port, data);

                        channel.basicPublish("", SEND_QUEUE, null, data.getBytes());

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
