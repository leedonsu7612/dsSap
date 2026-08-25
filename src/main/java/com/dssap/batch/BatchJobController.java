package com.dssap.batch;

import com.dssap.batch.entity.SapExtractEntity;
import com.dssap.common.QueryLog;
import com.dssap.mybatis.SapExtractMyBatisService;
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
    private final SapExtractMyBatisService myBatisService;

    public BatchJobController(SapExtractJobRunner jobRunner, SapExtractMyBatisService myBatisService) {
        this.jobRunner = jobRunner;
        this.myBatisService = myBatisService;
    }

    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> run() {
        QueryLog.query(log, "Batch job run", Map.of("jobName", SapExtractJobConfig.JOB_NAME, "trigger", "api"));
        JobExecution execution = jobRunner.run("api");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", execution.getStatus().toString());
        body.put("jobName", SapExtractJobConfig.JOB_NAME);
        body.put("jobId", execution.getJobId());
        body.put("readCount", execution.getStepExecutions().stream().mapToLong(step -> step.getReadCount()).sum());
        body.put("writeCount", execution.getStepExecutions().stream().mapToLong(step -> step.getWriteCount()).sum());
        body.put("storedCount", myBatisService.countAll());
        body.put("access", "mybatis");
        QueryLog.data(log, "Batch job result", body);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/records")
    public List<SapExtractEntity> recent() {
        return myBatisService.findRecent(50);
    }
}
