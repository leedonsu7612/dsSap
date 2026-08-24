package com.dssap.realtime;

import com.dssap.client.SapClient;
import com.dssap.client.SapRequest;
import com.dssap.client.SapResult;
import com.dssap.client.mock.MockSapClient;
import com.dssap.config.SapProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RealtimeSapServiceTest {

    @Test
    void getMaterial_returnsMockRowImmediately() {
        SapProperties properties = new SapProperties();
        RealtimeSapService service = new RealtimeSapService(new MockSapClient(), properties);

        RealtimeResponse response = service.getMaterial("10000001");

        assertEquals("mock", response.getMode());
        assertEquals("BAPI_MATERIAL_GET_DETAIL", response.getFunctionName());
        assertFalse(response.getRows().isEmpty());
        assertEquals("10000001", response.getRows().get(0).get("MATNR"));
    }

    @Test
    void sapClient_readPage_isUsedByBatchPaging() {
        SapClient client = new MockSapClient();
        List<Map<String, Object>> page = client.readPage("MARA", List.of("MATNR", "MTART"), "", 0, 2);
        assertEquals(2, page.size());
        SapResult result = client.call(SapRequest.function("BAPI_SALESORDER_GETLIST").importParam("VBELN", "0000000001"));
        assertFalse(result.firstTableOrExports().isEmpty());
    }
}
