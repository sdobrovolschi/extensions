package com.example;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Table("entity")
class Entity implements Persistable<UUID> {

    @Id
    private final UUID id;
    private final String value;

    Entity(UUID id, String value) {
        this.id = id;
        this.value = value;
        events.add(new Event(id, value));
    }

    @Transient
    List<Event> events = new ArrayList<>();

    List<Event> events() {
        return events;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean isNew() {
        return true;
    }
}
