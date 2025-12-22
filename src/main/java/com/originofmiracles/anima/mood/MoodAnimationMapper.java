package com.originofmiracles.anima.mood;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 情绪动画映射器
 * 
 * 将情绪状态映射到 YSM 动画 ID
 */
public class MoodAnimationMapper {
    
    private static final Random RANDOM = new Random();
    
    /**
     * 情绪状态 -> 动画 ID 列表的映射
     */
    private static final Map<MoodState, List<String>> ANIMATION_MAP = new HashMap<>();
    
    static {
        // 初始化映射
        ANIMATION_MAP.put(MoodState.NEUTRAL, List.of("idle_normal", "idle_look_around"));
        ANIMATION_MAP.put(MoodState.HAPPY, List.of("emote_happy", "emote_smile", "idle_happy"));
        ANIMATION_MAP.put(MoodState.EXCITED, List.of("emote_excited", "emote_jump", "emote_cheer"));
        ANIMATION_MAP.put(MoodState.SAD, List.of("emote_sad", "emote_depressed", "idle_sad"));
        ANIMATION_MAP.put(MoodState.ANGRY, List.of("emote_angry", "emote_annoyed"));
        ANIMATION_MAP.put(MoodState.SURPRISED, List.of("emote_surprised", "emote_shocked"));
        ANIMATION_MAP.put(MoodState.SHY, List.of("emote_shy", "emote_blush"));
        ANIMATION_MAP.put(MoodState.CONFUSED, List.of("emote_confused", "emote_think"));
        ANIMATION_MAP.put(MoodState.THINKING, List.of("emote_think", "idle_think"));
        ANIMATION_MAP.put(MoodState.TIRED, List.of("emote_tired", "emote_yawn", "idle_tired"));
        ANIMATION_MAP.put(MoodState.WORRIED, List.of("emote_worried", "emote_nervous"));
        ANIMATION_MAP.put(MoodState.ANTICIPATING, List.of("emote_anticipate", "idle_curious"));
    }
    
    /**
     * 获取情绪对应的动画 ID
     * 如果有多个可选动画，随机选择一个
     * 
     * @param state 情绪状态
     * @return 动画 ID
     */
    public static String getAnimationId(MoodState state) {
        List<String> animations = ANIMATION_MAP.get(state);
        if (animations == null || animations.isEmpty()) {
            return "idle_normal";
        }
        
        return animations.get(RANDOM.nextInt(animations.size()));
    }
    
    /**
     * 获取情绪对应的所有动画 ID
     * 
     * @param state 情绪状态
     * @return 动画 ID 列表
     */
    public static List<String> getAnimationIds(MoodState state) {
        return ANIMATION_MAP.getOrDefault(state, List.of("idle_normal"));
    }
    
    /**
     * 根据强度选择动画
     * 高强度时选择更激烈的动画（通常在列表后面）
     * 
     * @param state 情绪状态
     * @param intensity 强度 (0.0 - 1.0)
     * @return 动画 ID
     */
    public static String getAnimationByIntensity(MoodState state, float intensity) {
        List<String> animations = ANIMATION_MAP.get(state);
        if (animations == null || animations.isEmpty()) {
            return "idle_normal";
        }
        
        // 根据强度选择动画
        // 强度越高，越倾向于选择列表后面（更激烈）的动画
        int index = Math.min((int) (intensity * animations.size()), animations.size() - 1);
        return animations.get(index);
    }
    
    /**
     * 获取表情 ID（用于面部表情系统）
     * 
     * @param state 情绪状态
     * @return 表情 ID
     */
    public static String getExpressionId(MoodState state) {
        return "expr_" + state.getId();
    }
    
    /**
     * 注册自定义动画映射
     * 
     * @param state 情绪状态
     * @param animations 动画 ID 列表
     */
    public static void registerAnimations(MoodState state, List<String> animations) {
        ANIMATION_MAP.put(state, animations);
    }
}
