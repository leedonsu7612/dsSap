package com.dssap.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;

/**
 * 터미널에 조회 쿼리/결과를 JSON으로 남긴다.
 */
public final class QueryLog {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private QueryLog() {
    }

    public static void query(Logger log, String title, Object query) {
        log.info("[QUERY] {}\n{}", title, toJson(query));
    }

    public static void data(Logger log, String title, Object data) {
        log.info("[DATA] {}\n{}", title, toJson(data));
    }

    private static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }
}
