package com.dssap.mybatis;

import com.dssap.batch.entity.SapExtractEntity;
import com.dssap.common.QueryLog;
import com.dssap.mybatis.mapper.SapExtractMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SapExtractMyBatisService {

    private static final Logger log = LoggerFactory.getLogger(SapExtractMyBatisService.class);

    private final SapExtractMapper mapper;

    public SapExtractMyBatisService(SapExtractMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public int saveAll(List<? extends SapExtractEntity> items) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        List<SapExtractEntity> rows = new ArrayList<>(items);
        QueryLog.query(log, "MyBatis insertAll", "INSERT INTO sap_extract ... (" + rows.size() + " rows)");
        int written = mapper.insertAll(rows);
        QueryLog.data(log, "MyBatis insertAll result", rows);
        return written;
    }

    public long countAll() {
        return mapper.countAll();
    }

    public List<SapExtractEntity> findRecent(int limit) {
        QueryLog.query(log, "MyBatis findRecent", "SELECT ... ORDER BY extracted_at DESC FETCH FIRST " + limit);
        List<SapExtractEntity> rows = mapper.findRecent(limit);
        QueryLog.data(log, "MyBatis findRecent result (" + rows.size() + " rows)", rows);
        return rows;
    }

    public List<SapExtractEntity> search(ExtractSearchCondition condition) {
        QueryLog.query(log, "MyBatis search (dynamic SQL)", condition);
        List<SapExtractEntity> rows = mapper.search(condition);
        QueryLog.data(log, "MyBatis search result (" + rows.size() + " rows)", rows);
        return rows;
    }

    public SapExtractEntity findById(Long id) {
        QueryLog.query(log, "MyBatis findById", id);
        SapExtractEntity row = mapper.findById(id);
        QueryLog.data(log, "MyBatis findById result", row);
        return row;
    }
}
