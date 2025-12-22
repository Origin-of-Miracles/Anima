package com.originofmiracles.anima.compat;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.originofmiracles.anima.entity.StudentEntity;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;

/**
 * YSM（Yes Steve Model）兼容层
 * 
 * 为学生实体提供 YSM 模型控制支持。
 * 
 * ⚠️ 平台限制：
 * - Windows 10/11: ✅ 支持
 * - Linux glibc 2.31+: ✅ 支持
 * - macOS: ❌ 不支持（YSM 2.x 使用 C++ 原生库）
 * 
 * 模型加载说明：
 * - YSM 模型需要放在客户端的 config/yes_steve_model/custom/ 目录下
 * - 或使用资源包方式加载
 * - 服务端只需要同步模型 ID，渲染完全在客户端进行
 * 
 * @see <a href="https://ysm.cfpa.team/">YSM 文档</a>
 */
public class YSMCompat {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * YSM 的 Mod ID
     * 注意：YSM 2.x 版本使用 "yes_steve_model" 而非 "ysm"
     */
    private static final String YSM_MOD_ID = "yes_steve_model";
    
    private static Boolean ysmLoaded = null;
    
    /**
     * 检查 YSM 是否已加载
     */
    public static boolean isYSMLoaded() {
        if (ysmLoaded == null) {
            ysmLoaded = ModList.get().isLoaded(YSM_MOD_ID);
            if (ysmLoaded) {
                LOGGER.info("检测到 YSM - 已启用学生实体自定义模型支持");
            } else {
                LOGGER.info("未找到 YSM - 学生实体将使用默认玩家模型");
            }
        }
        return ysmLoaded;
    }
    
    /**
     * 为学生实体设置 YSM 模型
     * 
     * @param entity 学生实体
     * @param modelId 模型 ID（格式: "namespace:model_name" 或 "model_name"）
     */
    public static void setEntityModel(StudentEntity entity, String modelId) {
        if (!isYSMLoaded()) return;
        if (entity.level().isClientSide) return;
        
        ServerLevel level = (ServerLevel) entity.level();
        MinecraftServer server = level.getServer();
        
        // YSM 2.x 命令格式: /ysm model set <targets> <model_id> [extra_animation_model_id] [force]
        // 对于实体，使用 UUID 作为目标
        String command = String.format(
            "ysm model set %s %s true",
            entity.getUUID().toString(),
            modelId
        );
        
        executeCommand(server, level, command);
        LOGGER.debug("为学生实体 {} 设置 YSM 模型: {}", entity.getStudentId(), modelId);
    }
    
    /**
     * 为学生实体设置 YSM 模型和纹理
     * 
     * @param entity 学生实体
     * @param modelId 模型 ID
     * @param textureId 纹理 ID
     */
    public static void setEntityModelAndTexture(StudentEntity entity, String modelId, String textureId) {
        if (!isYSMLoaded()) return;
        if (entity.level().isClientSide) return;
        
        ServerLevel level = (ServerLevel) entity.level();
        MinecraftServer server = level.getServer();
        
        // 设置模型
        String command = String.format(
            "ysm model set %s %s %s true",
            entity.getUUID().toString(),
            modelId,
            textureId
        );
        
        executeCommand(server, level, command);
        LOGGER.debug("为学生实体 {} 设置 YSM 模型: {} 纹理: {}", 
            entity.getStudentId(), modelId, textureId);
    }
    
    /**
     * 为学生实体播放动画
     * 
     * @param entity 学生实体
     * @param animationName 动画名称
     */
    public static void playAnimation(StudentEntity entity, String animationName) {
        if (!isYSMLoaded()) return;
        if (entity.level().isClientSide) return;
        
        ServerLevel level = (ServerLevel) entity.level();
        MinecraftServer server = level.getServer();
        
        // YSM 2.x 命令格式: /ysm play <targets> <animation_name>
        String command = String.format(
            "ysm play %s %s",
            entity.getUUID().toString(),
            animationName
        );
        
        executeCommand(server, level, command);
        LOGGER.debug("为学生实体 {} 播放动画: {}", entity.getStudentId(), animationName);
    }
    
    /**
     * 停止学生实体的强制动画
     * 
     * @param entity 学生实体
     */
    public static void stopAnimation(StudentEntity entity) {
        if (!isYSMLoaded()) return;
        if (entity.level().isClientSide) return;
        
        ServerLevel level = (ServerLevel) entity.level();
        MinecraftServer server = level.getServer();
        
        String command = String.format(
            "ysm play %s stop",
            entity.getUUID().toString()
        );
        
        executeCommand(server, level, command);
    }
    
    /**
     * 执行 Molang 表达式（用于变量操作）
     * 
     * @param entity 学生实体
     * @param expression Molang 表达式（如 "v.expression='happy'"）
     */
    public static void executeMolang(StudentEntity entity, String expression) {
        if (!isYSMLoaded()) return;
        if (entity.level().isClientSide) return;
        
        ServerLevel level = (ServerLevel) entity.level();
        MinecraftServer server = level.getServer();
        
        // YSM 2.x 命令格式: /ysm molang execute <targets> <expression>
        String command = String.format(
            "ysm molang execute %s %s",
            entity.getUUID().toString(),
            expression
        );
        
        executeCommand(server, level, command);
        LOGGER.debug("为学生实体 {} 执行 Molang: {}", entity.getStudentId(), expression);
    }
    
    /**
     * 设置表情（通过 Molang 变量）
     * 
     * @param entity 学生实体
     * @param expressionId 表情 ID
     */
    public static void setExpression(StudentEntity entity, String expressionId) {
        executeMolang(entity, "v.expression='" + expressionId + "'");
    }
    
    /**
     * 重载所有 YSM 模型
     * 
     * @param server Minecraft 服务端
     */
    public static void reloadModels(MinecraftServer server) {
        if (!isYSMLoaded()) return;
        
        ServerLevel level = server.overworld();
        executeCommand(server, level, "ysm model reload");
        LOGGER.info("已请求 YSM 模型重载");
    }
    
    /**
     * 执行 YSM 命令
     */
    private static void executeCommand(MinecraftServer server, ServerLevel level, String command) {
        if (server == null) {
            LOGGER.warn("无法执行 YSM 命令: 服务器未就绪");
            return;
        }
        
        try {
            // 创建具有最高权限的命令源
            CommandSourceStack source = server.createCommandSourceStack()
                .withPermission(4)
                .withSuppressedOutput();
            
            server.getCommands().performPrefixedCommand(source, command);
            LOGGER.debug("执行 YSM 命令: {}", command);
        } catch (Exception e) {
            LOGGER.error("执行 YSM 命令失败: {}", command, e);
        }
    }
    
    /**
     * 检查模型 ID 格式是否有效
     * 
     * @param modelId 模型 ID
     * @return 是否有效
     */
    public static boolean isValidModelId(String modelId) {
        if (modelId == null || modelId.isEmpty()) {
            return false;
        }
        // 支持格式: "model_name" 或 "namespace:model_name"
        return modelId.matches("^[a-z0-9_]+(:?[a-z0-9_/]+)?$");
    }
    
    /**
     * 标准化模型 ID（添加默认命名空间）
     * 
     * @param modelId 原始模型 ID
     * @return 标准化后的模型 ID
     */
    public static String normalizeModelId(String modelId) {
        if (modelId == null || modelId.isEmpty()) {
            return "";
        }
        // 如果没有命名空间，添加 "anima:" 前缀
        if (!modelId.contains(":")) {
            return "anima:" + modelId;
        }
        return modelId;
    }
}
