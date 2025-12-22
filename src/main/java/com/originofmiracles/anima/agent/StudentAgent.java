package com.originofmiracles.anima.agent;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.originofmiracles.anima.llm.LLMService;
import com.originofmiracles.anima.llm.LLMService.ChatMessage;
import com.originofmiracles.anima.llm.LLMService.ChatResponse;
import com.originofmiracles.anima.memory.MemoryBank;
import com.originofmiracles.anima.mood.MoodAnimationMapper;
import com.originofmiracles.anima.mood.MoodState;
import com.originofmiracles.anima.mood.MoodSystem;
import com.originofmiracles.anima.mood.MoodTrigger;
import com.originofmiracles.anima.persona.Persona;

/**
 * 学生代理
 * 
 * 代表一个具有特定人格的 AI 角色
 * 
 * 功能：
 * - 管理对话历史与记忆
 * - 情绪状态追踪
 * - 调用 LLM 生成回复
 * - 生成动态系统提示词
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
    
    // 记忆系统
    private final MemoryBank memoryBank;
    
    // 情绪系统
    private final MoodSystem moodSystem;
    
    // 环境感知上下文（来自 PerceptionAPI）
    @Nullable
    private JsonObject perceptionContext;
    
    /**
     * 对话历史（不包含系统提示词）- 兼容旧代码
     */
    private final List<ChatMessage> history = new ArrayList<>();
    
    /**
     * 创建学生代理（无记忆持久化）
     */
    public StudentAgent(String id, Persona persona, LLMService llmService) {
        this(id, persona, llmService, null);
    }
    
    /**
     * 创建学生代理（支持记忆持久化）
     * 
     * @param id 学生 ID
     * @param persona 人格配置
     * @param llmService LLM 服务
     * @param memoryStorageDir 记忆存储目录（可选）
     */
    public StudentAgent(String id, Persona persona, LLMService llmService, @Nullable Path memoryStorageDir) {
        this.id = id;
        this.persona = persona;
        this.llmService = llmService;
        
        // 初始化情绪系统
        this.moodSystem = new MoodSystem(id);
        
        // 初始化记忆系统
        if (memoryStorageDir != null) {
            this.memoryBank = new MemoryBank(id, memoryStorageDir);
        } else {
            // 使用临时目录
            this.memoryBank = new MemoryBank(id, Path.of(System.getProperty("java.io.tmpdir"), "anima"));
        }
        
        // 情绪变化监听
        moodSystem.addListener(event -> {
            memoryBank.recordEmotion(
                    "情绪从" + event.getPreviousState().getDisplayName() + 
                    "变为" + event.getNewState().getDisplayName(),
                    event.getNewState().getBaseValence()
            );
        });
        
        LOGGER.debug("创建学生代理: {} ({})", persona.getName(), id);
    }
    
    /**
     * 设置环境感知上下文（来自 PerceptionAPI）
     * 
     * @param context 感知数据
     */
    public void setPerceptionContext(@Nullable JsonObject context) {
        this.perceptionContext = context;
    }
    
    /**
     * 处理用户消息并生成回复
     * 
     * @param userMessage 用户消息
     * @return 包含 AI 回复的 CompletableFuture
     */
    public CompletableFuture<ChatResponse> chat(String userMessage) {
        return chat(userMessage, null);
    }
    
    /**
     * 处理用户消息并生成回复（带环境感知）
     * 
     * @param userMessage 用户消息
     * @param perception 环境感知上下文（可选）
     * @return 包含 AI 回复的 CompletableFuture
     */
    public CompletableFuture<ChatResponse> chat(String userMessage, @Nullable JsonObject perception) {
        LOGGER.debug("[{}] 收到消息: {}", id, userMessage);
        
        // 更新感知上下文
        if (perception != null) {
            this.perceptionContext = perception;
        }
        
        // 对话开始触发情绪
        if (history.isEmpty()) {
            moodSystem.applyTrigger(MoodTrigger.CONVERSATION_STARTED);
        }
        
        // 记录用户消息到记忆
        memoryBank.recordUserMessage(userMessage);
        
        // 构建增强版系统提示词
        String enhancedSystemPrompt = buildEnhancedSystemPrompt();
        
        // 构建消息列表
        List<ChatMessage> messages = LLMService.buildMessagesWithHistory(
                enhancedSystemPrompt,
                history,
                userMessage
        );
        
        // 添加示例对话（few-shot）
        if (persona.getExampleDialogues() != null && history.isEmpty()) {
            // 只在首次对话时添加示例
            List<ChatMessage> messagesWithExamples = new ArrayList<>();
            messagesWithExamples.add(ChatMessage.system(enhancedSystemPrompt));
            
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
                
                // 记录助手回复到记忆
                memoryBank.recordAssistantMessage(response.getContent());
                
                // 根据回复内容分析情绪（简单启发式）
                analyzeResponseMood(response.getContent());
                
                LOGGER.debug("[{}] 生成回复: {}", id, 
                        response.getContent().substring(0, Math.min(50, response.getContent().length())));
            } else {
                LOGGER.warn("[{}] LLM 调用失败: {}", id, response.getError());
            }
            return response;
        });
    }
    
    /**
     * 构建增强版系统提示词
     * 包含基础人格 + 当前情绪 + 记忆上下文 + 环境感知
     */
    private String buildEnhancedSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        
        // 基础人格提示词
        sb.append(persona.buildFullSystemPrompt());
        
        // 当前情绪状态
        sb.append("\n\n【当前状态】\n");
        sb.append("- 情绪: ").append(moodSystem.getMoodDescription()).append("\n");
        
        // 环境感知
        if (perceptionContext != null) {
            sb.append("\n【环境感知】\n");
            
            if (perceptionContext.has("time")) {
                JsonObject time = perceptionContext.getAsJsonObject("time");
                sb.append("- 时间: ").append(time.get("timeOfDay").getAsString()).append("\n");
            }
            
            if (perceptionContext.has("player")) {
                JsonObject player = perceptionContext.getAsJsonObject("player");
                sb.append("- 玩家位置: ").append(player.get("position").getAsString()).append("\n");
                if (player.has("heldItem")) {
                    sb.append("- 玩家手持: ").append(player.get("heldItem").getAsString()).append("\n");
                }
            }
            
            if (perceptionContext.has("nearbyEntities")) {
                sb.append("- 附近实体: ").append(perceptionContext.getAsJsonArray("nearbyEntities").size()).append("个\n");
            }
        }
        
        // 记忆上下文
        String memoryContext = memoryBank.buildMemoryContext();
        if (!memoryContext.isEmpty()) {
            sb.append("\n").append(memoryContext);
        }
        
        return sb.toString();
    }
    
    /**
     * 分析回复内容，推断情绪变化
     */
    private void analyzeResponseMood(String response) {
        String lower = response.toLowerCase();
        
        // 简单的关键词分析
        if (lower.contains("开心") || lower.contains("高兴") || lower.contains("太好了") ||
            lower.contains("≧▽≦") || lower.contains("嘿嘿")) {
            moodSystem.applyTrigger(MoodTrigger.RECEIVED_COMPLIMENT, 0.5f);
        } else if (lower.contains("难过") || lower.contains("伤心") || lower.contains("呜呜") ||
                   lower.contains("•́︿•̀")) {
            moodSystem.setState(MoodState.SAD, 0.5f);
        } else if (lower.contains("生气") || lower.contains("哼") || lower.contains("讨厌")) {
            moodSystem.setState(MoodState.ANGRY, 0.4f);
        } else if (lower.contains("？") || lower.contains("什么") || lower.contains("为什么")) {
            moodSystem.setState(MoodState.CONFUSED, 0.3f);
        }
    }
    
    /**
     * 应用情绪触发器
     * 
     * @param trigger 触发器
     */
    public void applyMoodTrigger(MoodTrigger trigger) {
        moodSystem.applyTrigger(trigger);
    }
    
    /**
     * 获取当前情绪状态
     */
    public MoodState getCurrentMood() {
        return moodSystem.getCurrentState();
    }
    
    /**
     * 获取当前情绪强度
     */
    public float getMoodIntensity() {
        return moodSystem.getIntensity();
    }
    
    /**
     * 获取情绪对应的动画 ID
     */
    public String getMoodAnimationId() {
        return MoodAnimationMapper.getAnimationByIntensity(
                moodSystem.getCurrentState(), 
                moodSystem.getIntensity()
        );
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
        memoryBank.clearImmediateMemory();
        LOGGER.debug("[{}] 已清空对话历史", id);
    }
    
    /**
     * 结束对话会话
     * 保存重要记忆到短期记忆
     */
    public void endSession() {
        moodSystem.applyTrigger(MoodTrigger.CONVERSATION_ENDED);
        memoryBank.endSession();
        LOGGER.info("[{}] 对话会话已结束", id);
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
    
    /**
     * 获取记忆银行
     */
    public MemoryBank getMemoryBank() {
        return memoryBank;
    }
    
    /**
     * 获取情绪系统
     */
    public MoodSystem getMoodSystem() {
        return moodSystem;
    }
}