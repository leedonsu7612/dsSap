package com.dssap.realtime;

import com.dssap.client.SapClient;
import com.dssap.client.SapRequest;
import com.dssap.client.SapResult;
import com.dssap.common.QueryLog;
import com.dssap.config.SapProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 실시간 조회: 호출 즉시 SAP 에 동기 RFC/OData 를 날리고 결과를 그대로 반환한다.
 * 캐시·스케줄 없이 요청-응답 한 건이다.
 */
@Service
public class RealtimeSapService {

    private static final Logger log = LoggerFactory.getLogger(RealtimeSapService.class);

    private final SapClient sapClient;
    private final SapProperties properties;

    public RealtimeSapService(SapClient sapClient, SapProperties properties) {
        this.sapClient = sapClient;
        this.properties = properties;
    }

    public RealtimeResponse getMaterial(String materialNumber) {
        SapRequest request = SapRequest.function(properties.getRealtime().getMaterialBapi())
                .importParam("MATERIAL", materialNumber)
                .importParam("MATNR", materialNumber);
        return execute(request);
    }

    public RealtimeResponse getSalesOrder(String salesDocument) {
        SapRequest request = SapRequest.function(properties.getRealtime().getSalesOrderBapi())
                .importParam("SALESDOCUMENT", salesDocument)
                .importParam("VBELN", salesDocument);
        return execute(request);
    }

    public RealtimeResponse callFunction(String functionName, Map<String, Object> imports) {
        SapRequest request = SapRequest.function(functionName);
        if (imports != null) {
            imports.forEach(request::importParam);
        }
        return execute(request);
    }

    private RealtimeResponse execute(SapRequest request) {
        long started = System.currentTimeMillis();
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("mode", sapClient.mode());
        query.put("function", request.getFunctionName());
        query.put("params", request.getImports());
        QueryLog.query(log, "실시간 SAP 조회", query);

        SapResult result = sapClient.call(request);
        RealtimeResponse response = RealtimeResponse.from(sapClient.mode(), request.getFunctionName(), result);
        log.info("실시간 SAP 조회 완료 elapsedMs={}", System.currentTimeMillis() - started);
        QueryLog.data(log, "실시간 조회 결과", response);
        return response;
    }
}
