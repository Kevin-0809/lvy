package cn.sunline.lvy.dto;

/**
 * 通用键值对，用于 headers / params。
 */
public class KeyValue {

    private String key;
    private String value;
    private String description;
    private Boolean enabled = Boolean.TRUE;

    public KeyValue() {
    }

    public KeyValue(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isActive() {
        return enabled == null || enabled;
    }
}
