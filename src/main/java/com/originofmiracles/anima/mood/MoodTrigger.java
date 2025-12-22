package com.originofmiracles.anima.mood;

/**
 * 情绪触发器
 * 
 * 定义可能影响角色情绪的事件类型
 */
public enum MoodTrigger {
    
    // ==================== 正面触发器 ====================
    
    /** 收到礼物 */
    RECEIVED_GIFT("received_gift", 0.5f, MoodState.HAPPY),
    
    /** 收到喜欢的礼物 */
    RECEIVED_FAVORITE_GIFT("received_favorite_gift", 0.8f, MoodState.EXCITED),
    
    /** 被称赞 */
    RECEIVED_COMPLIMENT("received_compliment", 0.4f, MoodState.HAPPY),
    
    /** 完成任务 */
    TASK_COMPLETED("task_completed", 0.6f, MoodState.HAPPY),
    
    /** 玩家问候 */
    GREETED("greeted", 0.3f, MoodState.HAPPY),
    
    /** 开始对话 */
    CONVERSATION_STARTED("conversation_started", 0.2f, MoodState.ANTICIPATING),
    
    // ==================== 负面触发器 ====================
    
    /** 被攻击 */
    ATTACKED("attacked", -0.7f, MoodState.ANGRY),
    
    /** 任务失败 */
    TASK_FAILED("task_failed", -0.5f, MoodState.SAD),
    
    /** 被忽视（长时间不互动） */
    IGNORED("ignored", -0.3f, MoodState.SAD),
    
    /** 收到不喜欢的礼物 */
    RECEIVED_DISLIKED_GIFT("received_disliked_gift", -0.2f, MoodState.CONFUSED),
    
    /** 被打断 */
    INTERRUPTED("interrupted", -0.4f, MoodState.ANGRY),
    
    // ==================== 中性触发器 ====================
    
    /** 观察到有趣的事物 */
    SAW_SOMETHING_INTERESTING("saw_interesting", 0.3f, MoodState.SURPRISED),
    
    /** 被问问题 */
    ASKED_QUESTION("asked_question", 0.1f, MoodState.THINKING),
    
    /** 时间流逝（情绪衰减） */
    TIME_PASSED("time_passed", 0f, MoodState.NEUTRAL),
    
    /** 对话结束 */
    CONVERSATION_ENDED("conversation_ended", -0.1f, MoodState.NEUTRAL);
    
    private final String id;
    private final float valenceModifier;  // 情感效价修改量
    private final MoodState suggestedState;  // 建议的目标状态
    
    MoodTrigger(String id, float valenceModifier, MoodState suggestedState) {
        this.id = id;
        this.valenceModifier = valenceModifier;
        this.suggestedState = suggestedState;
    }
    
    public String getId() {
        return id;
    }
    
    public float getValenceModifier() {
        return valenceModifier;
    }
    
    public MoodState getSuggestedState() {
        return suggestedState;
    }
    
    /**
     * 根据 ID 查找触发器
     */
    public static MoodTrigger fromId(String id) {
        for (MoodTrigger trigger : values()) {
            if (trigger.id.equalsIgnoreCase(id)) {
                return trigger;
            }
        }
        return null;
    }
}
