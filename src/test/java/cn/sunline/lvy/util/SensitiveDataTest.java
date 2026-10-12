package cn.sunline.lvy.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataTest {

    @Test
    void masksCredentialFieldsWithoutChangingOtherFields() {
        Map<String, String> input = new LinkedHashMap<>();
        input.put("token", "secret-token");
        input.put("password", "secret-password");
        input.put("username", "alice");

        Map<String, String> masked = SensitiveData.maskAuthConfig(input);

        assertThat(masked).containsEntry("token", "••••••••")
                .containsEntry("password", "••••••••")
                .containsEntry("username", "alice");
        assertThat(input.get("token")).isEqualTo("secret-token");
    }
}
