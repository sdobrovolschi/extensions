package com.example;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
interface Entities extends CrudRepository<Entity, UUID> {

}
