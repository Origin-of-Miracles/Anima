package com.originofmiracles.anima.entity;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.originofmiracles.anima.compat.YSMCompat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;

/**
 * 学生实体驱动
 * 
 * 实现 Miracle-Bridge 的 IEntityDriver 接口，
 * 为 StudentEntity 提供高层行为控制 API。
 * 
 * 注意：此类不直接实现 IEntityDriver 接口，
 * 而是作为独立的控制器类，避免跨模组依赖问题。
 */
public class StudentEntityDriver {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private final StudentEntity entity;
    
    /**
     * 导航配置
     */
    private static final double ARRIVAL_THRESHOLD = 1.5;
    
    /**
     * 当前导航目标
     */
    @Nullable
    private BlockPos navigationTarget;
    
    public StudentEntityDriver(StudentEntity entity) {
        this.entity = entity;
    }
    
    // ==================== 动画控制 ====================
    
    /**
     * 播放命名动画
     * 
     * @param animationId 动画标识符
     */
    public void playAnimation(String animationId) {
        entity.setAnimation(animationId);
        
        // 如果安装了 YSM，通过 YSM 播放动画
        if (YSMCompat.isYSMLoaded()) {
            YSMCompat.playAnimation(entity, animationId);
        }
        
        LOGGER.debug("播放动画: {} -> {}", entity.getStudentId(), animationId);
    }
    
    /**
     * 停止当前动画
     */
    public void stopAnimation() {
        entity.setAnimation("idle");
        
        if (YSMCompat.isYSMLoaded()) {
            YSMCompat.stopAnimation(entity);
        }
    }
    
    /**
     * 设置表情
     * 
     * @param expressionId 表情标识符
     */
    public void setExpression(String expressionId) {
        // 通过情绪系统设置
        try {
            com.originofmiracles.anima.mood.MoodState mood = 
                    com.originofmiracles.anima.mood.MoodState.valueOf(expressionId.toUpperCase());
            entity.setMood(mood);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("未知表情: {}", expressionId);
        }
        
        // 如果安装了 YSM，设置表情变量
        if (YSMCompat.isYSMLoaded()) {
            YSMCompat.setExpression(entity, expressionId);
        }
    }
    
    /**
     * 执行 Molang 表达式（YSM 专用）
     * 
     * @param expression Molang 表达式
     */
    public void executeMolang(String expression) {
        if (YSMCompat.isYSMLoaded()) {
            YSMCompat.executeMolang(entity, expression);
        } else {
            LOGGER.debug("YSM 未加载，跳过 Molang: {}", expression);
        }
    }
    
    // ==================== 导航控制 ====================
    
    /**
     * 导航到目标位置
     * 
     * @param target 目标方块位置
     */
    public void navigateTo(BlockPos target) {
        this.navigationTarget = target;
        
        PathNavigation navigation = entity.getNavigation();
        navigation.moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0);
        
        // 播放行走动画
        playAnimation("walk");
        
        LOGGER.debug("导航到: {} -> {}", entity.getStudentId(), target);
    }
    
    /**
     * 停止所有行为
     */
    public void halt() {
        // 停止导航
        entity.getNavigation().stop();
        this.navigationTarget = null;
        
        // 重置动画
        playAnimation("idle");
        
        LOGGER.debug("停止行为: {}", entity.getStudentId());
    }
    
    /**
     * 让实体看向目标位置
     * 
     * @param target 目标位置
     */
    public void lookAt(BlockPos target) {
        Vec3 targetVec = new Vec3(target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5);
        entity.getLookControl().setLookAt(targetVec);
    }
    
    /**
     * 让实体看向玩家
     * 
     * @param player 目标玩家
     */
    public void lookAtPlayer(net.minecraft.world.entity.player.Player player) {
        entity.getLookControl().setLookAt(player, 30.0f, 30.0f);
    }
    
    // ==================== 状态查询 ====================
    
    /**
     * 检查是否正在导航
     */
    public boolean isNavigating() {
        return entity.getNavigation().isInProgress();
    }
    
    /**
     * 获取当前导航目标
     */
    @Nullable
    public BlockPos getNavigationTarget() {
        return navigationTarget;
    }
    
    /**
     * 获取当前位置
     */
    public BlockPos getPosition() {
        return entity.blockPosition();
    }
    
    /**
     * 检查是否到达目标
     */
    public boolean hasReachedTarget() {
        if (navigationTarget == null) return true;
        
        double distance = entity.position().distanceTo(
                new Vec3(navigationTarget.getX() + 0.5, navigationTarget.getY(), navigationTarget.getZ() + 0.5)
        );
        return distance < ARRIVAL_THRESHOLD;
    }
    
    /**
     * 检查驱动是否可用
     */
    public boolean isAvailable() {
        return entity.isAlive() && !entity.isRemoved();
    }
    
    // ==================== 高级行为 ====================
    
    /**
     * 跟随玩家
     * 
     * @param player 要跟随的玩家
     * @param minDistance 最小距离
     * @param maxDistance 最大距离
     */
    public void followPlayer(net.minecraft.world.entity.player.Player player, double minDistance, double maxDistance) {
        double distance = entity.distanceTo(player);
        
        if (distance > maxDistance) {
            // 传送到玩家附近
            BlockPos playerPos = player.blockPosition();
            entity.teleportTo(playerPos.getX(), playerPos.getY(), playerPos.getZ());
        } else if (distance > minDistance) {
            // 走向玩家
            navigateTo(player.blockPosition());
        } else {
            // 已经足够近，停下并看向玩家
            if (isNavigating()) {
                halt();
            }
            lookAtPlayer(player);
        }
    }
    
    /**
     * 执行问候行为
     */
    public void greet() {
        playAnimation("greeting");
        
        // 设置开心情绪
        entity.setMood(com.originofmiracles.anima.mood.MoodState.HAPPY);
    }
    
    /**
     * 执行坐下行为
     */
    public void sit() {
        halt();
        playAnimation("sit");
        entity.setNoGravity(false);
    }
    
    /**
     * 执行站立行为
     */
    public void stand() {
        playAnimation("stand");
        entity.setNoGravity(false);
    }
}
