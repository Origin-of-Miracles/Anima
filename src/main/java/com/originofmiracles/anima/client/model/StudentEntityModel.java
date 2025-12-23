package com.originofmiracles.anima.client.model;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.originofmiracles.anima.entity.StudentEntity;
import com.originofmiracles.anima.util.ResourceValidator;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

/**
 * 学生实体模型定义 (GeckoLib 4.8.2)
 * 
 * 使用自定义 GeoModel 实现动态资源加载和回退机制。
 * 
 * 回退链：
 * - 模型: 学生专属 → 通用模型 → Arona 模型
 * - 纹理: 学生专属 → 通用纹理 → Steve 皮肤
 * - 动画: 学生专属 → 默认动画
 * 
 * 路径结构：
 * - 模型: assets/anima/geo/students/{student_id}.geo.json
 * - 纹理: assets/anima/textures/entity/student/{student_id}.png
 * - 动画: assets/anima/animations/students/{student_id}.animation.json
 */
public class StudentEntityModel extends GeoModel<StudentEntity> {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * 获取模型资源（带回退机制）
     * 
     * GeckoLib 4.8.2 不再传递实体实例，而是传递 AnimationState
     * 我们需要从 AnimationState 中获取实体来确定学生 ID
     */
    @Override
    public ResourceLocation getModelResource(StudentEntity animatable) {
        if (animatable == null) {
            LOGGER.warn("animatable 为 null，使用默认模型");
            return ResourceValidator.FALLBACK_MODEL;
        }
        
        String studentId = animatable.getStudentId();
        
        if (studentId == null || studentId.isEmpty()) {
            LOGGER.debug("学生 ID 为空，使用默认模型");
            return ResourceValidator.FALLBACK_MODEL;
        }
        
        // 使用 ResourceValidator 的回退链
        return ResourceValidator.getAvailableModel(studentId);
    }
    
    /**
     * 获取纹理资源（带回退机制）
     */
    @Override
    public ResourceLocation getTextureResource(StudentEntity animatable) {
        if (animatable == null) {
            return ResourceValidator.FALLBACK_TEXTURE;
        }
        
        String studentId = animatable.getStudentId();
        
        if (studentId == null || studentId.isEmpty()) {
            return ResourceValidator.FALLBACK_TEXTURE;
        }
        
        return ResourceValidator.getAvailableTexture(studentId);
    }
    
    /**
     * 获取动画资源（带回退机制）
     */
    @Override
    public ResourceLocation getAnimationResource(StudentEntity animatable) {
        if (animatable == null) {
            return ResourceValidator.FALLBACK_ANIMATION;
        }
        
        String studentId = animatable.getStudentId();
        
        if (studentId == null || studentId.isEmpty()) {
            return ResourceValidator.FALLBACK_ANIMATION;
        }
        
        return ResourceValidator.getAvailableAnimation(studentId);
    }
    
    /**
     * 设置自定义动画（可选）
     * 在这里可以根据实体状态覆盖骨骼动画
     */
    @Override
    public void setCustomAnimations(StudentEntity animatable, long instanceId, 
                                     AnimationState<StudentEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        
        // TODO: 未来可以在这里添加：
        // - 根据情绪调整表情骨骼
        // - 根据装备显示/隐藏部件
        // - 根据动作状态调整姿势
    }
}
