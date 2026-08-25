package com.dssap;

import com.dssap.mybatis.SapExtractMyBatisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SapFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SapExtractMyBatisService myBatisService;

    @Test
    void realtimeMaterial_returnsImmediately() throws Exception {
        mockMvc.perform(get("/api/realtime/materials/10000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode", is("mock")))
                .andExpect(jsonPath("$.functionName", is("BAPI_MATERIAL_GET_DETAIL")))
                .andExpect(jsonPath("$.rows[0].MATNR", is("10000001")));
    }

    @Test
    void batchRun_extractsAllMockRows() throws Exception {
        mockMvc.perform(post("/api/batch/run").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.writeCount", greaterThan(0)))
                .andExpect(jsonPath("$.access", is("mybatis")));

        assertTrue(myBatisService.countAll() >= 5);

        mockMvc.perform(get("/api/mybatis/extracts").param("sourceTable", "MARA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sourceTable", is("MARA")));
    }
}
