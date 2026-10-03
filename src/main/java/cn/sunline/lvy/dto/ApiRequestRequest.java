package cn.sunline.lvy.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 新建 / 更新已保存请求入参。
 */
public class ApiRequestRequest {

    private Long collectionId;

    @NotBlank(message = "请求名称不能为空")
    private String name;

    private String method = "GET";

    private String url;

    private List<KeyValue> headers = new ArrayList<>();

    private List<KeyValue> params = new ArrayList<>();

    private String bodyType = "none";

    private String bodyContent;

    private String authType = "none";

    private Map<String, String> authConfig = new LinkedHashMap<>();

    public Long getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(Long collectionId) {
        this.collectionId = collectionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<KeyValue> getHeaders() {
        return headers;
    }

    public void setHeaders(List<KeyValue> headers) {
        this.headers = headers;
    }

    public List<KeyValue> getParams() {
        return params;
    }

    public void setParams(List<KeyValue> params) {
        this.params = params;
    }

    public String getBodyType() {
        return bodyType;
    }

    public void setBodyType(String bodyType) {
        this.bodyType = bodyType;
    }

    public String getBodyContent() {
        return bodyContent;
    }

    public void setBodyContent(String bodyContent) {
        this.bodyContent = bodyContent;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public Map<String, String> getAuthConfig() {
        return authConfig;
    }

    public void setAuthConfig(Map<String, String> authConfig) {
        this.authConfig = authConfig;
    }
}
