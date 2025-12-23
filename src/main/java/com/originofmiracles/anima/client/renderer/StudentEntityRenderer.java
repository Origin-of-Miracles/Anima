package com.originofmiracles.anima.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.originofmiracles.anima.client.model.StudentEntityModel;
import com.originofmiracles.anima.entity.StudentEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 学生实体渲染器
 * 
 * 负责渲染学生的 3D 模型和动画。
 * 
 * 特性：
 * - 支持动态模型切换
 * - 情绪相关的动画播放
 * - 可选的渲染层（发光、透明等）
 */
public class StudentEntityRenderer extends GeoEntityRenderer<StudentEntity> {
    
    public StudentEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new StudentEntityModel());
        this.shadowRadius = 0.5f; // 阴影半径
    }
    
    /**
     * 渲染前的变换处理
     * 
     * 可用于调整实体的缩放、旋转等
     */
    @Override
    protected void applyRotations(StudentEntity entity, PoseStack poseStack, float ageInTicks, 
                                   float rotationYaw, float partialTick) {
        super.applyRotations(entity, poseStack, ageInTicks, rotationYaw, partialTick);
        
        // TODO: 根据情绪调整姿态
        // 例如：悲伤时轻微低头，兴奋时轻微跳跃
    }
    
    /**
     * 自定义渲染逻辑
     */
    @Override
    public void render(StudentEntity entity, float entityYaw, float partialTick,
                      PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // 缩放调整（如果需要）
        float scale = 1.0f;
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        
        poseStack.popPose();
    }
}
