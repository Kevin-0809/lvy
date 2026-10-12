package cn.sunline.lvy.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 变量替换：支持 {{key}} 语法。
 * 多环境能力已移除，默认无变量来源，未匹配的占位符原样保留。
 */
@Service
public class VariableService {

    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{\\s*([\\w.\\-]+)\\s*}}");

    public Map<String, String> resolveVariables() {
        return Collections.emptyMap();
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
