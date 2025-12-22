package com.originofmiracles.anima.entity;

import java.util.UUID;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.originofmiracles.anima.Anima;
import com.originofmiracles.anima.agent.StudentAgent;
import com.originofmiracles.anima.mood.MoodState;
import com.originofmiracles.anima.mood.MoodSystem;
import com.originofmiracles.anima.persona.Persona;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 学生实体
 * 
 * Anima AI 角色在游戏世界中的实体表现。
 * 
 * 特性：
 * - 绑定 StudentAgent 进行 AI 对话
 * - 支持 YSM 模型显示
 * - 具有情绪系统
 * - 可与玩家互动
 */
public class StudentEntity extends PathfinderMob {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // ==================== 同步数据 ====================
    
    /**
     * 学生 ID（用于关联 Persona 和 Agent）
     */
    private static final EntityDataAccessor<String> DATA_STUDENT_ID = 
            SynchedEntityData.defineId(StudentEntity.class, EntityDataSerializers.STRING);
    
    /**
     * 当前情绪状态
     */
    private static final EntityDataAccessor<String> DATA_MOOD = 
            SynchedEntityData.defineId(StudentEntity.class, EntityDataSerializers.STRING);
    
    /**
     * YSM 模型 ID
     */
    private static final EntityDataAccessor<String> DATA_MODEL_ID = 
            SynchedEntityData.defineId(StudentEntity.class, EntityDataSerializers.STRING);
    
    /**
     * 当前播放的动画
     */
    private static final EntityDataAccessor<String> DATA_ANIMATION = 
            SynchedEntityData.defineId(StudentEntity.class, EntityDataSerializers.STRING);
    
    // ==================== 服务端数据 ====================
    
    /**
     * 关联的 StudentAgent（仅服务端）
     */
    @Nullable
    private StudentAgent agent;
    
    /**
     * 情绪系统（仅服务端）
     */
    @Nullable
    private MoodSystem moodSystem;
    
    /**
     * 所属玩家 UUID（召唤者）
     */
    @Nullable
    private UUID ownerUUID;
    
    // ==================== 构造函数 ====================
    
    public StudentEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
    }
    
    // ==================== 属性定义 ====================
    
    /**
     * 创建实体属性
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ATTACK_DAMAGE, 1.0);
    }
    
    // ==================== 初始化 ====================
    
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_STUDENT_ID, "");
        this.entityData.define(DATA_MOOD, MoodState.NEUTRAL.name());
        this.entityData.define(DATA_MODEL_ID, "");
        this.entityData.define(DATA_ANIMATION, "idle");
    }
    
    @Override
    protected void registerGoals() {
        // 基础 AI 目标
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.6));
        
        // TODO: 添加自定义 AI 目标
        // - FollowOwnerGoal：跟随主人
        // - InteractWithPlayerGoal：与玩家互动
        // - PerformTaskGoal：执行任务
    }
    
    // ==================== 学生绑定 ====================
    
    /**
     * 绑定学生 ID
     * 
     * @param studentId 学生 ID（对应 Persona ID）
     */
    public void bindStudent(String studentId) {
        this.entityData.set(DATA_STUDENT_ID, studentId);
        
        // 服务端初始化 Agent
        if (!this.level().isClientSide) {
            initializeAgent(studentId);
        }
        
        // 设置显示名称和 YSM 模型
        Persona persona = Anima.getInstance().getPersonaManager().getPersona(studentId);
        if (persona != null) {
            this.setCustomName(Component.literal(persona.getName()));
            this.setCustomNameVisible(true);
            
            // 应用 YSM 模型（如果配置了）
            applyYSMModel(persona);
        }
        
        LOGGER.info("学生实体已绑定: {} -> {}", this.getId(), studentId);
    }
    
    /**
     * 应用 YSM 模型
     * 
     * @param persona 角色配置
     */
    private void applyYSMModel(Persona persona) {
        if (this.level().isClientSide) return;
        
        // 检查是否配置了 YSM 模型
        if (persona.hasYsmModel()) {
            String modelId = persona.getYsmModelId();
            String textureId = persona.getYsmTextureId();
            
            // 保存模型 ID 到同步数据（供客户端使用）
            this.entityData.set(DATA_MODEL_ID, modelId);
            
            // 通过 YSM 命令 API 应用模型
            if (com.originofmiracles.anima.compat.YSMCompat.isYSMLoaded()) {
                if (textureId != null && !textureId.isEmpty()) {
                    com.originofmiracles.anima.compat.YSMCompat.setEntityModelAndTexture(this, modelId, textureId);
                } else {
                    com.originofmiracles.anima.compat.YSMCompat.setEntityModel(this, modelId);
                }
                LOGGER.info("为学生 {} 应用 YSM 模型: {}", persona.getId(), modelId);
            } else {
                LOGGER.debug("YSM 未加载，跳过模型应用: {}", persona.getId());
            }
        }
    }
    
    /**
     * 初始化 AI Agent
     */
    private void initializeAgent(String studentId) {
        if (this.level().isClientSide) return;
        
        // 从 AgentManager 获取或创建 Agent
        this.agent = Anima.getInstance().getAgentManager().getOrCreateAgent(studentId);
        
        // 初始化情绪系统
        this.moodSystem = new MoodSystem(studentId);
        this.moodSystem.addListener(event -> {
            // 同步情绪到客户端
            this.entityData.set(DATA_MOOD, event.getNewState().name());
            // 触发动画变化
            updateMoodAnimation(event.getNewState());
        });
    }
    
    /**
     * 根据情绪更新动画
     */
    private void updateMoodAnimation(MoodState mood) {
        String animation = switch (mood) {
            case HAPPY -> "happy_idle";
            case EXCITED -> "happy_idle";
            case SAD -> "sad_idle";
            case ANGRY -> "angry_idle";
            case SURPRISED -> "surprised_idle";
            case CONFUSED -> "confused_idle";
            case THINKING -> "thinking_idle";
            case ANTICIPATING -> "anticipating_idle";
            default -> "idle";
        };
        setAnimation(animation);
    }
    
    // ==================== Getter/Setter ====================
    
    public String getStudentId() {
        return this.entityData.get(DATA_STUDENT_ID);
    }
    
    public MoodState getMood() {
        try {
            return MoodState.valueOf(this.entityData.get(DATA_MOOD));
        } catch (IllegalArgumentException e) {
            return MoodState.NEUTRAL;
        }
    }
    
    public void setMood(MoodState mood) {
        this.entityData.set(DATA_MOOD, mood.name());
        if (this.moodSystem != null) {
            this.moodSystem.setState(mood, 0.5f);
        }
    }
    
    public String getModelId() {
        return this.entityData.get(DATA_MODEL_ID);
    }
    
    public void setModelId(String modelId) {
        this.entityData.set(DATA_MODEL_ID, modelId);
    }
    
    public String getAnimation() {
        return this.entityData.get(DATA_ANIMATION);
    }
    
    public void setAnimation(String animation) {
        this.entityData.set(DATA_ANIMATION, animation);
    }
    
    @Nullable
    public StudentAgent getAgent() {
        return this.agent;
    }
    
    @Nullable
    public MoodSystem getMoodSystem() {
        return this.moodSystem;
    }
    
    @Nullable
    public UUID getOwnerUUID() {
        return this.ownerUUID;
    }
    
    public void setOwnerUUID(@Nullable UUID uuid) {
        this.ownerUUID = uuid;
    }
    
    @Nullable
    public Player getOwner() {
        if (this.ownerUUID == null) return null;
        if (this.level() instanceof ServerLevel serverLevel) {
            return serverLevel.getServer().getPlayerList().getPlayer(this.ownerUUID);
        }
        return null;
    }
    
    // ==================== 交互 ====================
    
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && hand == InteractionHand.MAIN_HAND) {
            String studentId = getStudentId();
            if (!studentId.isEmpty()) {
                // 打开 MomoTalk 对话界面
                // TODO: 通过 Bridge 通知前端打开对话
                LOGGER.info("玩家 {} 与学生 {} 互动", player.getName().getString(), studentId);
                
                // 播放问候动画
                setAnimation("greeting");
                
                // 触发情绪
                if (moodSystem != null) {
                    moodSystem.applyTrigger(com.originofmiracles.anima.mood.MoodTrigger.GREETED);
                }
                
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }
    
    // ==================== 伤害处理 ====================
    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 学生实体不受伤害（或减少伤害）
        if (source.getEntity() instanceof Player) {
            // 被玩家攻击
            if (moodSystem != null) {
                moodSystem.applyTrigger(com.originofmiracles.anima.mood.MoodTrigger.ATTACKED);
            }
            setAnimation("hurt");
            return false; // 不受伤害
        }
        return super.hurt(source, amount * 0.1f); // 大幅减少伤害
    }
    
    // ==================== Tick ====================
    
    @Override
    public void tick() {
        super.tick();
        
        // 服务端更新
        if (!this.level().isClientSide) {
            // 更新情绪系统
            if (moodSystem != null) {
                moodSystem.update();
            }
        }
    }
    
    // ==================== 数据持久化 ====================
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("StudentId", getStudentId());
        tag.putString("Mood", this.entityData.get(DATA_MOOD));
        tag.putString("ModelId", getModelId());
        if (ownerUUID != null) {
            tag.putUUID("Owner", ownerUUID);
        }
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        
        String studentId = tag.getString("StudentId");
        if (!studentId.isEmpty()) {
            bindStudent(studentId);
        }
        
        if (tag.contains("Mood")) {
            this.entityData.set(DATA_MOOD, tag.getString("Mood"));
        }
        if (tag.contains("ModelId")) {
            this.entityData.set(DATA_MODEL_ID, tag.getString("ModelId"));
        }
        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
        }
    }
    
    // ==================== 其他 ====================
    
    @Override
    public boolean removeWhenFarAway(double distance) {
        // 学生实体不会因距离而被移除
        return false;
    }
    
    @Override
    public boolean canBeLeashed(Player player) {
        // 学生不能被拴绳牵引
        return false;
    }
    
    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }
}
