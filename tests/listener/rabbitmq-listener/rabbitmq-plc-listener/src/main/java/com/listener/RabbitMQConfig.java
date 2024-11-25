package com.listener;

import com.fasterxml.jackson.annotation.JsonProperty;

class RabbitMQConfig {
    public String host;
    public int port;
    public String login;
    public String passcode;

    @JsonProperty("queue-send")
    public String queueSend;

    @JsonProperty("queue-recv")
    public String queueRecv;
}
