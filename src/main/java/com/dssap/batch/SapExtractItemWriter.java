package com.dssap.batch;

import com.dssap.batch.entity.SapExtractEntity;
import com.dssap.common.QueryLog;
import com.dssap.config.SapProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class SapExtractItemWriter implements ItemWriter<SapExtractEntity> {

    private static final Logger log = LoggerFactory.getLogger(SapExtractItemWriter.class);

    private final SapExtractRepository repository;
    private final Path outputFile;

    public SapExtractItemWriter(SapExtractRepository repository, SapProperties.Batch batch) {
        this.repository = repository;
        this.outputFile = Path.of(batch.getOutputDir(), "sap-extract.jsonl");
    }

    @Override
    public void write(Chunk<? extends SapExtractEntity> chunk) throws Exception {
        repository.saveAll(chunk.getItems());
        QueryLog.data(log, "배치 DB 저장 (" + chunk.size() + "건)", chunk.getItems());
        Files.createDirectories(outputFile.getParent());
        StringBuilder lines = new StringBuilder();
        for (SapExtractEntity item : chunk) {
            lines.append(item.getPayloadJson()).append(System.lineSeparator());
        }
        Files.writeString(
                outputFile,
                lines.toString(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
    }
}
