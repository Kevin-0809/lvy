package cn.sunline.lvy.service;

import cn.sunline.lvy.entity.Environment;
import cn.sunline.lvy.repository.EnvironmentRepository;
import cn.sunline.lvy.util.JsonUtil;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 变量替换：支持 {{key}} 语法。
 * 若指定 environmentId，仅使用该环境变量；否则合并所有环境变量（后定义的同名变量覆盖前者）。
 */
@Service
public class VariableService {

    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{\\s*([\\w.\\-]+)\\s*}}");

    private final EnvironmentRepository environmentRepository;

    public VariableService(EnvironmentRepository environmentRepository) {
        this.environmentRepository = environmentRepository;
    }

    public Map<String, String> resolveVariables(Long environmentId) {
        Map<String, String> result = new LinkedHashMap<>();
        if (environmentId != null) {
            Environment env = environmentRepository.findById(environmentId).orElse(null);
            if (env != null) {
                result.putAll(JsonUtil.parseStringMap(env.getVariables()));
            }
            return result;
        }
        List<Environment> environments = environmentRepository.findAllByOrderByCreatedAtAsc();
        for (Environment env : environments) {
            result.putAll(JsonUtil.parseStringMap(env.getVariables()));
        }
        return result;
    }

    public String replace(String text, Map<String, String> variables) {
        if (text == null || text.isEmpty() || variables == null || variables.isEmpty()) {
            return text;
        }
        Matcher matcher = VAR_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = variables.get(key);
            if (value == null) {
                value = matcher.group(0);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
