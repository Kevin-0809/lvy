package cn.sunline.lvy.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 报文采集入参。
 */
public class MsgCollectRequest {

    @NotBlank(message = "服务码不能为空")
    private String serviceCode;

    @Min(value = 1, message = "采集数量至少为 1")
    @Max(value = 1000, message = "采集数量最多为 1000")
    private Integer limit = 10;

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}
