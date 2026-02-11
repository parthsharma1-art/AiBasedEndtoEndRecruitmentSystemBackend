package com.aibackend.AiBasedEndtoEndSystem.controller;

import com.aibackend.AiBasedEndtoEndSystem.service.FileStorageService;
import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final GridFsTemplate gridFsTemplate;
    private final FileStorageService fileStorageService;

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getFile(@PathVariable String id) {

        GridFSFile file = gridFsTemplate.findOne(
                Query.query(Criteria.where("_id").is(new ObjectId(id)))
        );

        if (file == null) {
            log.info("File not found");
            return ResponseEntity.notFound().build();
        }

        GridFsResource resource = fileStorageService.getFile(id);

        try {
            byte[] data = IOUtils.toByteArray(resource.getInputStream());

            String contentType = "application/octet-stream";
            if (file.getMetadata() != null &&
                    file.getMetadata().get("_contentType") != null) {
                contentType = file.getMetadata()
                        .get("_contentType")
                        .toString();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(data);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
