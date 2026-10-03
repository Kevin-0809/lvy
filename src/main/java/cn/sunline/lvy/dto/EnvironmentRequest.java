package cn.sunline.lvy.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 新建 / 更新环境入参。
 */
public class EnvironmentRequest {

    @NotBlank(message = "环境名称不能为空")
    private String name;

    private Map<String, String> variables = new LinkedHashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, String> variables) {
        this.variables = variables;
    }
}
