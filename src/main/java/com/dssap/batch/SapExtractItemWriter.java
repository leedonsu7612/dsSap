package com.dssap.batch;

import com.dssap.batch.entity.SapExtractEntity;
import com.dssap.common.QueryLog;
import com.dssap.config.SapProperties;
import com.dssap.mybatis.SapExtractMyBatisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class SapExtractItemWriter implements ItemWriter<SapExtractEntity> {

    private static final Logger log = LoggerFactory.getLogger(SapExtractItemWriter.class);

    private final SapExtractMyBatisService myBatisService;
    private final Path outputFile;

    public SapExtractItemWriter(SapExtractMyBatisService myBatisService, SapProperties.Batch batch) {
        this.myBatisService = myBatisService;
        this.outputFile = Path.of(batch.getOutputDir(), "sap-extract.jsonl");
    }

    @Override
    public void write(Chunk<? extends SapExtractEntity> chunk) throws Exception {
        List<SapExtractEntity> items = new ArrayList<>(chunk.getItems());
        myBatisService.saveAll(items);
        QueryLog.data(log, "Batch DB save MyBatis (" + chunk.size() + " rows)", items);
        Files.createDirectories(outputFile.getParent());
        StringBuilder lines = new StringBuilder();
        for (SapExtractEntity item : items) {
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
