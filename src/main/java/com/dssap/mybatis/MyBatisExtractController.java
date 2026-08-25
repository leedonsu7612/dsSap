package com.dssap.mybatis;

import com.dssap.batch.entity.SapExtractEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mybatis")
public class MyBatisExtractController {

    private final SapExtractMyBatisService service;

    public MyBatisExtractController(SapExtractMyBatisService service) {
        this.service = service;
    }

    @GetMapping("/extracts")
    public List<SapExtractEntity> search(
            @RequestParam(required = false) String businessKey,
            @RequestParam(required = false) String jobName,
            @RequestParam(required = false) String sourceTable,
            @RequestParam(defaultValue = "50") int limit
    ) {
        ExtractSearchCondition condition = new ExtractSearchCondition();
        condition.setBusinessKey(businessKey);
        condition.setJobName(jobName);
        condition.setSourceTable(sourceTable);
        condition.setLimit(limit);
        return service.search(condition);
    }

    @GetMapping("/extracts/count")
    public Map<String, Object> count() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("count", service.countAll());
        body.put("access", "mybatis");
        return body;
    }

    @GetMapping("/extracts/{id}")
    public ResponseEntity<SapExtractEntity> findById(@PathVariable Long id) {
        SapExtractEntity row = service.findById(id);
        if (row == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(row);
    }
}
