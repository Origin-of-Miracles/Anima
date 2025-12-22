package com.originofmiracles.anima.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.originofmiracles.anima.Anima;
import com.originofmiracles.anima.compat.YSMCompat;
import com.originofmiracles.anima.entity.StudentEntity;
import com.originofmiracles.anima.entity.StudentSpawnManager;
import com.originofmiracles.anima.persona.Persona;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Anima 命令注册
 * 
 * 提供的命令：
 * - /anima summon <studentId> - 召唤学生实体
 * - /anima dismiss <studentId> - 解散学生实体
 * - /anima dismissall - 解散所有学生实体
 * - /anima list - 列出所有可用学生
 * - /anima model <studentId> <modelId> [textureId] - 设置 YSM 模型
 * - /anima anim <studentId> <animationId> - 播放 YSM 动画
 */
public class AnimaCommands {
    
    /**
     * 学生 ID 自动补全
     */
    private static final SuggestionProvider<CommandSourceStack> STUDENT_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(
                Anima.getInstance().getPersonaManager().getAllPersonas().keySet(),
                builder
        );
    };
    
    /**
     * 已召唤学生 ID 自动补全
     */
    private static final SuggestionProvider<CommandSourceStack> SPAWNED_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(
                StudentSpawnManager.getInstance().getSpawnedStudentIds(),
                builder
        );
    };
    
    /**
     * 注册命令
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("anima")
                .requires(source -> source.hasPermission(2)) // 需要 OP 权限
                
                // /anima summon <studentId>
                .then(Commands.literal("summon")
                        .then(Commands.argument("studentId", StringArgumentType.word())
                                .suggests(STUDENT_SUGGESTIONS)
                                .executes(AnimaCommands::summonStudent)))
                
                // /anima dismiss <studentId>
                .then(Commands.literal("dismiss")
                        .then(Commands.argument("studentId", StringArgumentType.word())
                                .suggests(SPAWNED_SUGGESTIONS)
                                .executes(AnimaCommands::dismissStudent)))
                
                // /anima dismissall
                .then(Commands.literal("dismissall")
                        .executes(AnimaCommands::dismissAllStudents))
                
                // /anima list
                .then(Commands.literal("list")
                        .executes(AnimaCommands::listStudents))
                
                // /anima summonall
                .then(Commands.literal("summonall")
                        .executes(AnimaCommands::summonAllStudents))
                
                // /anima model <studentId> <modelId> [textureId]
                .then(Commands.literal("model")
                        .then(Commands.argument("studentId", StringArgumentType.word())
                                .suggests(SPAWNED_SUGGESTIONS)
                                .then(Commands.argument("modelId", StringArgumentType.string())
                                        .executes(AnimaCommands::setModel)
                                        .then(Commands.argument("textureId", StringArgumentType.string())
                                                .executes(AnimaCommands::setModelWithTexture)))))
                
                // /anima anim <studentId> <animationId>
                .then(Commands.literal("anim")
                        .then(Commands.argument("studentId", StringArgumentType.word())
                                .suggests(SPAWNED_SUGGESTIONS)
                                .then(Commands.argument("animationId", StringArgumentType.word())
                                        .executes(AnimaCommands::playAnimation))))
                
                // /anima stopanim <studentId>
                .then(Commands.literal("stopanim")
                        .then(Commands.argument("studentId", StringArgumentType.word())
                                .suggests(SPAWNED_SUGGESTIONS)
                                .executes(AnimaCommands::stopAnimation)))
        );
    }
    
    /**
     * 召唤学生
     */
    private static int summonStudent(CommandContext<CommandSourceStack> context) {
        String studentId = StringArgumentType.getString(context, "studentId");
        CommandSourceStack source = context.getSource();
        
        // 检查人格是否存在
        Persona persona = Anima.getInstance().getPersonaManager().getPersona(studentId);
        if (persona == null) {
            source.sendFailure(Component.literal("未找到学生: " + studentId));
            return 0;
        }
        
        // 获取玩家
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }
        
        // 生成实体
        StudentEntity entity = StudentSpawnManager.getInstance().spawnNearPlayer(player, studentId);
        if (entity != null) {
            source.sendSuccess(() -> Component.literal("已召唤学生: " + persona.getName()), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("召唤失败"));
            return 0;
        }
    }
    
    /**
     * 解散学生
     */
    private static int dismissStudent(CommandContext<CommandSourceStack> context) {
        String studentId = StringArgumentType.getString(context, "studentId");
        CommandSourceStack source = context.getSource();
        
        if (!StudentSpawnManager.getInstance().isSpawned(studentId)) {
            source.sendFailure(Component.literal("学生未召唤: " + studentId));
            return 0;
        }
        
        StudentSpawnManager.getInstance().despawnStudent(studentId);
        source.sendSuccess(() -> Component.literal("已解散学生: " + studentId), true);
        return 1;
    }
    
    /**
     * 解散所有学生
     */
    private static int dismissAllStudents(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            StudentSpawnManager.getInstance().despawnAllStudents(player.serverLevel());
            source.sendSuccess(() -> Component.literal("已解散所有学生"), true);
            return 1;
        }
        return 0;
    }
    
    /**
     * 列出所有学生
     */
    private static int listStudents(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        var personas = Anima.getInstance().getPersonaManager().getAllPersonas();
        
        source.sendSuccess(() -> Component.literal("=== 可用学生 (" + personas.size() + ") ==="), false);
        
        for (Persona persona : personas.values()) {
            boolean spawned = StudentSpawnManager.getInstance().isSpawned(persona.getId());
            String status = spawned ? " [已召唤]" : "";
            String ysmInfo = persona.hasYsmModel() ? " [YSM:" + persona.getYsmModelId() + "]" : "";
            source.sendSuccess(() -> Component.literal("- " + persona.getName() + " (" + persona.getId() + ")" + status + ysmInfo), false);
        }
        
        return 1;
    }
    
    /**
     * 召唤所有学生
     */
    private static int summonAllStudents(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }
        
        StudentSpawnManager.getInstance().summonAllStudents(player);
        source.sendSuccess(() -> Component.literal("已召唤所有学生"), true);
        return 1;
    }
    
    // ==================== YSM 模型命令 ====================
    
    /**
     * 设置学生 YSM 模型
     */
    private static int setModel(CommandContext<CommandSourceStack> context) {
        String studentId = StringArgumentType.getString(context, "studentId");
        String modelId = StringArgumentType.getString(context, "modelId");
        CommandSourceStack source = context.getSource();
        
        if (!YSMCompat.isYSMLoaded()) {
            source.sendFailure(Component.literal("YSM 未安装或未加载"));
            return 0;
        }
        
        StudentEntity entity = StudentSpawnManager.getInstance().getEntity(studentId);
        if (entity == null) {
            source.sendFailure(Component.literal("学生未召唤: " + studentId));
            return 0;
        }
        
        YSMCompat.setEntityModel(entity, modelId);
        source.sendSuccess(() -> Component.literal("已设置 " + studentId + " 的模型为: " + modelId), true);
        return 1;
    }
    
    /**
     * 设置学生 YSM 模型和纹理
     */
    private static int setModelWithTexture(CommandContext<CommandSourceStack> context) {
        String studentId = StringArgumentType.getString(context, "studentId");
        String modelId = StringArgumentType.getString(context, "modelId");
        String textureId = StringArgumentType.getString(context, "textureId");
        CommandSourceStack source = context.getSource();
        
        if (!YSMCompat.isYSMLoaded()) {
            source.sendFailure(Component.literal("YSM 未安装或未加载"));
            return 0;
        }
        
        StudentEntity entity = StudentSpawnManager.getInstance().getEntity(studentId);
        if (entity == null) {
            source.sendFailure(Component.literal("学生未召唤: " + studentId));
            return 0;
        }
        
        YSMCompat.setEntityModelAndTexture(entity, modelId, textureId);
        source.sendSuccess(() -> Component.literal("已设置 " + studentId + " 的模型为: " + modelId + " 纹理: " + textureId), true);
        return 1;
    }
    
    /**
     * 播放 YSM 动画
     */
    private static int playAnimation(CommandContext<CommandSourceStack> context) {
        String studentId = StringArgumentType.getString(context, "studentId");
        String animationId = StringArgumentType.getString(context, "animationId");
        CommandSourceStack source = context.getSource();
        
        if (!YSMCompat.isYSMLoaded()) {
            source.sendFailure(Component.literal("YSM 未安装或未加载"));
            return 0;
        }
        
        StudentEntity entity = StudentSpawnManager.getInstance().getEntity(studentId);
        if (entity == null) {
            source.sendFailure(Component.literal("学生未召唤: " + studentId));
            return 0;
        }
        
        YSMCompat.playAnimation(entity, animationId);
        source.sendSuccess(() -> Component.literal("正在播放 " + studentId + " 的动画: " + animationId), true);
        return 1;
    }
    
    /**
     * 停止 YSM 动画
     */
    private static int stopAnimation(CommandContext<CommandSourceStack> context) {
        String studentId = StringArgumentType.getString(context, "studentId");
        CommandSourceStack source = context.getSource();
        
        if (!YSMCompat.isYSMLoaded()) {
            source.sendFailure(Component.literal("YSM 未安装或未加载"));
            return 0;
        }
        
        StudentEntity entity = StudentSpawnManager.getInstance().getEntity(studentId);
        if (entity == null) {
            source.sendFailure(Component.literal("学生未召唤: " + studentId));
            return 0;
        }
        
        YSMCompat.stopAnimation(entity);
        source.sendSuccess(() -> Component.literal("已停止 " + studentId + " 的动画"), true);
        return 1;
    }
}
