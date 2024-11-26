package com.example;

import com.fasterxml.jackson.annotation.JsonProperty;

class ActiveMQConfig {
    public String host;
    public int port;
    public String login;
    public String passcode;

    @JsonProperty("queue-send")
    public String queueSend;

    @JsonProperty("queue-recv")
    public String queueRecv;
}
