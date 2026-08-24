package com.dssap.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SapExtractJobRunner {

    private static final Logger log = LoggerFactory.getLogger(SapExtractJobRunner.class);

    private final JobLauncher jobLauncher;
    private final Job sapExtractJob;

    public SapExtractJobRunner(JobLauncher jobLauncher, Job sapExtractJob) {
        this.jobLauncher = jobLauncher;
        this.sapExtractJob = sapExtractJob;
    }

    public JobExecution run(String trigger) {
        try {
            log.info("SAP 배치 추출 시작 trigger={}", trigger);
            return jobLauncher.run(sapExtractJob, new JobParametersBuilder()
                    .addString("trigger", trigger)
                    .addString("runAt", Instant.now().toString())
                    .toJobParameters());
        } catch (Exception e) {
            throw new IllegalStateException("SAP 배치 잡 실행 실패", e);
        }
    }
}
