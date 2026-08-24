package com.dssap.client;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RFC/BAPI 또는 OData 호출에 넘기는 입력값.
 */
public class SapRequest {

    private String functionName;
    private final Map<String, Object> imports = new LinkedHashMap<>();
    private String tableName;
    private String where;
    private int rowSkip;
    private int rowCount;

    public static SapRequest function(String functionName) {
        SapRequest request = new SapRequest();
        request.functionName = functionName;
        return request;
    }

    public SapRequest importParam(String name, Object value) {
        this.imports.put(name, value);
        return this;
    }

    public String getFunctionName() {
        return functionName;
    }

    public Map<String, Object> getImports() {
        return imports;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getWhere() {
        return where;
    }

    public void setWhere(String where) {
        this.where = where;
    }

    public int getRowSkip() {
        return rowSkip;
    }

    public void setRowSkip(int rowSkip) {
        this.rowSkip = rowSkip;
    }

    public int getRowCount() {
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }
}
