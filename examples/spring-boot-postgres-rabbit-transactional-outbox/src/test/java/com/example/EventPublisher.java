package com.example;

import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;

@MessagingGateway
interface EventPublisher {

    @Gateway(requestChannel = "events")
    void publish(Event event);
}
