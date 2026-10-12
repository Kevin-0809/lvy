package cn.sunline.lvy.service;

import cn.sunline.lvy.dto.KeyValue;
import cn.sunline.lvy.dto.ProxyRequest;
import cn.sunline.lvy.dto.ProxyResponse;
import cn.sunline.lvy.exception.BizException;
import cn.sunline.lvy.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 代理请求核心：后端使用 JDK HttpClient 发起真实请求，规避浏览器 CORS。
 */
@Service
public class ProxyService {

    private static final Logger log = LoggerFactory.getLogger(ProxyService.class);

    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final long DEFAULT_MAX_RESPONSE_BYTES = 10L * 1024 * 1024;
    private static final List<String> RESTRICTED_HEADERS = List.of(
            "connection", "content-length", "expect", "host", "upgrade", "transfer-encoding");

    private final HttpClient httpClient;
    private final VariableService variableService;
    private final HistoryService historyService;
    private final boolean allowPrivateHosts;
    private final long maxResponseBytes;

    @Autowired
    public ProxyService(VariableService variableService, HistoryService historyService) {
        this(variableService, historyService, false, DEFAULT_MAX_RESPONSE_BYTES);
    }

    ProxyService(VariableService variableService, HistoryService historyService, boolean allowPrivateHosts) {
        this(variableService, historyService, allowPrivateHosts, DEFAULT_MAX_RESPONSE_BYTES);
    }

    ProxyService(VariableService variableService, HistoryService historyService,
                 boolean allowPrivateHosts, long maxResponseBytes) {
        this.variableService = variableService;
        this.historyService = historyService;
        this.allowPrivateHosts = allowPrivateHosts;
        this.maxResponseBytes = maxResponseBytes;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public ProxyResponse send(ProxyRequest request) {
        Map<String, String> variables = variableService.resolveVariables();

        String method = request.getMethod() == null ? "GET" : request.getMethod().trim().toUpperCase(Locale.ROOT);
        String url = variableService.replace(request.getUrl(), variables);
        List<KeyValue> headers = replacePairs(request.getHeaders(), variables);
        List<KeyValue> params = replacePairs(request.getParams(), variables);
        String bodyContent = variableService.replace(request.getBodyContent(), variables);
        Map<String, String> authConfig = replaceMap(request.getAuthConfig(), variables);

        URI uri = buildUri(url, params);
        validateTarget(uri);
        BodySpec bodySpec = buildBody(request.getBodyType(), bodyContent);

        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(TIMEOUT)
                .method(method, bodySpec.publisher());

        for (KeyValue header : headers) {
            if (!header.isActive() || isBlank(header.getKey())) {
                continue;
            }
            String name = header.getKey().trim();
            if (RESTRICTED_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                continue;
            }
            try {
                builder.header(name, header.getValue() == null ? "" : header.getValue());
            } catch (IllegalArgumentException e) {
                throw new BizException(400, "非法请求头：" + name + "（" + e.getMessage() + "）");
            }
        }

        boolean hasContentType = headers.stream()
                .anyMatch(h -> h.isActive() && !isBlank(h.getKey())
                        && h.getKey().trim().equalsIgnoreCase("Content-Type"));
        if (!hasContentType && bodySpec.contentType() != null) {
            builder.header("Content-Type", bodySpec.contentType());
        }

        applyAuth(builder, headers, request.getAuthType(), authConfig);

        long start = System.nanoTime();
        ProxyResponse response = new ProxyResponse();
        try {
            HttpResponse<InputStream> httpResponse = httpClient.send(builder.build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            long durationMs = (System.nanoTime() - start) / 1_000_000;

            byte[] bytes = readResponse(httpResponse.body());
            String contentType = httpResponse.headers().firstValue("Content-Type").orElse(null);

            response.setStatusCode(httpResponse.statusCode());
            response.setDurationMs(durationMs);
            response.setSize(bytes.length);
            response.setContentType(contentType);
            response.setHeaders(toHeaderPairs(httpResponse.headers().map()));
            response.setBody(decodeBody(bytes, contentType));
        } catch (HttpTimeoutException e) {
            throw new BizException(504, "请求超时（超过 30s）");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(500, "请求被中断");
        } catch (BizException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new BizException(400, "请求地址或参数不合法：" + e.getMessage());
        } catch (Exception e) {
            log.warn("代理请求失败: {}", e.getMessage());
            throw new BizException(502, "请求失败: " + e.getMessage());
        }

        safeRecord(request, response);
        return response;
    }

    private URI buildUri(String rawUrl, List<KeyValue> params) {
        if (isBlank(rawUrl)) {
            throw new BizException(400, "URL 不能为空");
        }
        String url = rawUrl.trim();
        if (!url.contains("://")) {
            url = "http://" + url;
        }
        StringBuilder query = new StringBuilder();
        for (KeyValue param : params) {
            if (!param.isActive() || isBlank(param.getKey())) {
                continue;
            }
            if (query.length() > 0) {
                query.append('&');
            }
            query.append(encode(param.getKey())).append('=').append(encode(param.getValue()));
        }

        String fragment = "";
        int hashIndex = url.indexOf('#');
        if (hashIndex >= 0) {
            fragment = url.substring(hashIndex);
            url = url.substring(0, hashIndex);
        }

        StringBuilder result = new StringBuilder(url);
        if (query.length() > 0) {
            result.append(url.contains("?") ? '&' : '?').append(query);
        }
        result.append(fragment);

        try {
            URI uri = URI.create(result.toString());
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new BizException(400, "URL 不合法: " + rawUrl);
            }
            return uri;
        } catch (IllegalArgumentException e) {
            throw new BizException(400, "URL 不合法: " + rawUrl);
        }
    }

    private void validateTarget(URI uri) {
        if (allowPrivateHosts) {
            return;
        }
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new BizException(400, "仅支持 HTTP/HTTPS 地址");
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                        || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                        || address.isMulticastAddress()) {
                    throw new BizException(400, "禁止访问内网地址");
                }
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(400, "无法解析目标地址");
        }
    }

    private byte[] readResponse(InputStream input) throws Exception {
        try (InputStream in = input; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int read;
            while ((read = in.read(buffer)) >= 0) {
                total += read;
                if (total > maxResponseBytes) {
                    throw new BizException(413, "响应体超过限制（最大 " + maxResponseBytes + " 字节）");
                }
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    private BodySpec buildBody(String bodyType, String bodyContent) {
        String type = bodyType == null ? "none" : bodyType.toLowerCase(Locale.ROOT);
        byte[] content = bodyContent == null ? new byte[0] : bodyContent.getBytes(StandardCharsets.UTF_8);
        switch (type) {
            case "raw":
                return new BodySpec(HttpRequest.BodyPublishers.ofString(bodyContent == null ? "" : bodyContent,
                        StandardCharsets.UTF_8), "application/json; charset=utf-8");
            case "xml":
                return new BodySpec(HttpRequest.BodyPublishers.ofString(bodyContent == null ? "" : bodyContent,
                        StandardCharsets.UTF_8), "application/xml; charset=utf-8");
            case "x-www-form-urlencoded":
                return new BodySpec(HttpRequest.BodyPublishers.ofString(buildUrlEncoded(bodyContent),
                        StandardCharsets.UTF_8), "application/x-www-form-urlencoded; charset=utf-8");
            case "form-data":
                return buildMultipart(bodyContent);
            case "none":
            default:
                if (content.length == 0) {
                    return new BodySpec(HttpRequest.BodyPublishers.noBody(), null);
                }
                return new BodySpec(HttpRequest.BodyPublishers.ofByteArray(content), null);
        }
    }

    private String buildUrlEncoded(String bodyContent) {
        List<KeyValue> pairs = parsePairs(bodyContent);
        StringBuilder sb = new StringBuilder();
        for (KeyValue pair : pairs) {
            if (!pair.isActive() || isBlank(pair.getKey())) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(encode(pair.getKey())).append('=').append(encode(pair.getValue()));
        }
        return sb.toString();
    }

    private BodySpec buildMultipart(String bodyContent) {
        String boundary = "----lvyBoundary" + Long.toHexString(System.nanoTime());
        List<KeyValue> pairs = parsePairs(bodyContent);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            for (KeyValue pair : pairs) {
                if (!pair.isActive() || isBlank(pair.getKey())) {
                    continue;
                }
                String part = "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"" + pair.getKey() + "\"\r\n\r\n"
                        + (pair.getValue() == null ? "" : pair.getValue()) + "\r\n";
                out.write(part.getBytes(StandardCharsets.UTF_8));
            }
            out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new BizException(400, "构建 multipart 请求体失败: " + e.getMessage());
        }
        return new BodySpec(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray()),
                "multipart/form-data; boundary=" + boundary);
    }

    private List<KeyValue> parsePairs(String bodyContent) {
        if (isBlank(bodyContent)) {
            return new ArrayList<>();
        }
        try {
            List<KeyValue> list = JsonUtil.mapper().readValue(bodyContent, new TypeReference<List<KeyValue>>() {
            });
            return list == null ? new ArrayList<>() : list;
        } catch (Exception ignored) {
            // 兼容 {"key":"value"} 形式
            try {
                Map<String, String> map = JsonUtil.parseStringMap(bodyContent);
                List<KeyValue> list = new ArrayList<>();
                map.forEach((k, v) -> list.add(new KeyValue(k, v)));
                return list;
            } catch (Exception e) {
                throw new BizException(400, "表单请求体需为 JSON 数组或对象");
            }
        }
    }

    private void applyAuth(HttpRequest.Builder builder, List<KeyValue> headers,
                           String authType, Map<String, String> authConfig) {
        if (authType == null || headers.stream().anyMatch(h -> h.isActive() && !isBlank(h.getKey())
                && h.getKey().trim().equalsIgnoreCase("Authorization"))) {
            return;
        }
        switch (authType.toLowerCase(Locale.ROOT)) {
            case "bearer":
                String token = authConfig.get("token");
                if (!isBlank(token)) {
                    builder.header("Authorization", "Bearer " + token);
                }
                break;
            case "basic":
                String username = authConfig.getOrDefault("username", "");
                String password = authConfig.getOrDefault("password", "");
                String credential = Base64.getEncoder()
                        .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
                builder.header("Authorization", "Basic " + credential);
                break;
            default:
                break;
        }
    }

    private List<KeyValue> toHeaderPairs(Map<String, List<String>> map) {
        List<KeyValue> pairs = new ArrayList<>();
        map.forEach((name, values) -> {
            if (values == null || values.isEmpty()) {
                pairs.add(new KeyValue(name, ""));
            } else {
                values.forEach(v -> pairs.add(new KeyValue(name, v)));
            }
        });
        return pairs;
    }

    private String decodeBody(byte[] bytes, String contentType) {
        if (bytes.length == 0) {
            return "";
        }
        if (isBinary(contentType)) {
            return "[二进制内容，共 " + bytes.length + " 字节]";
        }
        Charset charset = resolveCharset(contentType);
        return new String(bytes, charset);
    }

    private boolean isBinary(String contentType) {
        if (contentType == null) {
            return false;
        }
        String type = contentType.toLowerCase(Locale.ROOT);
        if (type.startsWith("text/") || type.contains("json") || type.contains("xml")
                || type.contains("javascript") || type.contains("x-www-form-urlencoded")
                || type.contains("charset")) {
            return false;
        }
        return type.startsWith("image/") || type.startsWith("audio/") || type.startsWith("video/")
                || type.contains("octet-stream") || type.contains("pdf") || type.contains("zip")
                || type.contains("gzip") || type.contains("protobuf");
    }

    private Charset resolveCharset(String contentType) {
        if (contentType != null) {
            int idx = contentType.toLowerCase(Locale.ROOT).indexOf("charset=");
            if (idx >= 0) {
                String name = contentType.substring(idx + "charset=".length()).trim();
                int semi = name.indexOf(';');
                if (semi >= 0) {
                    name = name.substring(0, semi).trim();
                }
                name = name.replace("\"", "");
                try {
                    return Charset.forName(name);
                } catch (Exception ignored) {
                    return StandardCharsets.UTF_8;
                }
            }
        }
        return StandardCharsets.UTF_8;
    }

    private List<KeyValue> replacePairs(List<KeyValue> pairs, Map<String, String> variables) {
        List<KeyValue> result = new ArrayList<>();
        if (pairs == null) {
            return result;
        }
        for (KeyValue pair : pairs) {
            KeyValue copy = new KeyValue();
            copy.setKey(variableService.replace(pair.getKey(), variables));
            copy.setValue(variableService.replace(pair.getValue(), variables));
            copy.setDescription(pair.getDescription());
            copy.setEnabled(pair.getEnabled());
            result.add(copy);
        }
        return result;
    }

    private Map<String, String> replaceMap(Map<String, String> map, Map<String, String> variables) {
        Map<String, String> result = new LinkedHashMap<>();
        if (map == null) {
            return result;
        }
        map.forEach((k, v) -> result.put(k, variableService.replace(v, variables)));
        return result;
    }

    private void safeRecord(ProxyRequest request, ProxyResponse response) {
        try {
            historyService.record(request, response);
        } catch (Exception e) {
            log.warn("写入历史失败: {}", e.getMessage());
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record BodySpec(HttpRequest.BodyPublisher publisher, String contentType) {
    }
}
