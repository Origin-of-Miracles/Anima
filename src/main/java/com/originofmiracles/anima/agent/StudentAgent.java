package com.originofmiracles.anima.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.originofmiracles.anima.llm.LLMService;
import com.originofmiracles.anima.llm.LLMService.ChatMessage;
import com.originofmiracles.anima.llm.LLMService.ChatResponse;
import com.originofmiracles.anima.persona.Persona;

/**
 * 学生代理
 * 
 * 代表一个具有特定人格的 AI 角色
 * 管理对话历史、调用 LLM 生成回复
 */
public class StudentAgent {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * 最大历史消息数量（user + assistant 各算一条）
     */
    private static final int MAX_HISTORY_SIZE = 20;
    
    private final String id;
    private final Persona persona;
    private final LLMService llmService;
    
    /**
     * 对话历史（不包含系统提示词）
     */
    private final List<ChatMessage> history = new ArrayList<>();
    
    public StudentAgent(String id, Persona persona, LLMService llmService) {
        this.id = id;
        this.persona = persona;
        this.llmService = llmService;
        
        LOGGER.debug("创建学生代理: {} ({})", persona.getName(), id);
    }
    
    /**
     * 处理用户消息并生成回复
     * 
     * @param userMessage 用户消息
     * @return 包含 AI 回复的 CompletableFuture
     */
    public CompletableFuture<ChatResponse> chat(String userMessage) {
        LOGGER.debug("[{}] 收到消息: {}", id, userMessage);
        
        // 构建消息列表
        List<ChatMessage> messages = LLMService.buildMessagesWithHistory(
                persona.buildFullSystemPrompt(),
                history,
                userMessage
        );
        
        // 添加示例对话（few-shot）
        if (persona.getExampleDialogues() != null && history.isEmpty()) {
            // 只在首次对话时添加示例
            List<ChatMessage> messagesWithExamples = new ArrayList<>();
            messagesWithExamples.add(ChatMessage.system(persona.buildFullSystemPrompt()));
            
            for (Persona.ExampleDialogue example : persona.getExampleDialogues()) {
                messagesWithExamples.add(ChatMessage.user(example.getUser()));
                messagesWithExamples.add(ChatMessage.assistant(example.getAssistant()));
            }
            
            messagesWithExamples.addAll(history);
            messagesWithExamples.add(ChatMessage.user(userMessage));
            messages = messagesWithExamples;
        }
        
        // 调用 LLM
        return llmService.chat(
                messages,
                persona.getModelOverride(),
                persona.getTemperatureOverride()
        ).thenApply(response -> {
            if (response.isSuccess()) {
                // 更新历史记录
                addToHistory(ChatMessage.user(userMessage));
                addToHistory(ChatMessage.assistant(response.getContent()));
                
                LOGGER.debug("[{}] 生成回复: {}", id, 
                        response.getContent().substring(0, Math.min(50, response.getContent().length())));
            } else {
                LOGGER.warn("[{}] LLM 调用失败: {}", id, response.getError());
            }
            return response;
        });
    }
    
    /**
     * 添加消息到历史记录
     * 自动裁剪过长的历史
     */
    private void addToHistory(ChatMessage message) {
        history.add(message);
        
        // 裁剪历史记录
        while (history.size() > MAX_HISTORY_SIZE) {
            history.remove(0);
        }
    }
    
    /**
     * 清空对话历史
     */
    public void clearHistory() {
        history.clear();
        LOGGER.debug("[{}] 已清空对话历史", id);
    }
    
    /**
     * 获取代理 ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * 获取人格配置
     */
    public Persona getPersona() {
        return persona;
    }
    
    /**
     * 获取当前历史记录数量
     */
    public int getHistorySize() {
        return history.size();
    }
}
