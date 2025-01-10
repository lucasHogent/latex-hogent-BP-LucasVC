package com.listener;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private static Integer PLC;
    private static int[] ports;

    public static void main(String[] args) {

        // System.out.println("args " + args[0].trim());
        try {
            PLC = Integer.parseInt(args[0].trim());

            if (PLC == null || PLC == 0) {
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
            ports = config.plcChannelPorts.get(PLC.toString());
            logPath = config.logPath;

            ensureLogDirectory(logPath);
            ExecutorService executorService = Executors.newFixedThreadPool(ports.length);
            for (int port : ports) {
                executorService.execute(() -> RabbitMQSocketBridge.createListener(port));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void ensureLogDirectory(String path) throws IOException {
        Files.createDirectories(Paths.get(path));
    }

    public static String bytesToHex(byte[] bytes, int length) {
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < length; i++) {
            hexString.append(String.format("%02X ", bytes[i])); // Convert each byte to a two-character hex value
        }
        return hexString.toString().trim();
    }

    // Log messages
    private static void logMessage(String direction, int port, String message) {
        try (FileWriter writer = new FileWriter(logPath + "/channel_" + PLC + "_channel_" + port + ".log", true)) {
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

        factory.setUsername(login);
        factory.setPassword(passcode);

        return factory.newConnection();
    }

    private static void createListener(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Listening on port " + port);

            // Setup RabbitMQ connection
            Connection rabbitMQConnection = connectToRabbitMQ();
            Channel channel = rabbitMQConnection.createChannel();

            channel.queueDeclare(SEND_QUEUE + port, false, false, false, null);
            channel.queueDeclare(RECV_QUEUE + port, false, false, false, null);

            Socket clientSocket = serverSocket.accept();

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");

                try {
                    // PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

                    byte[] bytesMessage = new byte[1024];
                    bytesMessage = message.getBytes();
                    clientSocket.getOutputStream().write(bytesMessage);
                    // writer.print(bytesMessage);
                    System.out.println("Message from RabbitMQ: " + port + " " + message);
                    logMessage("WCS->PLC ", port, message);
                } catch (IOException e) {
                    System.err.println("Error sending message to client: " + e.getMessage());
                }
            };
            channel.basicConsume(RECV_QUEUE + port, true, deliverCallback, consumerTag -> {
            });

            // Main loop to accept and handle client connections
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

                        channel.basicPublish("", SEND_QUEUE + port, null, data.getBytes());

                    }
                } catch (IOException e) {
                    System.err.println("Error handling connection on port " + port + ": " + e.getMessage());
                    Thread.sleep(10000);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

}
