package com.originofmiracles.anima.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;

/**
 * Anima 主配置类
 * 
 * 配置文件位置: config/anima/config.json
 * 
 * 包含 LLM 配置：
 * - baseUrl: API 基础地址（只到 v1，如 https://api.openai.com/v1）
 * - model: 模型名称（如 gpt-4o-mini）
 * - apiKey: API 密钥
 */
public class AnimaConfig {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static AnimaConfig instance;
    private static Path configPath;
    
    // LLM 配置
    private String baseUrl = "https://api.openai.com/v1";
    private String model = "gpt-4o-mini";
    private String apiKey = "";
    
    // 请求配置
    private int maxTokens = 1024;
    private double temperature = 0.7;
    private int requestTimeoutSeconds = 30;
    
    // 私有构造函数
    private AnimaConfig() {}
    
    /**
     * 初始化配置系统
     * 
     * @param configDir Forge 配置目录 (通常是 config/)
     */
    public static void init(Path configDir) {
        Path animaDir = configDir.resolve("anima");
        configPath = animaDir.resolve("config.json");
        
        try {
            Files.createDirectories(animaDir);
            
            if (Files.exists(configPath)) {
                // 加载现有配置
                String content = Files.readString(configPath);
                instance = GSON.fromJson(content, AnimaConfig.class);
                LOGGER.info("已加载 Anima 配置: {}", configPath);
            } else {
                // 创建默认配置
                instance = new AnimaConfig();
                saveConfig();
                LOGGER.info("已创建默认 Anima 配置: {}", configPath);
            }
            
        } catch (IOException e) {
            LOGGER.error("加载 Anima 配置失败，使用默认值", e);
            instance = new AnimaConfig();
        }
    }
    
    /**
     * 保存配置到文件
     */
    public static void saveConfig() {
        if (instance == null || configPath == null) {
            LOGGER.warn("配置未初始化，无法保存");
            return;
        }
        
        try {
            String json = GSON.toJson(instance);
            Files.writeString(configPath, json);
            LOGGER.info("已保存 Anima 配置");
        } catch (IOException e) {
            LOGGER.error("保存 Anima 配置失败", e);
        }
    }
    
    /**
     * 获取配置实例
     */
    public static AnimaConfig getInstance() {
        if (instance == null) {
            LOGGER.warn("配置未初始化，返回默认实例");
            instance = new AnimaConfig();
        }
        return instance;
    }
    
    // ==================== Getters ====================
    
    /**
     * 获取 LLM API 基础地址
     * 只到 v1 级别，如 https://api.openai.com/v1
     */
    public String getBaseUrl() {
        return baseUrl;
    }
    
    /**
     * 获取完整的聊天补全端点 URL
     */
    public String getChatCompletionsUrl() {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return base + "/chat/completions";
    }
    
    /**
     * 获取模型名称
     */
    public String getModel() {
        return model;
    }
    
    /**
     * 获取 API 密钥
     */
    public String getApiKey() {
        return apiKey;
    }
    
    /**
     * 检查 API 密钥是否已配置
     */
    public boolean isApiKeyConfigured() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("your-api-key-here");
    }
    
    /**
     * 获取最大 Token 数
     */
    public int getMaxTokens() {
        return maxTokens;
    }
    
    /**
     * 获取温度参数
     */
    public double getTemperature() {
        return temperature;
    }
    
    /**
     * 获取请求超时时间（秒）
     */
    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }
    
    // ==================== Setters ====================
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }
    
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
    
    public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }
}
