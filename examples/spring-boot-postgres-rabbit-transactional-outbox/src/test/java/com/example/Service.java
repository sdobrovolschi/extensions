package com.example;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
class Service {

    private final Entities entities;
    private final EventPublisher eventPublisher;

    @Transactional
    void execute(Entity entity) {
        entities.save(entity);
        entity.events().forEach(eventPublisher::publish);
    }
}
