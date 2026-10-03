package cn.sunline.lvy.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 轻量 JSON 工具：用于把 TEXT 字段与结构化对象互相转换。
 */
public final class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private JsonUtil() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("JSON 序列化失败: " + e.getMessage(), e);
        }
    }

    public static <T> T parse(String json, TypeReference<T> typeRef, T defaultValue) {
        if (json == null || json.isBlank()) {
            return defaultValue;
        }
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static List<cn.sunline.lvy.dto.KeyValue> parseKeyValues(String json) {
        return parse(json, new TypeReference<List<cn.sunline.lvy.dto.KeyValue>>() {
        }, new ArrayList<>());
    }

    public static Map<String, String> parseStringMap(String json) {
        return parse(json, new TypeReference<Map<String, String>>() {
        }, new LinkedHashMap<>());
    }

    public static Map<String, Object> parseObjectMap(String json) {
        return parse(json, new TypeReference<Map<String, Object>>() {
        }, new LinkedHashMap<>());
    }
}
