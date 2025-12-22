package com.originofmiracles.anima.mood;

/**
 * 情绪状态枚举
 * 
 * 定义角色可能的情绪状态
 * 每个状态对应不同的表现和动画
 */
public enum MoodState {
    
    /** 中性/默认状态 */
    NEUTRAL("neutral", "平静", 0f),
    
    /** 开心 */
    HAPPY("happy", "开心", 0.7f),
    
    /** 兴奋 */
    EXCITED("excited", "兴奋", 0.9f),
    
    /** 难过 */
    SAD("sad", "难过", -0.6f),
    
    /** 生气 */
    ANGRY("angry", "生气", -0.8f),
    
    /** 惊讶 */
    SURPRISED("surprised", "惊讶", 0.3f),
    
    /** 害羞 */
    SHY("shy", "害羞", 0.4f),
    
    /** 困惑 */
    CONFUSED("confused", "困惑", -0.2f),
    
    /** 思考中 */
    THINKING("thinking", "思考中", 0f),
    
    /** 疲惫 */
    TIRED("tired", "疲惫", -0.3f),
    
    /** 担心 */
    WORRIED("worried", "担心", -0.4f),
    
    /** 期待 */
    ANTICIPATING("anticipating", "期待", 0.5f);
    
    private final String id;
    private final String displayName;
    private final float baseValence;  // 基础情感效价 (-1.0 到 1.0)
    
    MoodState(String id, String displayName, float baseValence) {
        this.id = id;
        this.displayName = displayName;
        this.baseValence = baseValence;
    }
    
    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public float getBaseValence() {
        return baseValence;
    }
    
    /**
     * 是否为正面情绪
     */
    public boolean isPositive() {
        return baseValence > 0.1f;
    }
    
    /**
     * 是否为负面情绪
     */
    public boolean isNegative() {
        return baseValence < -0.1f;
    }
    
    /**
     * 根据 ID 查找状态
     */
    public static MoodState fromId(String id) {
        for (MoodState state : values()) {
            if (state.id.equalsIgnoreCase(id)) {
                return state;
            }
        }
        return NEUTRAL;
    }
}
