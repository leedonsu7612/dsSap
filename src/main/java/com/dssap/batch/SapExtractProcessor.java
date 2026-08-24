package com.dssap.batch;

import com.dssap.batch.entity.SapExtractEntity;
import com.dssap.config.SapProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.batch.item.ItemProcessor;

import java.time.Instant;
import java.util.Map;

public class SapExtractProcessor implements ItemProcessor<Map<String, Object>, SapExtractEntity> {

    private final SapProperties.Batch batch;
    private final ObjectMapper objectMapper;
    private final String jobName;

    public SapExtractProcessor(SapProperties.Batch batch, ObjectMapper objectMapper, String jobName) {
        this.batch = batch;
        this.objectMapper = objectMapper;
        this.jobName = jobName;
    }

    @Override
    public SapExtractEntity process(Map<String, Object> item) {
        SapExtractEntity entity = new SapExtractEntity();
        entity.setJobName(jobName);
        entity.setSourceTable(batch.getTableName());
        entity.setBusinessKey(resolveKey(item));
        entity.setPayloadJson(toJson(item));
        entity.setExtractedAt(Instant.now());
        return entity;
    }

    private String resolveKey(Map<String, Object> item) {
        for (String candidate : new String[]{"MATNR", "VBELN", "KUNNR", "EBELN", "id", "ID"}) {
            if (item.containsKey(candidate) && item.get(candidate) != null) {
                return String.valueOf(item.get(candidate));
            }
        }
        return String.valueOf(item.hashCode());
    }

    private String toJson(Map<String, Object> item) {
        try {
            return objectMapper.writeValueAsString(item);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("배치 레코드 JSON 변환 실패", e);
        }
    }
}
