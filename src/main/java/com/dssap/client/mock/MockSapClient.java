package com.dssap.client.mock;

import com.dssap.client.SapClient;
import com.dssap.client.SapRequest;
import com.dssap.client.SapResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * SAP 접속 없이 실시간/배치 흐름을 검증하기 위한 샘플 데이터.
 * 한글 리터럴은 Windows 기본 인코딩(MS949) 컴파일 깨짐을 피하려고 Unicode escape 사용.
 */
@Component
@ConditionalOnProperty(name = "sap.client", havingValue = "mock", matchIfMissing = true)
public class MockSapClient implements SapClient {

    private static final List<Map<String, Object>> MATERIALS = List.of(
            material("10000001", "FERT", "\uC644\uC81C\uD488 A", "EA"),
            material("10000002", "HALB", "\uBC18\uC81C\uD488 B", "KG"),
            material("10000003", "ROH", "\uC6D0\uC790\uC7AC C", "M"),
            material("10000004", "FERT", "\uC644\uC81C\uD488 D", "EA"),
            material("10000005", "HAWA", "\uC0C1\uD488 E", "EA")
    );

    @Override
    public String mode() {
        return "mock";
    }

    @Override
    public SapResult call(SapRequest request) {
        String function = request.getFunctionName() == null ? "" : request.getFunctionName().toUpperCase(Locale.ROOT);
        SapResult result = new SapResult();
        result.setRawMessage("mock");

        if (function.contains("MATERIAL")) {
            Object matnr = first(request.getImports(), "MATERIAL", "MATNR");
            Map<String, Object> found = MATERIALS.stream()
                    .filter(row -> String.valueOf(row.get("MATNR")).equals(String.valueOf(matnr)))
                    .findFirst()
                    .orElseGet(() -> material(
                            String.valueOf(matnr),
                            "UNKN",
                            "\uBBF8\uB4F1\uB85D \uC790\uC7AC",
                            "EA"));
            result.putExport("MATERIAL_GENERAL_DATA", found);
            result.putTable("MATERIALDATA", List.of(found));
            return result;
        }

        if (function.contains("SALESORDER") || function.contains("SALES_ORDER")) {
            Object vbeln = first(request.getImports(), "SALESDOCUMENT", "VBELN");
            Map<String, Object> order = new LinkedHashMap<>();
            order.put("VBELN", vbeln == null ? "0000000001" : vbeln);
            order.put("AUART", "OR");
            order.put("KUNNR", "1000");
            order.put("NETWR", 1250000);
            order.put("WAERK", "KRW");
            order.put("ERDAT", LocalDate.now().toString());
            result.putTable("SALES_ORDERS", List.of(order));
            return result;
        }

        result.putExport("ECHO", request.getImports());
        return result;
    }

    @Override
    public List<Map<String, Object>> readPage(String tableOrEntity, List<String> fields, String where, int skip, int top) {
        List<Map<String, Object>> source = new ArrayList<>(MATERIALS);
        int from = Math.min(skip, source.size());
        int to = Math.min(skip + top, source.size());
        List<Map<String, Object>> page = new ArrayList<>();
        for (Map<String, Object> row : source.subList(from, to)) {
            if (fields == null || fields.isEmpty()) {
                page.add(row);
                continue;
            }
            Map<String, Object> filtered = new LinkedHashMap<>();
            for (String field : fields) {
                filtered.put(field, row.get(field));
            }
            page.add(filtered);
        }
        return page;
    }

    private static Object first(Map<String, Object> imports, String... keys) {
        for (String key : keys) {
            if (imports.containsKey(key)) {
                return imports.get(key);
            }
        }
        return imports.isEmpty() ? null : imports.values().iterator().next();
    }

    private static Map<String, Object> material(String matnr, String mtart, String maktx, String meins) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("MATNR", matnr);
        row.put("MTART", mtart);
        row.put("MAKTX", maktx);
        row.put("MATKL", "01");
        row.put("MEINS", meins);
        row.put("ERSDA", "20240101");
        return row;
    }
}
