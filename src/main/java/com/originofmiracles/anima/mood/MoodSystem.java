package com.originofmiracles.anima.mood;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

/**
 * 情绪系统
 * 
 * 管理角色的情绪状态转换
 * 
 * 特性：
 * - 情绪强度 (0.0 - 1.0)
 * - 情绪衰减（随时间趋向中性）
 * - 情绪转换平滑过渡
 * - 触发器驱动的状态变化
 */
public class MoodSystem {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /** 情绪衰减速率（每秒） */
    private static final float DECAY_RATE_PER_SECOND = 0.02f;
    
    /** 情绪变化平滑因子 */
    private static final float SMOOTHING_FACTOR = 0.3f;
    
    /** 触发状态切换的最小强度阈值 */
    private static final float STATE_CHANGE_THRESHOLD = 0.4f;
    
    private final String studentId;
    
    // 当前情绪状态
    private MoodState currentState = MoodState.NEUTRAL;
    private float intensity = 0.5f;  // 强度 0.0 - 1.0
    private float valence = 0f;      // 效价 -1.0 到 1.0
    
    // 上次更新时间
    private Instant lastUpdateTime = Instant.now();
    
    // 情绪变化监听器
    private final List<Consumer<MoodChangeEvent>> listeners = new ArrayList<>();
    
    public MoodSystem(String studentId) {
        this.studentId = studentId;
        LOGGER.debug("[{}] 情绪系统已初始化", studentId);
    }
    
    /**
     * 应用情绪触发器
     * 
     * @param trigger 触发器
     */
    public void applyTrigger(MoodTrigger trigger) {
        applyTrigger(trigger, 1.0f);
    }
    
    /**
     * 应用情绪触发器（带强度修饰）
     * 
     * @param trigger 触发器
     * @param multiplier 强度倍数
     */
    public synchronized void applyTrigger(MoodTrigger trigger, float multiplier) {
        // 更新效价
        float valenceChange = trigger.getValenceModifier() * multiplier;
        this.valence = clamp(this.valence + valenceChange * SMOOTHING_FACTOR, -1f, 1f);
        
        // 计算新强度
        float newIntensity = Math.abs(this.valence);
        this.intensity = lerp(this.intensity, newIntensity, SMOOTHING_FACTOR);
        
        // 检查是否需要切换状态
        MoodState previousState = currentState;
        if (this.intensity >= STATE_CHANGE_THRESHOLD) {
            MoodState suggestedState = trigger.getSuggestedState();
            if (suggestedState != currentState) {
                currentState = suggestedState;
                LOGGER.debug("[{}] 情绪变化: {} -> {} (强度: {})", 
                        studentId, previousState, currentState, String.format("%.2f", intensity));
            }
        } else if (this.intensity < STATE_CHANGE_THRESHOLD * 0.5f) {
            // 强度过低时回归中性
            currentState = MoodState.NEUTRAL;
        }
        
        // 通知监听器
        if (previousState != currentState) {
            notifyListeners(previousState, currentState);
        }
        
        lastUpdateTime = Instant.now();
    }
    
    /**
     * 直接设置情绪状态
     * 
     * @param state 目标状态
     * @param intensity 强度
     */
    public synchronized void setState(MoodState state, float intensity) {
        MoodState previousState = this.currentState;
        this.currentState = state;
        this.intensity = clamp(intensity, 0f, 1f);
        this.valence = state.getBaseValence() * this.intensity;
        
        if (previousState != state) {
            LOGGER.debug("[{}] 情绪设置: {} (强度: {})", studentId, state, String.format("%.2f", intensity));
            notifyListeners(previousState, state);
        }
        
        lastUpdateTime = Instant.now();
    }
    
    /**
     * 更新情绪（应用时间衰减）
     * 应该定期调用（如每游戏 tick 或每秒）
     */
    public synchronized void update() {
        Instant now = Instant.now();
        float secondsElapsed = (now.getEpochSecond() - lastUpdateTime.getEpochSecond()) + 
                (now.getNano() - lastUpdateTime.getNano()) / 1_000_000_000f;
        
        if (secondsElapsed > 0) {
            // 效价向零衰减
            float decay = DECAY_RATE_PER_SECOND * secondsElapsed;
            if (valence > 0) {
                valence = Math.max(0, valence - decay);
            } else if (valence < 0) {
                valence = Math.min(0, valence + decay);
            }
            
            // 强度衰减
            intensity = Math.max(0.1f, intensity - decay * 0.5f);
            
            // 检查是否回归中性
            if (intensity < STATE_CHANGE_THRESHOLD * 0.5f && currentState != MoodState.NEUTRAL) {
                MoodState previous = currentState;
                currentState = MoodState.NEUTRAL;
                notifyListeners(previous, currentState);
            }
            
            lastUpdateTime = now;
        }
    }
    
    /**
     * 添加情绪变化监听器
     */
    public void addListener(Consumer<MoodChangeEvent> listener) {
        listeners.add(listener);
    }
    
    /**
     * 移除监听器
     */
    public void removeListener(Consumer<MoodChangeEvent> listener) {
        listeners.remove(listener);
    }
    
    // ==================== 状态查询 ====================
    
    /**
     * 获取当前情绪状态
     */
    public MoodState getCurrentState() {
        return currentState;
    }
    
    /**
     * 获取当前情绪强度 (0.0 - 1.0)
     */
    public float getIntensity() {
        return intensity;
    }
    
    /**
     * 获取当前情感效价 (-1.0 到 1.0)
     */
    public float getValence() {
        return valence;
    }
    
    /**
     * 获取情绪描述（用于 prompt）
     */
    public String getMoodDescription() {
        String intensityDesc;
        if (intensity < 0.3f) {
            intensityDesc = "轻微";
        } else if (intensity < 0.6f) {
            intensityDesc = "一般";
        } else if (intensity < 0.8f) {
            intensityDesc = "明显";
        } else {
            intensityDesc = "强烈";
        }
        
        if (currentState == MoodState.NEUTRAL) {
            return "情绪平静";
        }
        
        return intensityDesc + currentState.getDisplayName();
    }
    
    /**
     * 获取情绪状态百分比（用于 UI 显示）
     */
    public int getIntensityPercent() {
        return Math.round(intensity * 100);
    }
    
    /**
     * 检查是否处于正面情绪
     */
    public boolean isPositive() {
        return currentState.isPositive() && intensity > STATE_CHANGE_THRESHOLD;
    }
    
    /**
     * 检查是否处于负面情绪
     */
    public boolean isNegative() {
        return currentState.isNegative() && intensity > STATE_CHANGE_THRESHOLD;
    }
    
    // ==================== 辅助方法 ====================
    
    private void notifyListeners(MoodState previous, MoodState current) {
        MoodChangeEvent event = new MoodChangeEvent(studentId, previous, current, intensity);
        for (Consumer<MoodChangeEvent> listener : listeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                LOGGER.error("[{}] 情绪监听器异常", studentId, e);
            }
        }
    }
    
    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    
    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
    
    /**
     * 情绪变化事件
     */
    public static class MoodChangeEvent {
        private final String studentId;
        private final MoodState previousState;
        private final MoodState newState;
        private final float intensity;
        private final Instant timestamp;
        
        public MoodChangeEvent(String studentId, MoodState previous, MoodState current, float intensity) {
            this.studentId = studentId;
            this.previousState = previous;
            this.newState = current;
            this.intensity = intensity;
            this.timestamp = Instant.now();
        }
        
        public String getStudentId() {
            return studentId;
        }
        
        public MoodState getPreviousState() {
            return previousState;
        }
        
        public MoodState getNewState() {
            return newState;
        }
        
        public float getIntensity() {
            return intensity;
        }
        
        public Instant getTimestamp() {
            return timestamp;
        }
    }
}
