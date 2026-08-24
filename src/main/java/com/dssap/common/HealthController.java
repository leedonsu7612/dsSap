package com.dssap.common;

import com.dssap.client.SapClient;
import com.dssap.config.SapProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final SapClient sapClient;
    private final SapProperties properties;

    public HealthController(SapClient sapClient, SapProperties properties) {
        this.sapClient = sapClient;
        this.properties = properties;
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("sapClient", sapClient.mode());
        body.put("systemId", properties.getSystemId());
        body.put("realtime", Map.of(
                "materialBapi", properties.getRealtime().getMaterialBapi(),
                "timeoutMs", properties.getRealtime().getTimeoutMs()
        ));
        body.put("batch", Map.of(
                "enabled", properties.getBatch().isEnabled(),
                "cron", properties.getBatch().getCron(),
                "tableName", properties.getBatch().getTableName(),
                "pageSize", properties.getBatch().getPageSize()
        ));
        return body;
    }
}
