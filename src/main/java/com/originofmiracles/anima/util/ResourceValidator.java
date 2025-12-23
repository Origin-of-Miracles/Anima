package com.originofmiracles.anima.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.originofmiracles.anima.Anima;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 资源验证工具类
 * 
 * 用于检查游戏资源（模型、纹理、动画等）是否存在，
 * 提供缓存机制以避免重复 I/O 操作。
 * 
 * 核心职责：
 * - 验证 GeckoLib 模型资源（geo.json, png, animation.json）
 * - 验证纹理文件是否存在
 * - 提供默认回退资源路径
 * - 缓存验证结果
 */
public class ResourceValidator {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * 资源验证结果缓存
     * Key: ResourceLocation 的完整路径字符串
     * Value: 资源是否存在
     */
    private static final Map<String, Boolean> validationCache = new HashMap<>();
    
    /**
     * 默认回退纹理（Steve 皮肤）
     */
    public static final ResourceLocation FALLBACK_TEXTURE = 
            new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");
    
    /**
     * 默认回退模型（Arona）
     */
    public static final ResourceLocation FALLBACK_MODEL = 
            new ResourceLocation(Anima.MOD_ID, "geo/students/arona.geo.json");
    
    /**
     * 默认回退动画（Arona）
     */
    public static final ResourceLocation FALLBACK_ANIMATION = 
            new ResourceLocation(Anima.MOD_ID, "animations/students/arona.animation.json");
    
    /**
     * 通用学生模型（兜底方案）
     */
    public static final ResourceLocation GENERIC_MODEL = 
            new ResourceLocation(Anima.MOD_ID, "geo/students/generic.geo.json");
    
    /**
     * 通用学生纹理（兜底方案）
     */
    public static final ResourceLocation GENERIC_TEXTURE = 
            new ResourceLocation(Anima.MOD_ID, "textures/entity/student/generic.png");
    
    /**
     * 检查资源是否存在（带缓存）
     * 
     * @param location 资源位置
     * @return 资源是否存在
     */
    @OnlyIn(Dist.CLIENT)
    public static boolean resourceExists(ResourceLocation location) {
        if (location == null) {
            return false;
        }
        
        String cacheKey = location.toString();
        
        // 检查缓存
        if (validationCache.containsKey(cacheKey)) {
            return validationCache.get(cacheKey);
        }
        
        // 实际验证
        boolean exists = checkResourceExists(location);
        validationCache.put(cacheKey, exists);
        
        return exists;
    }
    
    /**
     * 实际检查资源是否存在（不使用缓存）
     */
    @OnlyIn(Dist.CLIENT)
    private static boolean checkResourceExists(ResourceLocation location) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null || minecraft.getResourceManager() == null) {
                // 资源管理器未初始化（可能在服务端或启动早期）
                return false;
            }
            
            // 尝试获取资源
            Resource resource = minecraft.getResourceManager()
                    .getResourceOrThrow(location);
            
            return resource != null;
            
        } catch (Exception e) {
            // 资源不存在或读取失败
            return false;
        }
    }
    
    /**
     * 验证 GeckoLib 模型三件套（模型、纹理、动画）
     * 
     * @param studentId 学生 ID
     * @return 验证结果对象
     */
    @OnlyIn(Dist.CLIENT)
    public static ModelValidationResult validateGeckoLibModel(String studentId) {
        if (studentId == null || studentId.isEmpty()) {
            return ModelValidationResult.invalid("学生 ID 为空");
        }
        
        String idLower = studentId.toLowerCase();
        
        // 构建资源路径
        ResourceLocation modelLoc = new ResourceLocation(
                Anima.MOD_ID, 
                "geo/students/" + idLower + ".geo.json"
        );
        
        ResourceLocation textureLoc = new ResourceLocation(
                Anima.MOD_ID, 
                "textures/entity/student/" + idLower + ".png"
        );
        
        ResourceLocation animationLoc = new ResourceLocation(
                Anima.MOD_ID, 
                "animations/students/" + idLower + ".animation.json"
        );
        
        // 验证各个文件
        boolean modelExists = resourceExists(modelLoc);
        boolean textureExists = resourceExists(textureLoc);
        boolean animationExists = resourceExists(animationLoc);
        
        return new ModelValidationResult(
                studentId,
                modelExists,
                textureExists,
                animationExists,
                modelLoc,
                textureLoc,
                animationLoc
        );
    }
    
    /**
     * 获取可用的模型资源（带回退链）
     * 
     * 回退顺序：学生专属 → 通用模型 → Arona 模型
     */
    @OnlyIn(Dist.CLIENT)
    public static ResourceLocation getAvailableModel(String studentId) {
        // 1. 尝试学生专属模型
        ResourceLocation custom = new ResourceLocation(
                Anima.MOD_ID, 
                "geo/students/" + studentId.toLowerCase() + ".geo.json"
        );
        
        if (resourceExists(custom)) {
            return custom;
        }
        
        LOGGER.debug("找不到学生 {} 的专属模型，尝试通用模型", studentId);
        
        // 2. 尝试通用模型
        if (resourceExists(GENERIC_MODEL)) {
            return GENERIC_MODEL;
        }
        
        LOGGER.warn("找不到通用模型，使用 Arona 作为最终回退");
        
        // 3. 最终回退到 Arona
        return FALLBACK_MODEL;
    }
    
    /**
     * 获取可用的纹理资源（带回退链）
     * 
     * 回退顺序：学生专属 → 通用纹理 → Steve 皮肤
     */
    @OnlyIn(Dist.CLIENT)
    public static ResourceLocation getAvailableTexture(String studentId) {
        // 1. 尝试学生专属纹理
        ResourceLocation custom = new ResourceLocation(
                Anima.MOD_ID, 
                "textures/entity/student/" + studentId.toLowerCase() + ".png"
        );
        
        if (resourceExists(custom)) {
            return custom;
        }
        
        LOGGER.debug("找不到学生 {} 的专属纹理，尝试通用纹理", studentId);
        
        // 2. 尝试通用纹理
        if (resourceExists(GENERIC_TEXTURE)) {
            return GENERIC_TEXTURE;
        }
        
        LOGGER.debug("找不到通用纹理，使用默认材质");
        
        // 3. 最终回退到默认材质
        return FALLBACK_TEXTURE;
    }
    
    /**
     * 获取可用的动画资源（带回退链）
     */
    @OnlyIn(Dist.CLIENT)
    public static ResourceLocation getAvailableAnimation(String studentId) {
        ResourceLocation custom = new ResourceLocation(
                Anima.MOD_ID, 
                "animations/students/" + studentId.toLowerCase() + ".animation.json"
        );
        
        if (resourceExists(custom)) {
            return custom;
        }
        
        LOGGER.debug("找不到学生 {} 的专属动画，使用默认动画", studentId);
        return FALLBACK_ANIMATION;
    }
    
    /**
     * 清除验证缓存（用于资源重载时）
     */
    public static void clearCache() {
        validationCache.clear();
        LOGGER.debug("已清除资源验证缓存");
    }
    
    /**
     * 获取缓存统计信息
     */
    public static String getCacheStats() {
        return String.format("资源验证缓存: %d 项", validationCache.size());
    }
    
    /**
     * 模型验证结果
     */
    public static class ModelValidationResult {
        private final String studentId;
        private final boolean modelExists;
        private final boolean textureExists;
        private final boolean animationExists;
        private final ResourceLocation modelLocation;
        private final ResourceLocation textureLocation;
        private final ResourceLocation animationLocation;
        private final String errorMessage;
        
        public ModelValidationResult(String studentId, boolean modelExists, 
                                     boolean textureExists, boolean animationExists,
                                     ResourceLocation modelLoc, ResourceLocation textureLoc,
                                     ResourceLocation animationLoc) {
            this.studentId = studentId;
            this.modelExists = modelExists;
            this.textureExists = textureExists;
            this.animationExists = animationExists;
            this.modelLocation = modelLoc;
            this.textureLocation = textureLoc;
            this.animationLocation = animationLoc;
            this.errorMessage = null;
        }
        
        private ModelValidationResult(String errorMessage) {
            this.studentId = null;
            this.modelExists = false;
            this.textureExists = false;
            this.animationExists = false;
            this.modelLocation = null;
            this.textureLocation = null;
            this.animationLocation = null;
            this.errorMessage = errorMessage;
        }
        
        public static ModelValidationResult invalid(String error) {
            return new ModelValidationResult(error);
        }
        
        public boolean isValid() {
            return errorMessage == null;
        }
        
        public boolean isComplete() {
            return modelExists && textureExists && animationExists;
        }
        
        public boolean hasModel() {
            return modelExists;
        }
        
        public boolean hasTexture() {
            return textureExists;
        }
        
        public boolean hasAnimation() {
            return animationExists;
        }
        
        public String getStudentId() {
            return studentId;
        }
        
        public ResourceLocation getModelLocation() {
            return modelLocation;
        }
        
        public ResourceLocation getTextureLocation() {
            return textureLocation;
        }
        
        public ResourceLocation getAnimationLocation() {
            return animationLocation;
        }
        
        @Nullable
        public String getErrorMessage() {
            return errorMessage;
        }
        
        /**
         * 获取缺失资源的描述
         */
        public String getMissingResourcesDescription() {
            if (!isValid()) {
                return errorMessage;
            }
            
            if (isComplete()) {
                return "资源完整";
            }
            
            StringBuilder sb = new StringBuilder("缺失: ");
            if (!modelExists) sb.append("模型 ");
            if (!textureExists) sb.append("纹理 ");
            if (!animationExists) sb.append("动画 ");
            
            return sb.toString().trim();
        }
        
        @Override
        public String toString() {
            return String.format("ModelValidation[%s: 模型=%s, 纹理=%s, 动画=%s]",
                    studentId, modelExists, textureExists, animationExists);
        }
    }
}
