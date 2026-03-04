package com.consentledger.global.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.TreeMap;

@Slf4j
public final class JsonUtils {

    private static final ObjectMapper CANONICAL_MAPPER = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    private JsonUtils() {}

    public static String toCanonicalJson(Object obj) {
        try {
            return CANONICAL_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize to canonical JSON", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> fromJson(String json) {
        try {
            return CANONICAL_MAPPER.readValue(json, Map.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize JSON", e);
        }
    }

    public static String sortedJson(Map<String, Object> map) {
        try {
            return CANONICAL_MAPPER.writeValueAsString(new TreeMap<>(map));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize sorted JSON", e);
        }
    }
}
