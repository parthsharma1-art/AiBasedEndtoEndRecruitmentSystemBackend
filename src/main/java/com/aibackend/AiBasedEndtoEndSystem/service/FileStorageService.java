package com.aibackend.AiBasedEndtoEndSystem.service;

import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final GridFsTemplate gridFsTemplate;
    private final GridFsOperations operations;

    public String storeFile(MultipartFile file) {
        try {
            Document metadata = new Document();
            metadata.put("createdAt", Instant.now());
            ObjectId fileId = gridFsTemplate.store(
                    file.getInputStream(),
                    file.getOriginalFilename(),
                    file.getContentType(),
                    metadata
            );
            return fileId.toString();
        } catch (Exception e) {
            throw new RuntimeException("File upload failed", e);
        }
    }

    public GridFsResource getFile(String fileId) {
        try {
            GridFSFile file = gridFsTemplate.findOne(
                    Query.query(Criteria.where("_id").is(new ObjectId(fileId)))
            );
            if (file == null) {
                throw new RuntimeException("File not found");
            }
            return operations.getResource(file);
        } catch (Exception e) {
            throw new RuntimeException("File retrieval failed", e);
        }
    }
}
