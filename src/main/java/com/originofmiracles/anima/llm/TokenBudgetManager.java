package com.originofmiracles.anima.llm;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

/**
 * Token 预算管理器
 * 
 * 控制 LLM 请求的并发数和速率，避免超出 API 限制
 * 
 * 功能：
 * - 并发请求限制
 * - 每分钟请求速率限制
 * - Token 使用统计
 */
public class TokenBudgetManager {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /** 默认最大并发请求数 */
    public static final int DEFAULT_MAX_CONCURRENT = 5;
    
    /** 默认每分钟最大请求数 */
    public static final int DEFAULT_RATE_LIMIT_RPM = 60;
    
    /** 请求超时时间（秒） */
    public static final int ACQUIRE_TIMEOUT_SECONDS = 30;
    
    private final Semaphore concurrentLimit;
    private final int maxConcurrent;
    private final int rateLimitRpm;
    
    // 速率限制跟踪
    private final AtomicInteger requestsThisMinute = new AtomicInteger(0);
    private final AtomicLong minuteStartTime = new AtomicLong(System.currentTimeMillis());
    
    // 统计信息
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalPromptTokens = new AtomicLong(0);
    private final AtomicLong totalCompletionTokens = new AtomicLong(0);
    private final AtomicInteger rejectedRequests = new AtomicInteger(0);
    
    public TokenBudgetManager() {
        this(DEFAULT_MAX_CONCURRENT, DEFAULT_RATE_LIMIT_RPM);
    }
    
    public TokenBudgetManager(int maxConcurrent, int rateLimitRpm) {
        this.maxConcurrent = maxConcurrent;
        this.rateLimitRpm = rateLimitRpm;
        this.concurrentLimit = new Semaphore(maxConcurrent, true);  // 公平模式
        
        LOGGER.info("Token 预算管理器已初始化: 最大并发={}, 速率限制={}/分钟", 
                maxConcurrent, rateLimitRpm);
    }
    
    /**
     * 尝试获取请求许可
     * 如果超出限制会阻塞等待
     * 
     * @return 是否成功获取许可
     * @throws InterruptedException 如果等待被中断
     */
    public boolean tryAcquire() throws InterruptedException {
        // 检查速率限制
        if (!checkRateLimit()) {
            rejectedRequests.incrementAndGet();
            LOGGER.warn("请求被速率限制拒绝");
            return false;
        }
        
        // 获取并发许可
        boolean acquired = concurrentLimit.tryAcquire(ACQUIRE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        if (acquired) {
            requestsThisMinute.incrementAndGet();
            totalRequests.incrementAndGet();
        } else {
            rejectedRequests.incrementAndGet();
            LOGGER.warn("请求超时：无法获取并发许可");
        }
        
        return acquired;
    }
    
    /**
     * 释放请求许可
     * 在请求完成后调用
     */
    public void release() {
        concurrentLimit.release();
    }
    
    /**
     * 记录 Token 使用量
     * 
     * @param promptTokens 提示词 Token 数
     * @param completionTokens 补全 Token 数
     */
    public void recordTokenUsage(int promptTokens, int completionTokens) {
        totalPromptTokens.addAndGet(promptTokens);
        totalCompletionTokens.addAndGet(completionTokens);
    }
    
    /**
     * 检查速率限制
     * 
     * @return 是否在限制内
     */
    private boolean checkRateLimit() {
        long now = System.currentTimeMillis();
        long minuteStart = minuteStartTime.get();
        
        // 检查是否进入新的一分钟
        if (now - minuteStart >= 60_000) {
            // 重置计数
            minuteStartTime.set(now);
            requestsThisMinute.set(0);
            return true;
        }
        
        // 检查是否超出限制
        return requestsThisMinute.get() < rateLimitRpm;
    }
    
    /**
     * 获取剩余等待时间（毫秒）
     * 如果当前分钟请求已满
     * 
     * @return 等待毫秒数，如果不需要等待返回 0
     */
    public long getRemainingWaitTime() {
        if (requestsThisMinute.get() < rateLimitRpm) {
            return 0;
        }
        
        long elapsed = System.currentTimeMillis() - minuteStartTime.get();
        return Math.max(0, 60_000 - elapsed);
    }
    
    // ==================== 统计查询 ====================
    
    /**
     * 获取当前可用的并发许可数
     */
    public int getAvailablePermits() {
        return concurrentLimit.availablePermits();
    }
    
    /**
     * 获取当前分钟的请求数
     */
    public int getCurrentMinuteRequests() {
        return requestsThisMinute.get();
    }
    
    /**
     * 获取总请求数
     */
    public long getTotalRequests() {
        return totalRequests.get();
    }
    
    /**
     * 获取总提示词 Token 数
     */
    public long getTotalPromptTokens() {
        return totalPromptTokens.get();
    }
    
    /**
     * 获取总补全 Token 数
     */
    public long getTotalCompletionTokens() {
        return totalCompletionTokens.get();
    }
    
    /**
     * 获取总 Token 数
     */
    public long getTotalTokens() {
        return totalPromptTokens.get() + totalCompletionTokens.get();
    }
    
    /**
     * 获取被拒绝的请求数
     */
    public int getRejectedRequests() {
        return rejectedRequests.get();
    }
    
    /**
     * 获取统计信息摘要
     */
    public String getStatsSummary() {
        return String.format(
                "LLM 统计: 总请求=%d, 总Token=%d (提示=%d, 补全=%d), 拒绝=%d, 当前并发=%d/%d",
                totalRequests.get(),
                getTotalTokens(),
                totalPromptTokens.get(),
                totalCompletionTokens.get(),
                rejectedRequests.get(),
                maxConcurrent - concurrentLimit.availablePermits(),
                maxConcurrent
        );
    }
    
    /**
     * 重置统计信息
     */
    public void resetStats() {
        totalRequests.set(0);
        totalPromptTokens.set(0);
        totalCompletionTokens.set(0);
        rejectedRequests.set(0);
        requestsThisMinute.set(0);
        
        LOGGER.info("Token 预算统计已重置");
    }
}
