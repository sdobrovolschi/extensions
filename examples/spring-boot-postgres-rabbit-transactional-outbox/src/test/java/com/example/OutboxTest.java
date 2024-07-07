package com.example;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils.random;

@SpringBootTest
class OutboxTest {

    @Autowired
    Service service;

    @Autowired
    EventListener eventListener;

    @Test
    void relaying() {
        var entity = new Entity(randomUUID(), random(10));

        service.execute(entity);

        await().untilAsserted(() -> assertThat(eventListener.getEvents())
                .containsExactlyInAnyOrder(new Event(entity.getId(), entity.getValue())));
    }
}
