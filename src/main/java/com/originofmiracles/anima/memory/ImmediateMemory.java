package com.originofmiracles.anima.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.originofmiracles.anima.llm.LLMService.ChatMessage;

/**
 * 即时记忆
 * 
 * 存储当前对话上下文，保留最近 N 条消息
 * 这是最短期的记忆，用于维持对话的连贯性
 * 
 * 线程安全：所有操作都是同步的
 */
public class ImmediateMemory {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /** 默认最大记忆容量 */
    public static final int DEFAULT_CAPACITY = 20;
    
    private final int capacity;
    private final List<ChatMessage> messages;
    private final List<MemoryEntry> entries;
    
    public ImmediateMemory() {
        this(DEFAULT_CAPACITY);
    }
    
    public ImmediateMemory(int capacity) {
        this.capacity = capacity;
        this.messages = new ArrayList<>();
        this.entries = new ArrayList<>();
    }
    
    /**
     * 添加对话消息
     * 
     * @param message 聊天消息
     */
    public synchronized void addMessage(ChatMessage message) {
        messages.add(message);
        
        // 同时创建记忆条目
        String source = "assistant".equals(message.getRole()) ? "self" : "player";
        entries.add(MemoryEntry.dialogue(message.getContent(), source));
        
        // 裁剪超出容量的消息
        trimToCapacity();
    }
    
    /**
     * 添加用户消息
     */
    public void addUserMessage(String content) {
        addMessage(ChatMessage.user(content));
    }
    
    /**
     * 添加助手消息
     */
    public void addAssistantMessage(String content) {
        addMessage(ChatMessage.assistant(content));
    }
    
    /**
     * 添加记忆条目（非对话类型）
     * 
     * @param entry 记忆条目
     */
    public synchronized void addEntry(MemoryEntry entry) {
        entries.add(entry);
        trimEntriesToCapacity();
    }
    
    /**
     * 获取所有对话消息（不包含系统提示词）
     * 
     * @return 不可变的消息列表
     */
    public synchronized List<ChatMessage> getMessages() {
        return Collections.unmodifiableList(new ArrayList<>(messages));
    }
    
    /**
     * 获取最近的 N 条消息
     * 
     * @param n 消息数量
     * @return 消息列表
     */
    public synchronized List<ChatMessage> getRecentMessages(int n) {
        int size = messages.size();
        if (n >= size) {
            return getMessages();
        }
        return Collections.unmodifiableList(new ArrayList<>(messages.subList(size - n, size)));
    }
    
    /**
     * 获取所有记忆条目
     * 
     * @return 不可变的记忆列表
     */
    public synchronized List<MemoryEntry> getEntries() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }
    
    /**
     * 获取最近的高重要性记忆
     * 
     * @param minImportance 最小重要性阈值
     * @param limit 最大返回数量
     * @return 记忆列表
     */
    public synchronized List<MemoryEntry> getImportantEntries(float minImportance, int limit) {
        return entries.stream()
                .filter(e -> e.getImportance() >= minImportance)
                .sorted((a, b) -> Float.compare(b.getImportance(), a.getImportance()))
                .limit(limit)
                .toList();
    }
    
    /**
     * 清空所有记忆
     */
    public synchronized void clear() {
        messages.clear();
        entries.clear();
        LOGGER.debug("即时记忆已清空");
    }
    
    /**
     * 获取当前消息数量
     */
    public synchronized int getMessageCount() {
        return messages.size();
    }
    
    /**
     * 获取当前记忆条目数量
     */
    public synchronized int getEntryCount() {
        return entries.size();
    }
    
    /**
     * 获取容量
     */
    public int getCapacity() {
        return capacity;
    }
    
    /**
     * 构建用于 prompt 的上下文摘要
     * 
     * @return 格式化的上下文字符串
     */
    public synchronized String buildContextSummary() {
        if (entries.isEmpty()) {
            return "（暂无对话历史）";
        }
        
        StringBuilder sb = new StringBuilder();
        
        // 只取最近的几条重要记忆
        List<MemoryEntry> recent = entries.subList(
                Math.max(0, entries.size() - 5), 
                entries.size()
        );
        
        for (MemoryEntry entry : recent) {
            switch (entry.getType()) {
                case DIALOGUE:
                    sb.append("- ").append(entry.getSource()).append(": ");
                    sb.append(truncate(entry.getContent(), 50)).append("\n");
                    break;
                case EVENT:
                    sb.append("- [事件] ").append(entry.getContent()).append("\n");
                    break;
                case OBSERVATION:
                    sb.append("- [观察] ").append(entry.getContent()).append("\n");
                    break;
                default:
                    break;
            }
        }
        
        return sb.toString();
    }
    
    /**
     * 裁剪消息到容量限制
     */
    private void trimToCapacity() {
        while (messages.size() > capacity) {
            messages.remove(0);
        }
    }
    
    /**
     * 裁剪记忆条目到容量限制（记忆条目容量是消息容量的 2 倍）
     */
    private void trimEntriesToCapacity() {
        int entryCapacity = capacity * 2;
        while (entries.size() > entryCapacity) {
            entries.remove(0);
        }
    }
    
    /**
     * 截断字符串
     */
    private static String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
