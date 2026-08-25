package com.dssap.batch;

import com.dssap.batch.entity.SapExtractEntity;
import com.dssap.client.SapClient;
import com.dssap.config.SapProperties;
import com.dssap.mybatis.SapExtractMyBatisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Configuration
public class SapExtractJobConfig {

    public static final String JOB_NAME = "sapExtractJob";

    @Bean
    public Job sapExtractJob(JobRepository jobRepository, Step sapExtractStep, SapProperties sapProperties) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .listener(new JobExecutionListener() {
                    @Override
                    public void beforeJob(JobExecution jobExecution) {
                        Path out = Path.of(sapProperties.getBatch().getOutputDir(), "sap-extract.jsonl");
                        try {
                            Files.createDirectories(out.getParent());
                            Files.deleteIfExists(out);
                        } catch (IOException e) {
                            throw new IllegalStateException("배치 출력 파일 초기화 실패", e);
                        }
                    }
                })
                .start(sapExtractStep)
                .build();
    }

    @Bean
    public Step sapExtractStep(JobRepository jobRepository,
                               PlatformTransactionManager transactionManager,
                               SapClient sapClient,
                               SapProperties sapProperties,
                               SapExtractMyBatisService myBatisService,
                               ObjectMapper objectMapper) {
        SapProperties.Batch batch = sapProperties.getBatch();
        return new StepBuilder("sapExtractStep", jobRepository)
                .<Map<String, Object>, SapExtractEntity>chunk(batch.getChunkSize(), transactionManager)
                .reader(new SapPagingItemReader(sapClient, batch))
                .processor(new SapExtractProcessor(batch, objectMapper, JOB_NAME))
                .writer(new SapExtractItemWriter(myBatisService, batch))
                .build();
    }
}
