package com.dssap.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SapResult {

    private final Map<String, Object> exports = new LinkedHashMap<>();
    private final Map<String, List<Map<String, Object>>> tables = new LinkedHashMap<>();
    private String rawMessage;

    public Map<String, Object> getExports() {
        return exports;
    }

    public Map<String, List<Map<String, Object>>> getTables() {
        return tables;
    }

    public List<Map<String, Object>> table(String name) {
        return tables.getOrDefault(name, List.of());
    }

    public void putExport(String name, Object value) {
        exports.put(name, value);
    }

    public void putTable(String name, List<Map<String, Object>> rows) {
        tables.put(name, rows == null ? new ArrayList<>() : rows);
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public void setRawMessage(String rawMessage) {
        this.rawMessage = rawMessage;
    }

    public List<Map<String, Object>> firstTableOrExports() {
        if (!tables.isEmpty()) {
            return tables.values().iterator().next();
        }
        if (exports.isEmpty()) {
            return List.of();
        }
        return List.of(exports);
    }
}
