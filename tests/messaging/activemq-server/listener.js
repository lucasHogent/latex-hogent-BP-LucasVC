const net = require('net');
const stompit = require('stompit');
const fs = require('fs');
const path = require('path');

// Load configuration
const config = JSON.parse(fs.readFileSync('/opt/jprogram/config.json', 'utf8'));
const { host, port, login, passcode, queue } = config.activemq;
const ports = config.ports;
const logPath = config.logPath;

// Ensure log directory exists
if (!fs.existsSync(logPath)) {
    fs.mkdirSync(logPath, { recursive: true });
}

const connectOptions = {
    host: host,
    port: port,
    connectHeaders: {
        'host': '/',
        'login': login,
        'passcode': passcode,
    }
};

// Log function to write incoming/outgoing messages
function logMessage(direction, port, message) {
    const logFile = path.join(logPath, 'messages.log');
    const timestamp = new Date().toISOString();
    const logEntry = `${timestamp} | ${direction} | Port ${port} | Message: ${message}\n`;
    fs.appendFileSync(logFile, logEntry);
}

// Create listener and ActiveMQ connection for each port
ports.forEach((port) => {
    // Create TCP server for incoming socket connections
    const server = net.createServer((socket) => {
        console.log(`Listening on port ${port}`);

        // Handle incoming data on socket
        socket.on('data', (data) => {
            console.log(`Received data on port ${port}: ${data}`);
            logMessage("Incoming", port, data);

            // Send the data to ActiveMQ with a 'port' property
            stompit.connect(connectOptions, (error, client) => {
                if (error) {
                    console.error(`Connection error for port ${port}:`, error.message);
                    return;
                }

                const sendHeaders = {
                    destination: queue,
                    'content-type': 'text/plain',
                    port: port  // Custom property to filter by port
                };

                const frame = client.send(sendHeaders);
                frame.write(data);
                frame.end();

                client.disconnect();
            });
        });

        // Subscribe to ActiveMQ for each port with a selector
        stompit.connect(connectOptions, (error, client) => {
            if (error) {
                console.error(`Subscription error for port ${port}:`, error.message);
                return;
            }

            const subscribeHeaders = {
                destination: queue,
                ack: 'auto',
                selector: `port = '${port}'`  // Filter messages by port property
            };

            client.subscribe(subscribeHeaders, (error, message) => {
                if (error) {
                    console.error(`Message error for port ${port}:`, error.message);
                    return;
                }

                let body = '';
                message.on('data', (chunk) => {
                    body += chunk;
                });

                message.on('end', () => {
                    logMessage("Outgoing", port, body);
                    socket.write(body); // Send back to socket
                });
            });
        });

        socket.on('end', () => {
            console.log(`Connection closed on port ${port}`);
        });
    });

    // Start server for the port
    server.listen(port, () => console.log(`Server listening on port ${port}`));
});
