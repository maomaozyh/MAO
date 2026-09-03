package com.mao.maocodemother.ai.mcp;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SSE 传输的 MCP 客户端
 * 支持魔塔（ModelScope）等基于 SSE 传输的 MCP 服务
 * <p>
 * 协议流程：
 * 1. GET 连接 SSE URL，等待 endpoint 事件获取 POST 地址
 * 2. 通过 POST 发送 JSON-RPC 请求
 * 3. 通过 SSE 流接收响应，按 id 匹配
 */
@Slf4j
public class SseMcpClient implements AutoCloseable {

    private final String sseUrl;
    private final Map<String, String> headers;
    private final HttpClient httpClient;
    private final AtomicInteger requestIdCounter = new AtomicInteger(1);
    private final Map<String, CompletableFuture<JSONObject>> pendingRequests = new ConcurrentHashMap<>();

    private volatile String endpointUrl;
    private volatile boolean initialized = false;
    private volatile boolean closed = false;
    private Thread sseThread;

    public SseMcpClient(String sseUrl, Map<String, String> headers) {
        this.sseUrl = sseUrl;
        this.headers = headers;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * 初始化：连接 SSE 流，获取 endpoint 地址
     */
    public synchronized void initialize() throws Exception {
        if (initialized) {
            return;
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(sseUrl))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "text/event-stream");

        if (headers != null) {
            headers.forEach(requestBuilder::header);
        }

        HttpRequest request = requestBuilder.build();

        // 启动 SSE 监听线程
        sseThread = Thread.startVirtualThread(() -> {
            try {
                HttpResponse<java.io.InputStream> response = httpClient.send(
                        request, HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() != 200) {
                    log.error("MCP SSE 连接失败，状态码：{}，URL：{}", response.statusCode(), sseUrl);
                    return;
                }

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                    String line;
                    String eventType = "message";
                    StringBuilder dataBuffer = new StringBuilder();

                    while ((line = reader.readLine()) != null) {
                        if (closed) {
                            break;
                        }

                        if (line.startsWith("event:")) {
                            eventType = line.substring(6).trim();
                        } else if (line.startsWith("data:")) {
                            String data = line.substring(5).trim();
                            if (dataBuffer.length() > 0) {
                                dataBuffer.append('\n');
                            }
                            dataBuffer.append(data);
                        } else if (line.isEmpty()) {
                            // 空行表示事件结束
                            if (dataBuffer.length() > 0) {
                                handleSseEvent(eventType, dataBuffer.toString());
                                dataBuffer.setLength(0);
                            }
                            eventType = "message";
                        }
                    }
                }
            } catch (Exception e) {
                if (!closed) {
                    log.error("MCP SSE 连接异常，URL：{}", sseUrl, e);
                }
            }
        });

        // 等待 endpoint 事件（最多等 10 秒）
        long startTime = System.currentTimeMillis();
        while (endpointUrl == null && System.currentTimeMillis() - startTime < 10000) {
            Thread.sleep(100);
        }

        if (endpointUrl == null) {
            close();
            throw new RuntimeException("MCP SSE 初始化超时，未收到 endpoint 事件，URL：" + sseUrl);
        }

        initialized = true;
        log.info("MCP SSE 客户端初始化成功，endpoint：{}", endpointUrl);
    }

    /**
     * 处理 SSE 事件
     */
    private void handleSseEvent(String eventType, String data) {
        try {
            switch (eventType) {
                case "endpoint" -> {
                    // SSE 传输的 endpoint 事件：告诉客户端往哪里发 POST 请求
                    // data 可能是 URL 字符串，也可能是 JSON
                    String url = data.trim();
                    if (url.startsWith("\"") && url.endsWith("\"")) {
                        url = url.substring(1, url.length() - 1);
                    }
                    // 如果是相对路径，拼接成绝对 URL
                    if (url.startsWith("/")) {
                        URI baseUri = URI.create(sseUrl);
                        endpointUrl = baseUri.getScheme() + "://" + baseUri.getHost() +
                                (baseUri.getPort() > 0 ? ":" + baseUri.getPort() : "") + url;
                    } else {
                        endpointUrl = url;
                    }
                }
                case "message" -> {
                    // JSON-RPC 响应
                    JSONObject response = JSONUtil.parseObj(data);
                    String id = response.getStr("id");
                    if (id != null) {
                        CompletableFuture<JSONObject> future = pendingRequests.remove(id);
                        if (future != null) {
                            future.complete(response);
                        }
                    }
                }
                default -> log.debug("收到 MCP SSE 事件：{}，data：{}", eventType,
                        StrUtil.maxLength(data, 200));
            }
        } catch (Exception e) {
            log.warn("处理 MCP SSE 事件失败，eventType={}, data={}", eventType,
                    StrUtil.maxLength(data, 200), e);
        }
    }

    /**
     * 发送 JSON-RPC 请求并等待响应
     */
    public JSONObject sendRequest(String method, JSONObject params) throws Exception {
        if (!initialized) {
            initialize();
        }

        String requestId = String.valueOf(requestIdCounter.getAndIncrement());

        JSONObject requestBody = new JSONObject();
        requestBody.set("jsonrpc", "2.0");
        requestBody.set("id", requestId);
        requestBody.set("method", method);
        if (params != null) {
            requestBody.set("params", params);
        }

        CompletableFuture<JSONObject> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(endpointUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            requestBody.toString(), StandardCharsets.UTF_8));

            if (headers != null) {
                headers.forEach(requestBuilder::header);
            }

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 202) {
                pendingRequests.remove(requestId);
                throw new RuntimeException("MCP 请求失败，状态码：" + response.statusCode()
                        + "，响应：" + StrUtil.maxLength(response.body(), 500));
            }

            // 如果响应体非空且包含 jsonrpc，直接用响应体（某些实现直接返回结果）
            if (StrUtil.isNotBlank(response.body()) && response.body().contains("jsonrpc")) {
                JSONObject jsonResponse = JSONUtil.parseObj(response.body());
                if (requestId.equals(jsonResponse.getStr("id"))) {
                    pendingRequests.remove(requestId);
                    return jsonResponse;
                }
            }

            // 否则等待 SSE 响应
            return future.get(30, java.util.concurrent.TimeUnit.SECONDS);

        } catch (Exception e) {
            pendingRequests.remove(requestId);
            throw e;
        }
    }

    /**
     * 列出 MCP 服务器提供的工具
     */
    public JSONObject listTools() throws Exception {
        JSONObject result = sendRequest("tools/list", null);
        JSONObject error = result.getJSONObject("error");
        if (error != null) {
            throw new RuntimeException("MCP tools/list 失败：" + error.getStr("message"));
        }
        return result.getJSONObject("result");
    }

    /**
     * 调用 MCP 工具
     *
     * @param toolName  工具名称
     * @param arguments 参数（JSON 字符串）
     * @return 工具执行结果（content 数组的文本拼接）
     */
    public String executeTool(String toolName, String arguments) throws Exception {
        JSONObject params = new JSONObject();
        params.set("name", toolName);
        if (StrUtil.isNotBlank(arguments)) {
            try {
                params.set("arguments", JSONUtil.parseObj(arguments));
            } catch (Exception e) {
                // arguments 可能不是 JSON 对象，直接传字符串
                params.set("arguments", arguments);
            }
        }

        JSONObject result = sendRequest("tools/call", params);
        JSONObject error = result.getJSONObject("error");
        if (error != null) {
            throw new RuntimeException("MCP 工具调用失败：" + error.getStr("message"));
        }

        JSONObject resultData = result.getJSONObject("result");
        if (resultData == null) {
            return "";
        }

        // 解析 content 数组，拼接文本内容
        cn.hutool.json.JSONArray content = resultData.getJSONArray("content");
        if (content == null || content.isEmpty()) {
            return resultData.toString();
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < content.size(); i++) {
            JSONObject item = content.getJSONObject(i);
            String type = item.getStr("type", "text");
            if ("text".equals(type)) {
                sb.append(item.getStr("text", ""));
            } else {
                sb.append(item.toString());
            }
            if (i < content.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    @Override
    public void close() {
        closed = true;
        if (sseThread != null) {
            sseThread.interrupt();
        }
        pendingRequests.forEach((id, future) ->
                future.completeExceptionally(new RuntimeException("MCP 客户端已关闭")));
        pendingRequests.clear();
        try {
            httpClient.close();
        } catch (Exception ignored) {
        }
        log.info("MCP SSE 客户端已关闭，URL：{}", sseUrl);
    }
}
