package com.dssap.client.odata;

import com.dssap.client.SapClient;
import com.dssap.client.SapRequest;
import com.dssap.client.SapResult;
import com.dssap.config.SapProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SAP Gateway / S/4HANA OData 클라이언트.
 * 실시간은 단건 GET, 배치는 $skip/$top 페이지 조회로 사용한다.
 */
@Component
@ConditionalOnProperty(name = "sap.client", havingValue = "odata")
public class ODataSapClient implements SapClient {

    private final SapProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ODataSapClient(SapProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofMillis(properties.getRealtime().getTimeoutMs()));
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(trimSlash(properties.getOdata().getBaseUrl()))
                .defaultHeader(HttpHeaders.AUTHORIZATION, basicAuth())
                .defaultHeader("sap-client", properties.getOdata().getSapClient())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String mode() {
        return "odata";
    }

    @Override
    public SapResult call(SapRequest request) {
        String entity = request.getFunctionName();
        String key = request.getImports().values().stream()
                .findFirst()
                .map(String::valueOf)
                .orElse("");
        String body = restClient.get()
                .uri("/{entity}('{key}')", entity, key)
                .retrieve()
                .body(String.class);
        SapResult result = new SapResult();
        result.setRawMessage(body);
        Map<String, Object> row = jsonToMap(body);
        result.getExports().putAll(row);
        result.putTable("d", List.of(row));
        return result;
    }

    @Override
    public List<Map<String, Object>> readPage(String tableOrEntity, List<String> fields, String where, int skip, int top) {
        String select = (fields == null || fields.isEmpty()) ? null : String.join(",", fields);
        String body = restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/{entity}");
                    uriBuilder.queryParam("$format", "json");
                    uriBuilder.queryParam("$skip", skip);
                    uriBuilder.queryParam("$top", top);
                    if (select != null) {
                        uriBuilder.queryParam("$select", select);
                    }
                    if (where != null && !where.isBlank()) {
                        uriBuilder.queryParam("$filter", where);
                    }
                    return uriBuilder.build(tableOrEntity);
                })
                .retrieve()
                .body(String.class);
        return parseCollection(body);
    }

    private List<Map<String, Object>> parseCollection(String body) {
        try {
            JsonNode root = objectMapper.readTree(body == null ? "{}" : body);
            JsonNode results = root.path("d").path("results");
            if (results.isMissingNode() || results.isNull()) {
                results = root.path("value");
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            if (results.isArray()) {
                for (JsonNode node : results) {
                    rows.add(nodeToMap(node));
                }
            }
            return rows;
        } catch (Exception e) {
            throw new IllegalStateException("OData 응답 파싱 실패", e);
        }
    }

    private Map<String, Object> jsonToMap(String body) {
        try {
            JsonNode root = objectMapper.readTree(body == null ? "{}" : body);
            JsonNode data = root.has("d") ? root.get("d") : root;
            return nodeToMap(data);
        } catch (Exception e) {
            throw new IllegalStateException("OData 단건 파싱 실패", e);
        }
    }

    private Map<String, Object> nodeToMap(JsonNode node) {
        Map<String, Object> map = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (field.getKey().startsWith("__")) {
                continue;
            }
            JsonNode value = field.getValue();
            map.put(field.getKey(), value.isValueNode() ? value.asText() : value.toString());
        }
        return map;
    }

    private String basicAuth() {
        String raw = properties.getOdata().getUsername() + ":" + properties.getOdata().getPassword();
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static String trimSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
