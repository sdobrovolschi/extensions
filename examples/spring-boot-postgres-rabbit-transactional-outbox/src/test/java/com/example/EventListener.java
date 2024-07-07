package com.example;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class EventListener {

    private final List<Event> events = new CopyOnWriteArrayList<>();

    @RabbitListener(id = "messages.handling", queues = "messages.handling")
    void listen(Event event) {
        events.add(event);
    }

    public List<Event> getEvents() {
        return events;
    }
}
