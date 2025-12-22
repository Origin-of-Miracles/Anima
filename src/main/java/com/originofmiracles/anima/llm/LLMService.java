package com.originofmiracles.anima.llm;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.originofmiracles.anima.config.AnimaConfig;

/**
 * LLM 服务层
 * 
 * 使用 Java 11+ HttpClient 异步调用 OpenAI 兼容 API
 * 支持任何兼容 OpenAI API 格式的服务（OpenAI、Claude via Adapter、Ollama 等）
 */
public class LLMService {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    
    private final AnimaConfig config;
    private final HttpClient httpClient;
    private final ExecutorService executor;
    
    public LLMService(AnimaConfig config) {
        this.config = config;
        this.executor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "Anima-LLM-Worker");
            t.setDaemon(true);
            return t;
        });
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .executor(executor)
                .build();
        
        LOGGER.info("LLM 服务已初始化 - 端点: {}", config.getChatCompletionsUrl());
    }
    
    /**
     * 发送聊天请求（异步）
     * 
     * @param messages 消息列表
     * @return 包含响应文本的 CompletableFuture
     */
    public CompletableFuture<ChatResponse> chat(List<ChatMessage> messages) {
        return chat(messages, null, null);
    }
    
    /**
     * 发送聊天请求（异步，支持覆盖参数）
     * 
     * @param messages 消息列表
     * @param modelOverride 模型覆盖（可选）
     * @param temperatureOverride 温度覆盖（可选）
     * @return 包含响应的 CompletableFuture
     */
    public CompletableFuture<ChatResponse> chat(
            List<ChatMessage> messages,
            String modelOverride,
            Double temperatureOverride) {
        
        if (!config.isApiKeyConfigured()) {
            return CompletableFuture.completedFuture(
                ChatResponse.error("API Key 未配置，请在 config/anima/config.json 中设置"));
        }
        
        // 构建请求体
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", modelOverride != null ? modelOverride : config.getModel());
        requestBody.addProperty("max_tokens", config.getMaxTokens());
        requestBody.addProperty("temperature", temperatureOverride != null ? temperatureOverride : config.getTemperature());
        
        JsonArray messagesArray = new JsonArray();
        for (ChatMessage msg : messages) {
            JsonObject msgObj = new JsonObject();
            msgObj.addProperty("role", msg.getRole());
            msgObj.addProperty("content", msg.getContent());
            messagesArray.add(msgObj);
        }
        requestBody.add("messages", messagesArray);
        
        String requestJson = GSON.toJson(requestBody);
        
        LOGGER.debug("发送 LLM 请求: model={}, messages={}", 
                requestBody.get("model").getAsString(), 
                messages.size());
        
        // 构建 HTTP 请求
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getChatCompletionsUrl()))
                .timeout(Duration.ofSeconds(config.getRequestTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();
        
        // 异步发送请求
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::parseResponse)
                .exceptionally(e -> {
                    LOGGER.error("LLM 请求失败", e);
                    return ChatResponse.error("请求失败: " + e.getMessage());
                });
    }
    
    /**
     * 解析 API 响应
     */
    private ChatResponse parseResponse(HttpResponse<String> response) {
        try {
            if (response.statusCode() != 200) {
                LOGGER.warn("LLM API 返回错误状态码: {} - {}", 
                        response.statusCode(), response.body());
                return ChatResponse.error("API 错误 " + response.statusCode() + ": " + response.body());
            }
            
            JsonObject json = GSON.fromJson(response.body(), JsonObject.class);
            
            // 解析 OpenAI 格式响应
            if (json.has("choices") && json.getAsJsonArray("choices").size() > 0) {
                JsonObject choice = json.getAsJsonArray("choices").get(0).getAsJsonObject();
                JsonObject message = choice.getAsJsonObject("message");
                String content = message.get("content").getAsString();
                
                // 提取 usage 信息
                int promptTokens = 0;
                int completionTokens = 0;
                if (json.has("usage")) {
                    JsonObject usage = json.getAsJsonObject("usage");
                    promptTokens = usage.get("prompt_tokens").getAsInt();
                    completionTokens = usage.get("completion_tokens").getAsInt();
                }
                
                LOGGER.debug("LLM 响应: tokens={}/{}", promptTokens, completionTokens);
                
                return ChatResponse.success(content, promptTokens, completionTokens);
            }
            
            LOGGER.warn("无法解析 LLM 响应: {}", response.body());
            return ChatResponse.error("响应格式错误");
            
        } catch (Exception e) {
            LOGGER.error("解析 LLM 响应失败", e);
            return ChatResponse.error("解析响应失败: " + e.getMessage());
        }
    }
    
    /**
     * 快捷方法：构建包含系统提示词的消息列表
     * 
     * @param systemPrompt 系统提示词
     * @param userMessage 用户消息
     * @return 消息列表
     */
    public static List<ChatMessage> buildMessages(String systemPrompt, String userMessage) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(systemPrompt));
        messages.add(ChatMessage.user(userMessage));
        return messages;
    }
    
    /**
     * 快捷方法：构建包含历史记录的消息列表
     * 
     * @param systemPrompt 系统提示词
     * @param history 历史消息
     * @param userMessage 当前用户消息
     * @return 消息列表
     */
    public static List<ChatMessage> buildMessagesWithHistory(
            String systemPrompt,
            List<ChatMessage> history,
            String userMessage) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(systemPrompt));
        messages.addAll(history);
        messages.add(ChatMessage.user(userMessage));
        return messages;
    }
    
    /**
     * 关闭服务，释放资源
     */
    public void shutdown() {
        executor.shutdown();
        LOGGER.info("LLM 服务已关闭");
    }
    
    /**
     * 聊天消息
     */
    public static class ChatMessage {
        private final String role;
        private final String content;
        
        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
        
        public static ChatMessage system(String content) {
            return new ChatMessage("system", content);
        }
        
        public static ChatMessage user(String content) {
            return new ChatMessage("user", content);
        }
        
        public static ChatMessage assistant(String content) {
            return new ChatMessage("assistant", content);
        }
        
        public String getRole() {
            return role;
        }
        
        public String getContent() {
            return content;
        }
    }
    
    /**
     * 聊天响应
     */
    public static class ChatResponse {
        private final boolean success;
        private final String content;
        private final String error;
        private final int promptTokens;
        private final int completionTokens;
        
        private ChatResponse(boolean success, String content, String error, 
                           int promptTokens, int completionTokens) {
            this.success = success;
            this.content = content;
            this.error = error;
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
        }
        
        public static ChatResponse success(String content, int promptTokens, int completionTokens) {
            return new ChatResponse(true, content, null, promptTokens, completionTokens);
        }
        
        public static ChatResponse error(String error) {
            return new ChatResponse(false, null, error, 0, 0);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getContent() {
            return content;
        }
        
        public String getError() {
            return error;
        }
        
        public int getPromptTokens() {
            return promptTokens;
        }
        
        public int getCompletionTokens() {
            return completionTokens;
        }
        
        public int getTotalTokens() {
            return promptTokens + completionTokens;
        }
    }
}
