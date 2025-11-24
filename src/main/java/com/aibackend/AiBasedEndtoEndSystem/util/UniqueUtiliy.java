package com.aibackend.AiBasedEndtoEndSystem.util;


import com.aibackend.AiBasedEndtoEndSystem.entity.AutoIncrementEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.text.MessageFormat;

@Component
public class UniqueUtiliy {

    @Autowired
    private MongoTemplate mongoTemplate;

    public String getNextNumber(String basePath, String prefix) {
        Long val = getNextNumber(basePath, 1);
        if (!ObjectUtils.isEmpty(prefix)) {
            return MessageFormat.format("{0}_{1}", prefix, String.valueOf(val));
        }
        return val.toString();
    }
    private Long getNextNumber(String basePath, Integer count) {
        Query query = new Query(Criteria.where("_id").is(basePath));
        Update update = new Update().inc("value", count);
        AutoIncrementEntity updatedDocument = mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().returnNew(true).upsert(true), AutoIncrementEntity.class,
                "auto_increment_entity");
        return updatedDocument.getValue() + 10000L;
    }

}
