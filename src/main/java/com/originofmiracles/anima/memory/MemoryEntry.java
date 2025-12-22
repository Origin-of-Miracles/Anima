package com.originofmiracles.anima.memory;

import java.time.Instant;
import java.util.UUID;

/**
 * 记忆条目
 * 
 * 表示一条存储在记忆系统中的信息
 */
public class MemoryEntry {
    
    /**
     * 记忆类型
     */
    public enum MemoryType {
        /** 对话消息 */
        DIALOGUE,
        /** 事件（如玩家送礼物、完成任务） */
        EVENT,
        /** 观察到的环境信息 */
        OBSERVATION,
        /** 情感记忆（重要的情感时刻） */
        EMOTION
    }
    
    private final String id;
    private final MemoryType type;
    private final String content;
    private final Instant timestamp;
    private final String source;  // 来源（如 "player", "self", "environment"）
    
    // 情感相关
    private float emotionalValence;  // 情感效价：-1.0（负面）到 1.0（正面）
    private float importance;        // 重要性：0.0 到 1.0
    
    // 上下文信息
    private String location;         // 发生地点
    private String[] participants;   // 参与者
    
    /**
     * 创建记忆条目
     */
    public MemoryEntry(MemoryType type, String content, String source) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.content = content;
        this.source = source;
        this.timestamp = Instant.now();
        this.emotionalValence = 0f;
        this.importance = 0.5f;
    }
    
    /**
     * 创建对话记忆
     */
    public static MemoryEntry dialogue(String content, String source) {
        return new MemoryEntry(MemoryType.DIALOGUE, content, source);
    }
    
    /**
     * 创建事件记忆
     */
    public static MemoryEntry event(String content) {
        return new MemoryEntry(MemoryType.EVENT, content, "environment");
    }
    
    /**
     * 创建观察记忆
     */
    public static MemoryEntry observation(String content) {
        return new MemoryEntry(MemoryType.OBSERVATION, content, "self");
    }
    
    // ==================== Getters & Setters ====================
    
    public String getId() {
        return id;
    }
    
    public MemoryType getType() {
        return type;
    }
    
    public String getContent() {
        return content;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public String getSource() {
        return source;
    }
    
    public float getEmotionalValence() {
        return emotionalValence;
    }
    
    public MemoryEntry withEmotionalValence(float valence) {
        this.emotionalValence = Math.max(-1f, Math.min(1f, valence));
        return this;
    }
    
    public float getImportance() {
        return importance;
    }
    
    public MemoryEntry withImportance(float importance) {
        this.importance = Math.max(0f, Math.min(1f, importance));
        return this;
    }
    
    public String getLocation() {
        return location;
    }
    
    public MemoryEntry withLocation(String location) {
        this.location = location;
        return this;
    }
    
    public String[] getParticipants() {
        return participants;
    }
    
    public MemoryEntry withParticipants(String... participants) {
        this.participants = participants;
        return this;
    }
    
    /**
     * 计算记忆的相关性分数
     * 考虑时间衰减和重要性
     * 
     * @param hoursDecayFactor 每小时衰减因子（默认约 0.95）
     * @return 相关性分数 0.0 - 1.0
     */
    public float calculateRelevance(float hoursDecayFactor) {
        long hoursSinceCreation = (Instant.now().getEpochSecond() - timestamp.getEpochSecond()) / 3600;
        float timeDecay = (float) Math.pow(hoursDecayFactor, hoursSinceCreation);
        return importance * timeDecay;
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s: %s", type, source, content);
    }
}
