package com.listener;

import java.util.Map;

class Config {
    public RabbitMQConfig broker;

    public Map<String, Integer> channelPorts;

    public String logPath;
}