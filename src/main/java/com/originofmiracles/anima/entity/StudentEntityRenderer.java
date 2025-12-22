package com.originofmiracles.anima.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.originofmiracles.anima.Anima;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 学生实体渲染器
 * 
 * 负责在客户端渲染学生实体。
 * 默认使用玩家模型，支持 YSM 模型覆盖。
 * 
 * TODO: 集成 YSM API 实现自定义模型渲染
 */
@OnlyIn(Dist.CLIENT)
public class StudentEntityRenderer extends MobRenderer<StudentEntity, PlayerModel<StudentEntity>> {
    
    /**
     * 默认材质（Steve 皮肤）
     */
    private static final ResourceLocation DEFAULT_TEXTURE = 
            new ResourceLocation("textures/entity/steve.png");
    
    /**
     * 自定义材质目录
     */
    private static final String TEXTURE_PATH = "textures/entity/student/";
    
    public StudentEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }
    
    @Override
    public ResourceLocation getTextureLocation(StudentEntity entity) {
        String studentId = entity.getStudentId();
        
        // 尝试加载学生专属材质
        if (!studentId.isEmpty()) {
            ResourceLocation customTexture = new ResourceLocation(
                    Anima.MOD_ID, 
                    TEXTURE_PATH + studentId.toLowerCase() + ".png"
            );
            // TODO: 检查材质是否存在
            // 目前先返回默认材质
        }
        
        return DEFAULT_TEXTURE;
    }
    
    @Override
    public void render(StudentEntity entity, float entityYaw, float partialTicks, 
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // 检查是否有 YSM 模型
        String modelId = entity.getModelId();
        if (!modelId.isEmpty()) {
            // TODO: 使用 YSM API 渲染自定义模型
            // YSMCompat.renderModel(entity, modelId, poseStack, buffer, packedLight);
        }
        
        // 默认渲染
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
    
    @Override
    protected void scale(StudentEntity entity, PoseStack poseStack, float partialTicks) {
        // 可以根据学生设置不同的缩放
        float scale = 0.9375f; // 略小于玩家
        poseStack.scale(scale, scale, scale);
    }
    
    @Override
    protected void renderNameTag(StudentEntity entity, Component displayName, 
                                  PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // 渲染名称标签
        // 可以添加情绪图标等
        super.renderNameTag(entity, displayName, poseStack, buffer, packedLight);
        
        // TODO: 在名称旁边显示情绪图标
    }
}
