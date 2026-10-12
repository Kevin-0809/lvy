package cn.sunline.lvy.util;

import java.util.LinkedHashMap;
import java.util.Map;

/** Utilities for preventing credentials from being exposed in API responses. */
public final class SensitiveData {

    public static final String MASK = "••••••••";

    private SensitiveData() {
    }

    public static Map<String, String> maskAuthConfig(Map<String, String> authConfig) {
        Map<String, String> result = new LinkedHashMap<>();
        if (authConfig == null) {
            return result;
        }
        authConfig.forEach((key, value) -> {
            if ("token".equalsIgnoreCase(key) || "password".equalsIgnoreCase(key)) {
                result.put(key, value == null || value.isBlank() ? "" : MASK);
            } else {
                result.put(key, value);
            }
        });
        return result;
    }
}
