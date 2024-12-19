package com.listener;

import java.util.Map;

class Config {
    public RabbitMQConfig broker;

    public Map<String, int[]> plcChannelPorts;

    public String logPath;
}