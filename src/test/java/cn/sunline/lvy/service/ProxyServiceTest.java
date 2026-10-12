package cn.sunline.lvy.service;

import cn.sunline.lvy.dto.EsfAuthResult;
import cn.sunline.lvy.dto.KeyValue;
import cn.sunline.lvy.dto.ProxyRequest;
import cn.sunline.lvy.dto.ProxyResponse;
import cn.sunline.lvy.repository.RequestHistoryRepository;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProxyServiceTest {

    private HttpServer server;
    private String baseUrl;
    private AtomicReference<String> receivedContentType;
    private AtomicReference<String> receivedBody;
    private AtomicReference<String> receivedMicServId;
    private AtomicReference<String> receivedAuthContent;

    @BeforeEach
    void setUp() throws IOException {
        receivedContentType = new AtomicReference<>();
        receivedBody = new AtomicReference<>();
        receivedMicServId = new AtomicReference<>();
        receivedAuthContent = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/echo", exchange -> {
            receivedContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            receivedMicServId.set(exchange.getRequestHeaders().getFirst("micServId"));
            receivedAuthContent.set(exchange.getRequestHeaders().getFirst("authContent"));
            byte[] requestBody = exchange.getRequestBody().readAllBytes();
            receivedBody.set(new String(requestBody, StandardCharsets.UTF_8));
            byte[] responseBody = "<ok>true</ok>".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/xml; charset=utf-8");
            exchange.sendResponseHeaders(200, responseBody.length);
            exchange.getResponseBody().write(responseBody);
            exchange.close();
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsXmlBodyWithXmlContentType() {
        ProxyService service = newProxyService();
        ProxyRequest request = new ProxyRequest();
        request.setMethod("POST");
        request.setUrl(baseUrl + "/echo");
        request.setBodyType("xml");
        request.setBodyContent("<req><name>lvy</name></req>");

        ProxyResponse response = service.send(request);

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(receivedContentType.get()).contains("application/xml");
        assertThat(receivedBody.get()).isEqualTo("<req><name>lvy</name></req>");
    }

    @Test
    void explicitContentTypeHeaderOverridesXmlDefault() {
        ProxyService service = newProxyService();
        ProxyRequest request = new ProxyRequest();
        request.setMethod("POST");
        request.setUrl(baseUrl + "/echo");
        request.setBodyType("xml");
        request.setBodyContent("<req/>");
        request.setHeaders(List.of(new KeyValue("Content-Type", "text/xml; charset=gbk")));

        service.send(request);

        assertThat(receivedContentType.get()).contains("text/xml");
        assertThat(receivedBody.get()).isEqualTo("<req/>");
    }

    @Test
    void rejectsPrivateTargetByDefault() {
        ProxyService service = new ProxyService(new VariableService(),
                new HistoryService(mock(RequestHistoryRepository.class)), mock(EsfAuthService.class));
        ProxyRequest request = new ProxyRequest();
        request.setMethod("GET");
        request.setUrl("http://127.0.0.1:8080/internal");

        assertThatThrownBy(() -> service.send(request))
                .isInstanceOf(cn.sunline.lvy.exception.BizException.class)
                .hasMessageContaining("禁止访问内网地址");
    }

    @Test
    void rejectsResponseLargerThanConfiguredLimit() {
        server.removeContext("/echo");
        server.createContext("/large", exchange -> {
            byte[] responseBody = new byte[1024 * 1024 + 1];
            exchange.sendResponseHeaders(200, responseBody.length);
            exchange.getResponseBody().write(responseBody);
            exchange.close();
        });
        ProxyService service = new ProxyService(new VariableService(),
                new HistoryService(mock(RequestHistoryRepository.class)), mock(EsfAuthService.class),
                true, 1024 * 1024);
        ProxyRequest request = new ProxyRequest();
        request.setMethod("GET");
        request.setUrl(baseUrl + "/large");

        assertThatThrownBy(() -> service.send(request))
                .isInstanceOf(cn.sunline.lvy.exception.BizException.class)
                .hasMessageContaining("响应体超过限制");
    }

    @Test
    void injectsEsfAuthHeadersWhenEnvironmentProvided() {
        EsfAuthService esfAuthService = mock(EsfAuthService.class);
        when(esfAuthService.generate(any())).thenReturn(new EsfAuthResult("528_json", "10530013", "AUTH-XYZ"));
        ProxyService service = new ProxyService(new VariableService(),
                new HistoryService(mock(RequestHistoryRepository.class)), esfAuthService, true);
        ProxyRequest request = new ProxyRequest();
        request.setMethod("POST");
        request.setUrl(baseUrl + "/echo");
        request.setBodyType("xml");
        request.setBodyContent("<req/>");
        request.setEnvironmentId("528");
        request.setMessageType("bzjson");

        service.send(request);

        assertThat(receivedMicServId.get()).isEqualTo("10530013");
        assertThat(receivedAuthContent.get()).isEqualTo("AUTH-XYZ");
    }

    @Test
    void explicitHeaderOverridesEsfInjection() {
        EsfAuthService esfAuthService = mock(EsfAuthService.class);
        when(esfAuthService.generate(any())).thenReturn(new EsfAuthResult("528_json", "10530013", "AUTH-XYZ"));
        ProxyService service = new ProxyService(new VariableService(),
                new HistoryService(mock(RequestHistoryRepository.class)), esfAuthService, true);
        ProxyRequest request = new ProxyRequest();
        request.setMethod("POST");
        request.setUrl(baseUrl + "/echo");
        request.setBodyType("xml");
        request.setBodyContent("<req/>");
        request.setEnvironmentId("528");
        request.setMessageType("bzjson");
        request.setHeaders(List.of(new KeyValue("micServId", "custom-sid")));

        service.send(request);

        assertThat(receivedMicServId.get()).isEqualTo("custom-sid");
        assertThat(receivedAuthContent.get()).isEqualTo("AUTH-XYZ");
    }

    private ProxyService newProxyService() {
        RequestHistoryRepository historyRepository = mock(RequestHistoryRepository.class);
        VariableService variableService = new VariableService();
        HistoryService historyService = new HistoryService(historyRepository);
        return new ProxyService(variableService, historyService, mock(EsfAuthService.class), true);
    }
}
