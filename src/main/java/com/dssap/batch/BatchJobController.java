package com.dssap.batch;

import com.dssap.batch.entity.SapExtractEntity;
import com.dssap.common.QueryLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/batch")
public class BatchJobController {

    private static final Logger log = LoggerFactory.getLogger(BatchJobController.class);

    private final SapExtractJobRunner jobRunner;
    private final SapExtractRepository repository;

    public BatchJobController(SapExtractJobRunner jobRunner, SapExtractRepository repository) {
        this.jobRunner = jobRunner;
        this.repository = repository;
    }

    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> run() {
        QueryLog.query(log, "배치 Job 실행", Map.of("jobName", SapExtractJobConfig.JOB_NAME, "trigger", "api"));
        JobExecution execution = jobRunner.run("api");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", execution.getStatus().toString());
        body.put("jobName", SapExtractJobConfig.JOB_NAME);
        body.put("jobId", execution.getJobId());
        body.put("readCount", execution.getStepExecutions().stream().mapToLong(step -> step.getReadCount()).sum());
        body.put("writeCount", execution.getStepExecutions().stream().mapToLong(step -> step.getWriteCount()).sum());
        body.put("storedCount", repository.count());
        QueryLog.data(log, "배치 Job 실행 결과", body);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/records")
    public List<SapExtractEntity> recent() {
        QueryLog.query(log, "적재 데이터 조회", "SELECT TOP 50 * FROM sap_extract ORDER BY extracted_at DESC");
        List<SapExtractEntity> rows = repository.findTop50ByOrderByExtractedAtDesc();
        QueryLog.data(log, "적재 데이터 조회 결과 (" + rows.size() + "건)", rows);
        return rows;
    }
}
