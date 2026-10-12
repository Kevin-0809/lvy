package cn.sunline.lvy.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 报文采集入参：一次可提交多个服务码（S码），每个服务码按 bzjson / soap 各采集 limit 条。
 */
public class MsgCollectRequest {

    @NotEmpty(message = "服务码不能为空")
    private List<String> serviceCodes;

    @Min(value = 1, message = "采集数量至少为 1")
    @Max(value = 1000, message = "采集数量最多为 1000")
    private Integer limit = 10;

    public List<String> getServiceCodes() {
        return serviceCodes;
    }

    public void setServiceCodes(List<String> serviceCodes) {
        this.serviceCodes = serviceCodes;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}
