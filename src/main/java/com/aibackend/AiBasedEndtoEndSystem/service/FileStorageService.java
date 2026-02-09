package com.aibackend.AiBasedEndtoEndSystem.service;

import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
//import org.springframework.data.mongodb.gridfs.GridFSFile;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FileStorageService {

    @Autowired
    private GridFsTemplate gridFsTemplate;


    public String storeFile(org.springframework.web.multipart.MultipartFile file) {
        try {
            ObjectId fileId = gridFsTemplate.store(
                    file.getInputStream(),
                    file.getOriginalFilename(),
                    file.getContentType()
            );
            return fileId.toString();
        } catch (Exception e) {
            log.error("Failed to store the file", e);
            throw new RuntimeException(e);
        }
    }

    public GridFsResource getFileById(String id) {
        try {
            GridFSFile file = gridFsTemplate.findOne(
                    Query.query(Criteria.where("_id").is(new ObjectId(id)))
            );

            if (file == null) {
                throw new RuntimeException("File not found");
            }

            return gridFsTemplate.getResource(file);

        } catch (Exception e) {
            log.error("Failed to retrieve file", e);
            throw new RuntimeException(e);
        }
    }
}
