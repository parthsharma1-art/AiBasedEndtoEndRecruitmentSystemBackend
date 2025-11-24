package com.aibackend.AiBasedEndtoEndSystem.entity;


import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "auto_increment_entity")
public  class AutoIncrementEntity {
    @Id
    private String id;
    private Long value;
    private Instant updatedAt;
}

