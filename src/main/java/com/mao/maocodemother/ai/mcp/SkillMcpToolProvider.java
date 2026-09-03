package com.mao.maocodemother.ai.mcp;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.mao.maocodemother.model.entity.Skill;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 技能专属 MCP 工具提供者
 * 从技能配置的 MCP 服务器中加载工具列表，提供给 AI 模型调用
 * <p>
 * 实现 ToolProvider 接口，可直接注入到 AiServices 中
 */
@Slf4j
public class SkillMcpToolProvider implements ToolProvider, AutoCloseable {

    private final Skill skill;
    private final Map<String, SseMcpClient> mcpClients = new HashMap<>();
    private final Map<String, String> toolToServerMap = new HashMap<>(); // toolName -> serverName
    private Map<ToolSpecification, ToolExecutor> toolsCache;

    public SkillMcpToolProvider(Skill skill) {
        this.skill = skill;
    }

    /**
     * 初始化：连接所有 MCP 服务器，加载工具列表
     */
    public synchronized void initialize() throws Exception {
        if (toolsCache != null) {
            return;
        }

        String mcpServersJson = skill.getMcpServers();
        if (StrUtil.isBlank(mcpServersJson)) {
            toolsCache = Map.of();
            return;
        }

        JSONArray serverConfigs = JSONUtil.parseArray(mcpServersJson);
        Map<ToolSpecification, ToolExecutor> toolMap = new HashMap<>();

        for (int i = 0; i < serverConfigs.size(); i++) {
            JSONObject config = serverConfigs.getJSONObject(i);
            String name = config.getStr("name", "mcp-server-" + i);
            String type = config.getStr("type", "sse");
            String url = config.getStr("url");
            JSONObject headersObj = config.getJSONObject("headers");
            Map<String, String> headers = new HashMap<>();
            if (headersObj != null) {
                headersObj.forEach((k, v) -> headers.put(k, String.valueOf(v)));
            }

            if (StrUtil.isBlank(url) || !"sse".equalsIgnoreCase(type)) {
                log.warn("跳过不支持的 MCP 服务器配置：name={}, type={}, url={}", name, type, url);
                continue;
            }

            try {
                SseMcpClient client = new SseMcpClient(url, headers);
                client.initialize();

                // 获取工具列表
                JSONObject toolsResult = client.listTools();
                JSONArray tools = toolsResult.getJSONArray("tools");
                if (tools != null) {
                    for (int j = 0; j < tools.size(); j++) {
                        JSONObject tool = tools.getJSONObject(j);
                        String toolName = tool.getStr("name");
                        if (StrUtil.isBlank(toolName)) {
                            continue;
                        }

                        // 服务器名前缀，避免不同服务器同名工具冲突
                        String prefixedName = name + "__" + toolName;

                        ToolSpecification toolSpec = convertToToolSpecification(prefixedName, tool);
                        String serverName = name;
                        String originalToolName = toolName;

                        ToolExecutor executor = (request, memoryId) -> {
                            try {
                                SseMcpClient c = mcpClients.get(serverName);
                                if (c == null) {
                                    return "Error: MCP 服务器 " + serverName + " 未连接";
                                }
                                return c.executeTool(originalToolName, request.arguments());
                            } catch (Exception e) {
                                log.error("MCP 工具执行失败，server={}, tool={}", serverName, originalToolName, e);
                                return "Error: " + e.getMessage();
                            }
                        };

                        toolMap.put(toolSpec, executor);
                        toolToServerMap.put(prefixedName, name);
                        log.debug("注册 MCP 工具：{} (来自服务器 {})", prefixedName, name);
                    }
                }

                mcpClients.put(name, client);
                log.info("MCP 服务器 {} 加载成功，工具数量：{}", name,
                        tools != null ? tools.size() : 0);

            } catch (Exception e) {
                log.error("连接 MCP 服务器失败：{}", name, e);
            }
        }

        toolsCache = toolMap;
        log.info("技能「{}」的 MCP 工具提供者初始化完成，共 {} 个工具",
                skill.getSkillName(), toolsCache.size());
    }

    /**
     * 将 MCP 工具定义转换为 LangChain4j 的 ToolSpecification
     */
    private ToolSpecification convertToToolSpecification(String toolName, JSONObject mcpTool) {
        String description = mcpTool.getStr("description", "");
        JSONObject inputSchema = mcpTool.getJSONObject("inputSchema");

        ToolSpecification.Builder builder = ToolSpecification.builder()
                .name(toolName)
                .description(StrUtil.isBlank(description) ? "MCP 工具" : description);

        if (inputSchema != null) {
            JsonObjectSchema params = convertJsonSchema(inputSchema);
            builder.parameters(params);
        }

        return builder.build();
    }

    /**
     * 将 MCP 的 JSON Schema 转换为 LangChain4j 的 JsonObjectSchema
     */
    private JsonObjectSchema convertJsonSchema(JSONObject schema) {
        JsonObjectSchema.Builder builder = JsonObjectSchema.builder();

        String desc = schema.getStr("description");
        if (StrUtil.isNotBlank(desc)) {
            builder.description(desc);
        }

        JSONObject properties = schema.getJSONObject("properties");
        if (properties != null) {
            for (String propName : properties.keySet()) {
                JSONObject propSchema = properties.getJSONObject(propName);
                JsonSchemaElement element = convertSchemaElement(propSchema);
                if (element != null) {
                    builder.addProperty(propName, element);
                }
            }
        }

        JSONArray required = schema.getJSONArray("required");
        if (required != null && !required.isEmpty()) {
            List<String> requiredList = required.toList(String.class);
            if (CollUtil.isNotEmpty(requiredList)) {
                builder.required(requiredList);
            }
        }

        return builder.build();
    }

    /**
     * 递归转换 JSON Schema 元素
     */
    private JsonSchemaElement convertSchemaElement(JSONObject schema) {
        if (schema == null) {
            return null;
        }
        String type = schema.getStr("type", "string");

        // 处理 type 为数组的情况
        if (type.contains("[")) {
            try {
                JSONArray types = JSONUtil.parseArray(type);
                if (!types.isEmpty()) {
                    type = types.getStr(0, "string");
                }
            } catch (Exception ignored) {
            }
        }

        String description = schema.getStr("description");
        JSONArray enumValues = schema.getJSONArray("enum");

        return switch (type) {
            case "string" -> {
                if (enumValues != null && !enumValues.isEmpty()) {
                    List<String> enumList = enumValues.toList(String.class);
                    yield dev.langchain4j.model.chat.request.json.JsonEnumSchema.builder()
                            .description(description)
                            .enumValues(enumList)
                            .build();
                }
                yield dev.langchain4j.model.chat.request.json.JsonStringSchema.builder()
                        .description(description)
                        .build();
            }
            case "integer" -> dev.langchain4j.model.chat.request.json.JsonIntegerSchema.builder()
                    .description(description)
                    .build();
            case "number" -> dev.langchain4j.model.chat.request.json.JsonNumberSchema.builder()
                    .description(description)
                    .build();
            case "boolean" -> dev.langchain4j.model.chat.request.json.JsonBooleanSchema.builder()
                    .description(description)
                    .build();
            case "object" -> convertJsonSchema(schema);
            case "array" -> {
                JSONObject items = schema.getJSONObject("items");
                JsonSchemaElement itemSchema = items != null ? convertSchemaElement(items) : null;
                yield dev.langchain4j.model.chat.request.json.JsonArraySchema.builder()
                        .description(description)
                        .items(itemSchema)
                        .build();
            }
            default -> dev.langchain4j.model.chat.request.json.JsonStringSchema.builder()
                    .description(description)
                    .build();
        };
    }

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        try {
            initialize();
        } catch (Exception e) {
            log.error("MCP 工具提供者初始化失败", e);
            return ToolProviderResult.builder().build();
        }
        return ToolProviderResult.builder()
                .addAll(toolsCache)
                .build();
    }

    @Override
    public void close() {
        mcpClients.forEach((name, client) -> {
            try {
                client.close();
            } catch (Exception e) {
                log.warn("关闭 MCP 客户端失败：{}", name, e);
            }
        });
        mcpClients.clear();
        toolToServerMap.clear();
        toolsCache = null;
    }
}
