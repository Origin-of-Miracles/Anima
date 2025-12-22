package com.originofmiracles.anima.memory;

import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.originofmiracles.anima.llm.LLMService.ChatMessage;

/**
 * 记忆银行
 * 
 * 整合即时记忆和短期记忆，提供统一的记忆管理接口
 * 
 * 记忆层级：
 * 1. 即时记忆 (ImmediateMemory) - 当前对话上下文
 * 2. 短期记忆 (ShortTermMemory) - 近期重要事件
 * 3. 长期记忆 (未实现) - 向量检索的永久记忆
 */
public class MemoryBank {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /** 提取到短期记忆的重要性阈值 */
    private static final float EXTRACTION_THRESHOLD = 0.7f;
    
    private final String studentId;
    private final ImmediateMemory immediateMemory;
    private final ShortTermMemory shortTermMemory;
    
    /**
     * 创建记忆银行
     * 
     * @param studentId 学生 ID
     * @param storageDir 存储目录（用于短期记忆持久化）
     */
    public MemoryBank(String studentId, Path storageDir) {
        this(studentId, storageDir, ImmediateMemory.DEFAULT_CAPACITY);
    }
    
    /**
     * 创建记忆银行
     * 
     * @param studentId 学生 ID
     * @param storageDir 存储目录
     * @param immediateCapacity 即时记忆容量
     */
    public MemoryBank(String studentId, Path storageDir, int immediateCapacity) {
        this.studentId = studentId;
        this.immediateMemory = new ImmediateMemory(immediateCapacity);
        this.shortTermMemory = new ShortTermMemory(studentId, storageDir);
        
        LOGGER.debug("[{}] 记忆银行已创建", studentId);
    }
    
    // ==================== 对话记忆操作 ====================
    
    /**
     * 记录用户消息
     */
    public void recordUserMessage(String content) {
        immediateMemory.addUserMessage(content);
    }
    
    /**
     * 记录助手回复
     */
    public void recordAssistantMessage(String content) {
        immediateMemory.addAssistantMessage(content);
    }
    
    /**
     * 获取对话历史（用于 LLM 调用）
     */
    public List<ChatMessage> getConversationHistory() {
        return immediateMemory.getMessages();
    }
    
    /**
     * 获取最近的对话历史
     * 
     * @param count 消息数量
     */
    public List<ChatMessage> getRecentConversation(int count) {
        return immediateMemory.getRecentMessages(count);
    }
    
    // ==================== 事件记忆操作 ====================
    
    /**
     * 记录重要事件
     * 
     * @param description 事件描述
     * @param importance 重要性 (0.0 - 1.0)
     */
    public void recordEvent(String description, float importance) {
        MemoryEntry entry = MemoryEntry.event(description)
                .withImportance(importance);
        
        immediateMemory.addEntry(entry);
        
        // 高重要性事件直接进入短期记忆
        if (importance >= EXTRACTION_THRESHOLD) {
            shortTermMemory.addEntry(entry);
        }
    }
    
    /**
     * 记录观察
     * 
     * @param observation 观察内容
     */
    public void recordObservation(String observation) {
        MemoryEntry entry = MemoryEntry.observation(observation)
                .withImportance(0.3f);
        
        immediateMemory.addEntry(entry);
    }
    
    /**
     * 记录情感变化
     * 
     * @param emotion 情感描述
     * @param valence 效价 (-1.0 到 1.0)
     */
    public void recordEmotion(String emotion, float valence) {
        MemoryEntry entry = new MemoryEntry(
                MemoryEntry.MemoryType.EMOTION, 
                emotion, 
                "self"
        ).withEmotionalValence(valence)
         .withImportance(0.6f);
        
        immediateMemory.addEntry(entry);
    }
    
    // ==================== 记忆检索 ====================
    
    /**
     * 构建完整的记忆上下文（用于 prompt）
     * 
     * @return 格式化的记忆上下文字符串
     */
    public String buildMemoryContext() {
        StringBuilder sb = new StringBuilder();
        
        // 短期记忆摘要
        String shortTermSummary = shortTermMemory.buildSummary(10);
        if (!shortTermSummary.equals("（没有近期记忆）")) {
            sb.append("【近期记忆】\n");
            sb.append(shortTermSummary);
            sb.append("\n");
        }
        
        // 即时记忆上下文
        String immediateContext = immediateMemory.buildContextSummary();
        if (!immediateContext.equals("（暂无对话历史）")) {
            sb.append("【当前对话】\n");
            sb.append(immediateContext);
        }
        
        return sb.toString();
    }
    
    /**
     * 搜索相关记忆
     * 
     * @param keyword 关键词
     * @param limit 最大返回数量
     * @return 记忆条目列表
     */
    public List<MemoryEntry> searchMemories(String keyword, int limit) {
        return shortTermMemory.searchByKeyword(keyword, limit);
    }
    
    // ==================== 生命周期管理 ====================
    
    /**
     * 结束当前对话会话
     * 将重要记忆提取到短期记忆，然后清空即时记忆
     */
    public void endSession() {
        // 提取重要记忆
        shortTermMemory.extractFromImmediate(immediateMemory, EXTRACTION_THRESHOLD);
        
        // 保存短期记忆
        shortTermMemory.save();
        
        // 清空即时记忆
        immediateMemory.clear();
        
        LOGGER.info("[{}] 对话会话已结束，记忆已保存", studentId);
    }
    
    /**
     * 清空即时记忆（不影响短期记忆）
     */
    public void clearImmediateMemory() {
        immediateMemory.clear();
    }
    
    /**
     * 清空所有记忆
     */
    public void clearAll() {
        immediateMemory.clear();
        // 注意：短期记忆的清空需要删除文件，这里只清空内存中的数据
        LOGGER.warn("[{}] 所有记忆已清空", studentId);
    }
    
    /**
     * 执行日常维护
     * 应该在游戏日期变化或定时触发
     */
    public void performDailyMaintenance() {
        // 提取重要记忆到短期记忆
        shortTermMemory.extractFromImmediate(immediateMemory, EXTRACTION_THRESHOLD);
        
        // 清理过期记忆
        shortTermMemory.cleanup();
        
        // 保存
        shortTermMemory.save();
        
        LOGGER.debug("[{}] 记忆日常维护完成", studentId);
    }
    
    // ==================== 状态查询 ====================
    
    /**
     * 获取学生 ID
     */
    public String getStudentId() {
        return studentId;
    }
    
    /**
     * 获取即时记忆消息数量
     */
    public int getImmediateMessageCount() {
        return immediateMemory.getMessageCount();
    }
    
    /**
     * 获取短期记忆条目总数
     */
    public int getShortTermEntryCount() {
        return shortTermMemory.getTotalEntryCount();
    }
    
    /**
     * 获取即时记忆实例（高级用法）
     */
    public ImmediateMemory getImmediateMemory() {
        return immediateMemory;
    }
    
    /**
     * 获取短期记忆实例（高级用法）
     */
    public ShortTermMemory getShortTermMemory() {
        return shortTermMemory;
    }
}
