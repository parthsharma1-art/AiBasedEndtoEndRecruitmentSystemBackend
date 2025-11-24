package com.aibackend.AiBasedEndtoEndSystem.entity;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "company_details")
public class Company {
    @Id
    private String id;
    private String companyName;
    private String address;
    private String website;
    private String industry;
    private List<String> recruiterIds;

}
