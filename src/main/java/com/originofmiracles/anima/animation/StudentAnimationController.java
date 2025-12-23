package com.originofmiracles.anima.animation;

import com.originofmiracles.anima.entity.StudentEntity;
import com.originofmiracles.anima.mood.MoodState;

import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

/**
 * 学生动画控制器
 * 
 * 负责根据实体状态（移动、情绪、行为）自动切换动画。
 * 
 * 动画优先级：
 * 1. 强制动画（由 AI 触发）- 如 "greeting", "thinking"
 * 2. 情绪动画（情绪系统触发）- 如 "happy_idle", "sad_walk"
 * 3. 基础动画（移动状态）- 如 "walk", "idle"
 */
public class StudentAnimationController {
    
    /**
     * 动画名称常量
     */
    public static class Animations {
        // 基础动画
        public static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
        public static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
        public static final RawAnimation RUN = RawAnimation.begin().thenLoop("run");
        public static final RawAnimation SIT = RawAnimation.begin().thenLoop("sit");
        public static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("sleep");
        
        // 情绪动画（Idle 变体）
        public static final RawAnimation IDLE_HAPPY = RawAnimation.begin().thenLoop("idle_happy");
        public static final RawAnimation IDLE_SAD = RawAnimation.begin().thenLoop("idle_sad");
        public static final RawAnimation IDLE_ANGRY = RawAnimation.begin().thenLoop("idle_angry");
        public static final RawAnimation IDLE_SURPRISED = RawAnimation.begin().thenLoop("idle_surprised");
        public static final RawAnimation IDLE_THINKING = RawAnimation.begin().thenLoop("idle_thinking");
        
        // 交互动画（单次播放）
        public static final RawAnimation GREETING = RawAnimation.begin().thenPlay("greeting");
        public static final RawAnimation WAVE = RawAnimation.begin().thenPlay("wave");
        public static final RawAnimation NOD = RawAnimation.begin().thenPlay("nod");
        public static final RawAnimation SHAKE_HEAD = RawAnimation.begin().thenPlay("shake_head");
        public static final RawAnimation HURT = RawAnimation.begin().thenPlay("hurt");
    }
    
    /**
     * 创建主动画控制器
     * 
     * @param entity 学生实体
     * @return 动画控制器
     */
    public static AnimationController<StudentEntity> createMainController(StudentEntity entity) {
        return new AnimationController<>(entity, "main_controller", 5, 
            StudentAnimationController::mainAnimationPredicate);
    }
    
    /**
     * 主动画逻辑
     * 
     * @param state 动画状态
     * @return 播放状态
     */
    private static PlayState mainAnimationPredicate(AnimationState<StudentEntity> state) {
        StudentEntity entity = state.getAnimatable();
        
        // 1. 检查强制动画
        String forcedAnim = entity.getAnimation();
        if (forcedAnim != null && !forcedAnim.isEmpty() && !forcedAnim.equals("idle")) {
            RawAnimation animation = getAnimationByName(forcedAnim);
            if (animation != null) {
                return state.setAndContinue(animation);
            }
        }
        
        // 2. 检查情绪动画
        MoodState mood = entity.getMood();
        if (mood != null && mood != MoodState.NEUTRAL) {
            RawAnimation moodAnimation = getMoodAnimation(mood, state.isMoving());
            if (moodAnimation != null) {
                return state.setAndContinue(moodAnimation);
            }
        }
        
        // 3. 基础动画
        if (state.isMoving()) {
            return state.setAndContinue(Animations.WALK);
        } else {
            return state.setAndContinue(Animations.IDLE);
        }
    }
    
    /**
     * 根据名称获取动画
     * 
     * @param name 动画名称
     * @return 动画对象，如果未找到则返回 null
     */
    private static RawAnimation getAnimationByName(String name) {
        return switch (name.toLowerCase()) {
            case "idle" -> Animations.IDLE;
            case "walk" -> Animations.WALK;
            case "run" -> Animations.RUN;
            case "sit" -> Animations.SIT;
            case "sleep" -> Animations.SLEEP;
            case "greeting" -> Animations.GREETING;
            case "wave" -> Animations.WAVE;
            case "nod" -> Animations.NOD;
            case "shake_head" -> Animations.SHAKE_HEAD;
            case "hurt" -> Animations.HURT;
            case "happy_idle", "idle_happy" -> Animations.IDLE_HAPPY;
            case "sad_idle", "idle_sad" -> Animations.IDLE_SAD;
            case "angry_idle", "idle_angry" -> Animations.IDLE_ANGRY;
            case "surprised_idle", "idle_surprised" -> Animations.IDLE_SURPRISED;
            case "thinking_idle", "idle_thinking" -> Animations.IDLE_THINKING;
            default -> null;
        };
    }
    
    /**
     * 根据情绪获取动画
     * 
     * @param mood 情绪状态
     * @param isMoving 是否在移动
     * @return 情绪动画
     */
    private static RawAnimation getMoodAnimation(MoodState mood, boolean isMoving) {
        if (isMoving) {
            // 移动时使用基础 walk 动画（可以后续添加情绪变体）
            return Animations.WALK;
        }
        
        return switch (mood) {
            case HAPPY, EXCITED -> Animations.IDLE_HAPPY;
            case SAD -> Animations.IDLE_SAD;
            case ANGRY -> Animations.IDLE_ANGRY;
            case SURPRISED -> Animations.IDLE_SURPRISED;
            case THINKING, CONFUSED -> Animations.IDLE_THINKING;
            default -> Animations.IDLE;
        };
    }
    
    /**
     * 头部追踪控制器（可选）
     * 
     * 让学生的头部跟随玩家
     */
    public static AnimationController<StudentEntity> createHeadTrackingController(StudentEntity entity) {
        return new AnimationController<>(entity, "head_tracking", 0, state -> {
            // TODO: 实现头部追踪逻辑
            // 参考 GeckoLib 的 lookAt 功能
            return PlayState.CONTINUE;
        });
    }
}
