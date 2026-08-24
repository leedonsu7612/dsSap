package com.dssap.client.jco;

import com.dssap.client.SapClient;
import com.dssap.client.SapRequest;
import com.dssap.client.SapResult;
import com.dssap.config.SapProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SAP JCo RFC 클라이언트.
 * sapjco3.jar 가 classpath 에 있을 때만 실제 연결을 연다 (리플렉션 사용으로 컴파일은 jar 없이 가능).
 *
 * <pre>
 * JCoDestination dest = JCoDestinationManager.getDestination("DS_SAP");
 * JCoFunction fn = dest.getRepository().getFunction("BAPI_MATERIAL_GET_DETAIL");
 * fn.getImportParameterList().setValue("MATERIAL", matnr);
 * fn.execute(dest);
 * </pre>
 */
@Component
@ConditionalOnProperty(name = "sap.client", havingValue = "jco")
public class JcoSapClient implements SapClient {

    private static final Logger log = LoggerFactory.getLogger(JcoSapClient.class);

    private final SapProperties properties;
    private final AtomicBoolean providerRegistered = new AtomicBoolean(false);

    public JcoSapClient(SapProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void connect() {
        requireJco();
        registerDestinationProvider();
        log.info("JCo destination [{}] registered for {}:{}",
                properties.getJco().getDestinationName(),
                properties.getJco().getAshost(),
                properties.getJco().getSysnr());
    }

    @PreDestroy
    public void disconnect() {
        // JCo 풀은 JVM 종료 시 해제된다. DestinationDataProvider 는 프로세스당 1회 등록이 원칙이다.
    }

    @Override
    public String mode() {
        return "jco";
    }

    @Override
    public SapResult call(SapRequest request) {
        try {
            Object destination = destination();
            Object function = function(destination, request.getFunctionName());
            fillImports(function, request.getImports());
            execute(function, destination);
            return toResult(function);
        } catch (Exception e) {
            throw new IllegalStateException("JCo RFC 호출 실패: " + request.getFunctionName(), e);
        }
    }

    @Override
    public List<Map<String, Object>> readPage(String tableOrEntity, List<String> fields, String where, int skip, int top) {
        try {
            Object destination = destination();
            Object function = function(destination, properties.getBatch().getFunctionName());
            fillImports(function, Map.of(
                    "QUERY_TABLE", tableOrEntity,
                    "DELIMITER", "|",
                    "ROWCOUNT", top,
                    "ROWSKIPS", skip
            ));
            if (fields != null) {
                for (String field : fields) {
                    appendTableRow(function, "FIELDS", Map.of("FIELDNAME", field));
                }
            }
            if (where != null && !where.isBlank()) {
                appendTableRow(function, "OPTIONS", Map.of("TEXT", where));
            }
            execute(function, destination);
            SapResult result = toResult(function);
            List<Map<String, Object>> data = result.table("DATA");
            List<Map<String, Object>> fieldsMeta = result.table("FIELDS");
            if (data.isEmpty()) {
                return result.firstTableOrExports();
            }
            return parseRfcReadTable(data, fieldsMeta, fields);
        } catch (Exception e) {
            throw new IllegalStateException("JCo 테이블 페이지 읽기 실패: " + tableOrEntity, e);
        }
    }

    private void fillImports(Object function, Map<String, Object> imports) throws Exception {
        Object importList = invoke(function, "getImportParameterList");
        if (importList == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : imports.entrySet()) {
            setRecordValue(importList, entry.getKey(), entry.getValue());
        }
    }

    private void appendTableRow(Object function, String tableName, Map<String, Object> row) throws Exception {
        Object tables = invoke(function, "getTableParameterList");
        Object table = invoke(tables, "getTable", new Class<?>[]{String.class}, tableName);
        invoke(table, "appendRow");
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            setRecordValue(table, entry.getKey(), entry.getValue());
        }
    }

    private void execute(Object function, Object destination) throws Exception {
        Class<?> destType = classFor("com.sap.conn.jco.JCoDestination");
        function.getClass().getMethod("execute", destType).invoke(function, destination);
    }

    private void setRecordValue(Object record, String name, Object value) throws Exception {
        try {
            invoke(record, "setValue", new Class<?>[]{String.class, Object.class}, name, value);
        } catch (NoSuchMethodException ex) {
            invoke(record, "setValue", new Class<?>[]{String.class, String.class}, name, String.valueOf(value));
        }
    }

    private Object destination() throws Exception {
        Class<?> manager = classFor("com.sap.conn.jco.JCoDestinationManager");
        Method get = manager.getMethod("getDestination", String.class);
        return get.invoke(null, properties.getJco().getDestinationName());
    }

    private Object function(Object destination, String name) throws Exception {
        Object repository = invoke(destination, "getRepository");
        Object function = invoke(repository, "getFunction", new Class<?>[]{String.class}, name);
        if (function == null) {
            throw new IllegalArgumentException("SAP Function Module 을 찾을 수 없습니다: " + name);
        }
        return function;
    }

    private SapResult toResult(Object function) throws Exception {
        SapResult result = new SapResult();
        copyList(invoke(function, "getExportParameterList"), result.getExports());
        Object tables = invoke(function, "getTableParameterList");
        if (tables != null) {
            int count = (int) invoke(tables, "getFieldCount");
            for (int i = 0; i < count; i++) {
                String name = String.valueOf(invoke(tables, "getName", new Class<?>[]{int.class}, i));
                Object table = invoke(tables, "getTable", new Class<?>[]{String.class}, name);
                result.putTable(name, readTable(table));
            }
        }
        return result;
    }

    private void copyList(Object paramList, Map<String, Object> target) throws Exception {
        if (paramList == null) {
            return;
        }
        int count = (int) invoke(paramList, "getFieldCount");
        for (int i = 0; i < count; i++) {
            String name = String.valueOf(invoke(paramList, "getName", new Class<?>[]{int.class}, i));
            target.put(name, invoke(paramList, "getValue", new Class<?>[]{String.class}, name));
        }
    }

    private List<Map<String, Object>> readTable(Object table) throws Exception {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (table == null) {
            return rows;
        }
        int rowCount = (int) invoke(table, "getNumRows");
        int fieldCount = (int) invoke(table, "getFieldCount");
        for (int r = 0; r < rowCount; r++) {
            invoke(table, "setRow", new Class<?>[]{int.class}, r);
            Map<String, Object> row = new LinkedHashMap<>();
            for (int f = 0; f < fieldCount; f++) {
                String name = String.valueOf(invoke(table, "getName", new Class<?>[]{int.class}, f));
                row.put(name, invoke(table, "getValue", new Class<?>[]{String.class}, name));
            }
            rows.add(row);
        }
        return rows;
    }

    /**
     * RFC_READ_TABLE 의 DATA(WA) + FIELDS 메타를 컬럼 Map 으로 변환한다.
     */
    private List<Map<String, Object>> parseRfcReadTable(List<Map<String, Object>> data,
                                                        List<Map<String, Object>> fieldsMeta,
                                                        List<String> requestedFields) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> raw : data) {
            String wa = String.valueOf(raw.getOrDefault("WA", raw.values().iterator().next()));
            String[] parts = wa.split("\\|", -1);
            Map<String, Object> row = new LinkedHashMap<>();
            if (!fieldsMeta.isEmpty()) {
                for (int i = 0; i < fieldsMeta.size() && i < parts.length; i++) {
                    String fieldName = String.valueOf(fieldsMeta.get(i).getOrDefault("FIELDNAME", "COL" + i)).trim();
                    row.put(fieldName, parts[i].trim());
                }
            } else if (requestedFields != null && !requestedFields.isEmpty()) {
                for (int i = 0; i < requestedFields.size() && i < parts.length; i++) {
                    row.put(requestedFields.get(i), parts[i].trim());
                }
            } else {
                row.put("WA", wa);
            }
            rows.add(row);
        }
        return rows;
    }

    private void registerDestinationProvider() {
        if (!providerRegistered.compareAndSet(false, true)) {
            return;
        }
        try {
            Class<?> providerType = classFor("com.sap.conn.jco.ext.DestinationDataProvider");
            Class<?> env = classFor("com.sap.conn.jco.ext.Environment");
            Object provider = Proxy.newProxyInstance(
                    providerType.getClassLoader(),
                    new Class<?>[]{providerType},
                    new DestinationHandler(buildDestinationProperties())
            );
            env.getMethod("registerDestinationDataProvider", providerType).invoke(null, provider);
        } catch (Exception e) {
            throw new IllegalStateException("JCo DestinationDataProvider 등록 실패", e);
        }
    }

    private Properties buildDestinationProperties() {
        SapProperties.Jco jco = properties.getJco();
        Properties props = new Properties();
        props.setProperty("jco.client.ashost", nullToEmpty(jco.getAshost()));
        props.setProperty("jco.client.sysnr", nullToEmpty(jco.getSysnr()));
        props.setProperty("jco.client.client", nullToEmpty(jco.getClient()));
        props.setProperty("jco.client.user", nullToEmpty(jco.getUser()));
        props.setProperty("jco.client.passwd", nullToEmpty(jco.getPasswd()));
        props.setProperty("jco.client.lang", nullToEmpty(jco.getLang()));
        props.setProperty("jco.destination.pool_capacity", String.valueOf(jco.getPoolCapacity()));
        props.setProperty("jco.destination.peak_limit", String.valueOf(jco.getPeakLimit()));
        return props;
    }

    private void requireJco() {
        try {
            classFor("com.sap.conn.jco.JCoDestinationManager");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "sap.client=jco 이지만 sapjco3.jar 가 classpath 에 없습니다. SAP Support Portal에서 JCo를 받아 lib/ 에 두세요.",
                    e
            );
        }
    }

    private static Class<?> classFor(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }

    private static Object invoke(Object target, String method) throws Exception {
        return target.getClass().getMethod(method).invoke(target);
    }

    private static Object invoke(Object target, String method, Class<?>[] types, Object... args) throws Exception {
        return target.getClass().getMethod(method, types).invoke(target, args);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record DestinationHandler(Properties properties) implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("getDestinationProperties".equals(name)) {
                return properties;
            }
            if ("supportsEvents".equals(name)) {
                return false;
            }
            if ("equals".equals(name)) {
                return proxy == args[0];
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("toString".equals(name)) {
                return "DsSapDestinationDataProvider";
            }
            return null;
        }
    }
}
