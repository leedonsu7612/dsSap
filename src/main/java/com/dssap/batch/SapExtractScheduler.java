package com.dssap.batch;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "sap.batch.enabled", havingValue = "true", matchIfMissing = true)
public class SapExtractScheduler {

    private final SapExtractJobRunner jobRunner;

    public SapExtractScheduler(SapExtractJobRunner jobRunner) {
        this.jobRunner = jobRunner;
    }

    @Scheduled(cron = "${sap.batch.cron}")
    public void runOnSchedule() {
        jobRunner.run("scheduler");
    }
}
