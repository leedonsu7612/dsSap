package com.dssap.realtime;

import com.dssap.client.SapResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class RealtimeResponse {

    private String mode;
    private String functionName;
    private Instant fetchedAt;
    private Map<String, Object> exports;
    private List<Map<String, Object>> rows;

    public static RealtimeResponse from(String mode, String functionName, SapResult result) {
        RealtimeResponse response = new RealtimeResponse();
        response.mode = mode;
        response.functionName = functionName;
        response.fetchedAt = Instant.now();
        response.exports = result.getExports();
        response.rows = result.firstTableOrExports();
        return response;
    }

    public String getMode() {
        return mode;
    }

    public String getFunctionName() {
        return functionName;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public Map<String, Object> getExports() {
        return exports;
    }

    public List<Map<String, Object>> getRows() {
        return rows;
    }
}
