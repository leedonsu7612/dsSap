package com.dssap.batch;

import com.dssap.client.SapClient;
import com.dssap.config.SapProperties;
import com.dssap.common.QueryLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SapPagingItemReader implements ItemStreamReader<Map<String, Object>> {

    private static final Logger log = LoggerFactory.getLogger(SapPagingItemReader.class);
    private static final String SKIP_KEY = "sap.reader.skip";

    private final SapClient sapClient;
    private final SapProperties.Batch batch;

    private int skip;
    private Iterator<Map<String, Object>> currentPage = Collections.emptyIterator();
    private boolean lastPageLoaded;

    public SapPagingItemReader(SapClient sapClient, SapProperties.Batch batch) {
        this.sapClient = sapClient;
        this.batch = batch;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        this.skip = executionContext.containsKey(SKIP_KEY) ? executionContext.getInt(SKIP_KEY) : 0;
        this.lastPageLoaded = false;
        this.currentPage = Collections.emptyIterator();
        log.info("Batch SAP extract start table={} skip={} pageSize={}", batch.getTableName(), skip, batch.getPageSize());
    }

    @Override
    public Map<String, Object> read() {
        if (!currentPage.hasNext()) {
            if (lastPageLoaded) {
                return null;
            }
            List<Map<String, Object>> page = sapClient.readPage(
                    batch.getTableName(),
                    batch.getFields(),
                    batch.getWhere(),
                    skip,
                    batch.getPageSize()
            );
            QueryLog.query(log, "Batch SAP page read", Map.of(
                    "table", batch.getTableName(),
                    "fields", batch.getFields(),
                    "where", batch.getWhere() == null ? "" : batch.getWhere(),
                    "skip", skip,
                    "pageSize", batch.getPageSize()
            ));
            if (page == null || page.isEmpty()) {
                lastPageLoaded = true;
                log.info("[DATA] Batch page empty skip={}", skip);
                return null;
            }
            QueryLog.data(log, "Batch page result (" + page.size() + " rows)", page);
            skip += page.size();
            if (page.size() < batch.getPageSize()) {
                lastPageLoaded = true;
            }
            currentPage = page.iterator();
        }
        return currentPage.next();
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        executionContext.putInt(SKIP_KEY, skip);
    }

    @Override
    public void close() {
        currentPage = Collections.emptyIterator();
    }
}
