package com.originofmiracles.anima.entity;

import org.slf4j.Logger;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.originofmiracles.anima.util.ResourceValidator;

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
 * 包含资源回退机制，确保在纹理缺失时不会崩溃。
 * 
 * TODO: 集成 YSM API 实现自定义模型渲染（已经集成了GeckoLib库，这个Todo留着以防后续需要）
 */
@OnlyIn(Dist.CLIENT)
public class StudentEntityRenderer extends MobRenderer<StudentEntity, PlayerModel<StudentEntity>> {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * 默认材质（Steve 皮肤）
     * 注意：1.20.1 的正确路径是 player/wide/steve.png
     */
    private static final ResourceLocation DEFAULT_TEXTURE = 
            new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");
    

    
    public StudentEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }
    
    @Override
    public ResourceLocation getTextureLocation(StudentEntity entity) {
        if (entity == null) {
            LOGGER.warn("实体为 null，使用默认纹理");
            return DEFAULT_TEXTURE;
        }
        
        String studentId = entity.getStudentId();
        
        if (studentId == null || studentId.isEmpty()) {
            LOGGER.debug("学生 ID 为空，使用默认纹理");
            return DEFAULT_TEXTURE;
        }
        
        // 使用 ResourceValidator 的回退链
        return ResourceValidator.getAvailableTexture(studentId);
    }
    
    @Override
    public void render(StudentEntity entity, float entityYaw, float partialTicks, 
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // 检查是否有 YSM 模型
        String modelId = entity.getModelId();
        if (!modelId.isEmpty()) {
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
