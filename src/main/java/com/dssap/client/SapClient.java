package com.dssap.client;

import java.util.List;
import java.util.Map;

/**
 * SAP 조회의 공통 진입점.
 * 실시간(요청 즉시 1건/소수 건)과 배치(페이지 단위 대량)가 같은 클라이언트를 사용한다.
 */
public interface SapClient {

    String mode();

    /**
     * 동기 RFC/BAPI 또는 OData 호출. 실시간 조회에 사용한다.
     */
    SapResult call(SapRequest request);

    /**
     * 테이블/엔티티를 페이지 단위로 읽는다. 배치 추출에 사용한다.
     */
    List<Map<String, Object>> readPage(String tableOrEntity, List<String> fields, String where, int skip, int top);
}
