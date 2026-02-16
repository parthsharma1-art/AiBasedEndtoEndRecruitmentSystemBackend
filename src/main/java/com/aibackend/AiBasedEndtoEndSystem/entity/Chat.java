package com.aibackend.AiBasedEndtoEndSystem.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Document(collection = "chats")
public class Chat {
    @Id
    private String id;
    private String recruiterId;
    private String candidateId;
    private List<ChatData> chatData;
    private Instant createdAt;
    private String createdBy;
    private Instant updateAt;
    private String updatedBy;

    @Data
    public static class ChatData {
        private String messageId;
        private String message;
        private Instant createdAt;
        private String createdBy;
        private Source source;
    }


    public enum Source {
        RECRUITER, CANDIDATE
    }

}
