package com.originofmiracles.anima.memory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;

/**
 * 短期记忆
 * 
 * 存储近期（默认7天）的事件摘要和重要对话
 * 使用 JSON 文件持久化，按学生 ID 分隔
 * 
 * 记忆结构：
 * - 按日期分组
 * - 每天保存重要事件的摘要
 * - 定期清理过期记忆
 */
public class ShortTermMemory {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    /** 默认保留天数 */
    public static final int DEFAULT_RETENTION_DAYS = 7;
    
    /** 每天最大记忆条数 */
    public static final int MAX_ENTRIES_PER_DAY = 20;
    
    private final String studentId;
    private final Path storagePath;
    private final int retentionDays;
    
    /** 按日期存储的记忆 */
    private final Map<String, List<MemoryEntry>> dailyMemories;
    
    /**
     * 创建短期记忆
     * 
     * @param studentId 学生 ID
     * @param storageDir 存储目录
     */
    public ShortTermMemory(String studentId, Path storageDir) {
        this(studentId, storageDir, DEFAULT_RETENTION_DAYS);
    }
    
    /**
     * 创建短期记忆
     * 
     * @param studentId 学生 ID
     * @param storageDir 存储目录
     * @param retentionDays 保留天数
     */
    public ShortTermMemory(String studentId, Path storageDir, int retentionDays) {
        this.studentId = studentId;
        this.storagePath = storageDir.resolve(studentId + "_memory.json");
        this.retentionDays = retentionDays;
        this.dailyMemories = new HashMap<>();
        
        load();
    }
    
    /**
     * 添加记忆条目
     * 
     * @param entry 记忆条目
     */
    public synchronized void addEntry(MemoryEntry entry) {
        String dateKey = getDateKey(entry.getTimestamp());
        
        dailyMemories.computeIfAbsent(dateKey, k -> new ArrayList<>())
                .add(entry);
        
        // 限制每天的记忆数量
        trimDailyEntries(dateKey);
        
        LOGGER.debug("[{}] 添加短期记忆: {}", studentId, entry.getType());
    }
    
    /**
     * 从即时记忆中提取重要记忆到短期记忆
     * 
     * @param immediateMemory 即时记忆
     * @param importanceThreshold 重要性阈值
     */
    public synchronized void extractFromImmediate(ImmediateMemory immediateMemory, float importanceThreshold) {
        List<MemoryEntry> important = immediateMemory.getImportantEntries(importanceThreshold, 5);
        
        for (MemoryEntry entry : important) {
            addEntry(entry);
        }
        
        LOGGER.debug("[{}] 从即时记忆提取了 {} 条重要记忆", studentId, important.size());
    }
    
    /**
     * 获取指定日期的记忆
     * 
     * @param date 日期
     * @return 记忆列表
     */
    public synchronized List<MemoryEntry> getEntriesForDate(LocalDate date) {
        String dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        return new ArrayList<>(dailyMemories.getOrDefault(dateKey, List.of()));
    }
    
    /**
     * 获取今天的记忆
     */
    public List<MemoryEntry> getTodayEntries() {
        return getEntriesForDate(LocalDate.now());
    }
    
    /**
     * 获取最近 N 天的所有记忆
     * 
     * @param days 天数
     * @return 按时间排序的记忆列表
     */
    public synchronized List<MemoryEntry> getRecentEntries(int days) {
        LocalDate today = LocalDate.now();
        List<MemoryEntry> result = new ArrayList<>();
        
        for (int i = 0; i < days; i++) {
            String dateKey = today.minusDays(i).format(DateTimeFormatter.ISO_LOCAL_DATE);
            List<MemoryEntry> entries = dailyMemories.get(dateKey);
            if (entries != null) {
                result.addAll(entries);
            }
        }
        
        // 按时间排序
        result.sort(Comparator.comparing(MemoryEntry::getTimestamp));
        
        return result;
    }
    
    /**
     * 搜索包含关键词的记忆
     * 
     * @param keyword 关键词
     * @param limit 最大返回数量
     * @return 匹配的记忆列表
     */
    public synchronized List<MemoryEntry> searchByKeyword(String keyword, int limit) {
        String lowerKeyword = keyword.toLowerCase();
        
        return dailyMemories.values().stream()
                .flatMap(List::stream)
                .filter(e -> e.getContent().toLowerCase().contains(lowerKeyword))
                .sorted(Comparator.comparing(MemoryEntry::getTimestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * 构建用于 prompt 的记忆摘要
     * 
     * @param maxEntries 最大条目数
     * @return 格式化的摘要字符串
     */
    public synchronized String buildSummary(int maxEntries) {
        List<MemoryEntry> recent = getRecentEntries(retentionDays);
        
        if (recent.isEmpty()) {
            return "（没有近期记忆）";
        }
        
        StringBuilder sb = new StringBuilder();
        
        // 按日期分组显示
        Map<String, List<MemoryEntry>> grouped = recent.stream()
                .limit(maxEntries)
                .collect(Collectors.groupingBy(e -> getDateKey(e.getTimestamp())));
        
        grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    sb.append("【").append(entry.getKey()).append("】\n");
                    for (MemoryEntry mem : entry.getValue()) {
                        sb.append("- ").append(summarizeEntry(mem)).append("\n");
                    }
                });
        
        return sb.toString();
    }
    
    /**
     * 清理过期记忆
     */
    public synchronized void cleanup() {
        LocalDate cutoffDate = LocalDate.now().minusDays(retentionDays);
        String cutoffKey = cutoffDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        int removed = 0;
        var iterator = dailyMemories.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getKey().compareTo(cutoffKey) < 0) {
                iterator.remove();
                removed++;
            }
        }
        
        if (removed > 0) {
            LOGGER.info("[{}] 清理了 {} 天的过期记忆", studentId, removed);
            save();
        }
    }
    
    /**
     * 保存到文件
     */
    public synchronized void save() {
        try {
            Files.createDirectories(storagePath.getParent());
            
            // 序列化为 JSON
            String json = GSON.toJson(dailyMemories);
            Files.writeString(storagePath, json);
            
            LOGGER.debug("[{}] 短期记忆已保存", studentId);
        } catch (IOException e) {
            LOGGER.error("[{}] 保存短期记忆失败", studentId, e);
        }
    }
    
    /**
     * 从文件加载
     */
    private void load() {
        if (!Files.exists(storagePath)) {
            LOGGER.debug("[{}] 短期记忆文件不存在，将创建新的", studentId);
            return;
        }
        
        try {
            String json = Files.readString(storagePath);
            Map<String, List<MemoryEntry>> loaded = GSON.fromJson(
                    json, 
                    new TypeToken<Map<String, List<MemoryEntry>>>(){}.getType()
            );
            
            if (loaded != null) {
                dailyMemories.putAll(loaded);
                cleanup();  // 加载后清理过期记忆
                LOGGER.info("[{}] 已加载短期记忆，共 {} 天", studentId, dailyMemories.size());
            }
        } catch (Exception e) {
            LOGGER.error("[{}] 加载短期记忆失败", studentId, e);
        }
    }
    
    /**
     * 获取日期键
     */
    private String getDateKey(Instant timestamp) {
        return LocalDate.ofInstant(timestamp, ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
    
    /**
     * 裁剪每天的记忆数量
     */
    private void trimDailyEntries(String dateKey) {
        List<MemoryEntry> entries = dailyMemories.get(dateKey);
        if (entries != null && entries.size() > MAX_ENTRIES_PER_DAY) {
            // 按重要性排序，保留最重要的
            entries.sort(Comparator.comparing(MemoryEntry::getImportance).reversed());
            dailyMemories.put(dateKey, new ArrayList<>(entries.subList(0, MAX_ENTRIES_PER_DAY)));
        }
    }
    
    /**
     * 生成记忆条目的摘要描述
     */
    private String summarizeEntry(MemoryEntry entry) {
        String content = entry.getContent();
        if (content.length() > 100) {
            content = content.substring(0, 97) + "...";
        }
        
        return switch (entry.getType()) {
            case DIALOGUE -> entry.getSource() + "说: \"" + content + "\"";
            case EVENT -> content;
            case OBSERVATION -> "观察到: " + content;
            case EMOTION -> "感受到: " + content;
        };
    }
    
    /**
     * 获取学生 ID
     */
    public String getStudentId() {
        return studentId;
    }
    
    /**
     * 获取总记忆条数
     */
    public synchronized int getTotalEntryCount() {
        return dailyMemories.values().stream()
                .mapToInt(List::size)
                .sum();
    }
}
