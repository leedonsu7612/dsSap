package com.dssap.mybatis;

/**
 * MyBatis 동적 조회 조건.
 */
public class ExtractSearchCondition {

    private String businessKey;
    private String jobName;
    private String sourceTable;
    private int limit = 50;

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getSourceTable() {
        return sourceTable;
    }

    public void setSourceTable(String sourceTable) {
        this.sourceTable = sourceTable;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit <= 0 ? 50 : Math.min(limit, 500);
    }
}
