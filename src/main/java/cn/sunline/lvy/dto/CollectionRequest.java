package cn.sunline.lvy.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 新建 / 更新集合入参。
 */
public class CollectionRequest {

    @NotBlank(message = "集合名称不能为空")
    private String name;

    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
