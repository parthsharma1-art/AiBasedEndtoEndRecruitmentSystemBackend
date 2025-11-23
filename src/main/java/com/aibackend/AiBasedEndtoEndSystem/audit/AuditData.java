package com.aibackend.AiBasedEndtoEndSystem.audit;


import lombok.Data;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;

import java.time.Instant;

@Data
public class AuditData {

    private Instant createdAt;
    private Instant createdBy;
    private Instant updatedBy;
    private Instant updatedAt;

    public AuditData() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
}

