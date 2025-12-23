package com.originofmiracles.anima.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.originofmiracles.anima.Anima;
import com.originofmiracles.anima.persona.Persona;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;

/**
 * 学生实体生成管理器
 * 
 * 负责：
 * - 生成学生实体
 * - 追踪已生成的实体
 * - 实体的召唤与解散
 */
public class StudentSpawnManager {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private static StudentSpawnManager instance;
    
    /**
     * 已生成的学生实体映射
     * Key: studentId, Value: 实体 UUID
     */
    private final Map<String, UUID> spawnedStudents = new HashMap<>();
    
    /**
     * 实体驱动映射
     * Key: 实体 UUID, Value: StudentEntityDriver
     */
    private final Map<UUID, StudentEntityDriver> entityDrivers = new HashMap<>();
    
    private StudentSpawnManager() {}
    
    public static StudentSpawnManager getInstance() {
        if (instance == null) {
            instance = new StudentSpawnManager();
        }
        return instance;
    }
    
    // ==================== 生成 ====================
    
    /**
     * 在指定位置生成学生实体
     * 
     * @param level 服务端世界
     * @param studentId 学生 ID
     * @param pos 生成位置
     * @param owner 所属玩家（可选）
     * @return 生成的实体，失败返回 null
     */
    @Nullable
    public StudentEntity spawnStudent(ServerLevel level, String studentId, BlockPos pos, @Nullable ServerPlayer owner) {
        // 检查人格是否存在
        Persona persona = Anima.getInstance().getPersonaManager().getPersona(studentId);
        if (persona == null) {
            LOGGER.warn("无法生成学生 {}: 人格配置不存在", studentId);
            return null;
        }
        
        // 检查是否已生成
        if (spawnedStudents.containsKey(studentId.toLowerCase())) {
            LOGGER.warn("学生 {} 已经存在于世界中", studentId);
            // 可以选择移除旧实体或返回 null
            despawnStudent(studentId);
        }
        
        // 创建实体
        StudentEntity entity = ModEntities.STUDENT.get().create(level);
        if (entity == null) {
            LOGGER.error("创建学生实体失败: {}", studentId);
            return null;
        }
        
        // 设置位置
        entity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        
        // 绑定学生 ID
        entity.bindStudent(studentId);
        
        // 设置所有者
        if (owner != null) {
            entity.setOwnerUUID(owner.getUUID());
        }
        
        // 生成到世界
        level.addFreshEntity(entity);
        
        // 记录
        spawnedStudents.put(studentId.toLowerCase(), entity.getUUID());
        
        // 创建驱动
        StudentEntityDriver driver = new StudentEntityDriver(entity);
        entityDrivers.put(entity.getUUID(), driver);
        
        LOGGER.info("已生成学生实体: {} at {}", studentId, pos);
        return entity;
    }
    
    /**
     * 在玩家附近生成学生
     * 
     * @param player 玩家
     * @param studentId 学生 ID
     * @return 生成的实体
     */
    @Nullable
    public StudentEntity spawnNearPlayer(ServerPlayer player, String studentId) {
        // 在玩家前方 2 格位置生成
        Vec3 look = player.getLookAngle();
        BlockPos spawnPos = player.blockPosition().offset(
                (int) (look.x * 2),
                0,
                (int) (look.z * 2)
        );
        
        return spawnStudent(player.serverLevel(), studentId, spawnPos, player);
    }
    
    /**
     * 召唤所有已注册的学生
     * 
     * @param player 召唤者
     */
    public void summonAllStudents(ServerPlayer player) {
        int count = 0;
        double angle = 0;
        double radius = 3;
        
        for (Persona persona : Anima.getInstance().getPersonaManager().getAllPersonas().values()) {
            // 计算环形分布位置
            double x = player.getX() + radius * Math.cos(angle);
            double z = player.getZ() + radius * Math.sin(angle);
            BlockPos pos = new BlockPos((int) x, (int) player.getY(), (int) z);
            
            StudentEntity entity = spawnStudent(player.serverLevel(), persona.getId(), pos, player);
            if (entity != null) {
                count++;
                angle += Math.PI * 2 / Anima.getInstance().getPersonaManager().getAllPersonas().size();
            }
        }
        
        LOGGER.info("已召唤 {} 个学生实体", count);
    }
    
    // ==================== 移除 ====================
    
    /**
     * 移除指定学生实体
     * 
     * @param studentId 学生 ID
     */
    public void despawnStudent(String studentId) {
        UUID entityUUID = spawnedStudents.remove(studentId.toLowerCase());
        if (entityUUID != null) {
            entityDrivers.remove(entityUUID);
            // 实体会在下次 tick 时被移除
            LOGGER.info("已标记移除学生实体: {}", studentId);
        }
    }
    
    /**
     * 移除所有学生实体
     * 
     * @param level 世界
     */
    public void despawnAllStudents(ServerLevel level) {
        for (UUID entityUUID : spawnedStudents.values()) {
            // 查找并移除实体
            level.getEntities().get(entityUUID);
        }
        spawnedStudents.clear();
        entityDrivers.clear();
        LOGGER.info("已移除所有学生实体");
    }
    
    // ==================== 查询 ====================
    
    /**
     * 获取学生实体（通过服务端全局查找）
     * 
     * @param studentId 学生 ID
     * @return 实体，不存在返回 null
     */
    @Nullable
    public StudentEntity getEntity(String studentId) {
        UUID entityUUID = spawnedStudents.get(studentId.toLowerCase());
        if (entityUUID == null) return null;
        
        // 遍历所有世界查找实体
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        
        for (ServerLevel level : server.getAllLevels()) {
            var entity = level.getEntity(entityUUID);
            if (entity instanceof StudentEntity studentEntity) {
                return studentEntity;
            }
        }
        return null;
    }
    
    /**
     * 获取学生实体
     * 
     * @param level 世界
     * @param studentId 学生 ID
     * @return 实体，不存在返回 null
     */
    @Nullable
    public StudentEntity getStudentEntity(ServerLevel level, String studentId) {
        UUID entityUUID = spawnedStudents.get(studentId.toLowerCase());
        if (entityUUID == null) return null;
        
        var entity = level.getEntity(entityUUID);
        if (entity instanceof StudentEntity studentEntity) {
            return studentEntity;
        }
        return null;
    }
    
    /**
     * 获取实体驱动
     * 
     * @param studentId 学生 ID
     * @return 驱动，不存在返回 null
     */
    @Nullable
    public StudentEntityDriver getDriver(String studentId) {
        UUID entityUUID = spawnedStudents.get(studentId.toLowerCase());
        if (entityUUID == null) return null;
        return entityDrivers.get(entityUUID);
    }
    
    /**
     * 检查学生是否已生成
     */
    public boolean isSpawned(String studentId) {
        return spawnedStudents.containsKey(studentId.toLowerCase());
    }
    
    /**
     * 获取所有已生成学生的 ID
     */
    public Iterable<String> getSpawnedStudentIds() {
        return spawnedStudents.keySet();
    }
    
    // ==================== 清理 ====================
    
    /**
     * 强制清理所有记录
     * 用于命令 /anima clear
     */
    public void clearAll() {
        spawnedStudents.clear();
        entityDrivers.clear();
        LOGGER.info("已清空所有学生实体记录");
    }
    
    /**
     * 清理无效引用
     * 应在服务器 tick 中定期调用
     */
    public void cleanup(ServerLevel level) {
        spawnedStudents.entrySet().removeIf(entry -> {
            var entity = level.getEntity(entry.getValue());
            if (entity == null || entity.isRemoved()) {
                entityDrivers.remove(entry.getValue());
                LOGGER.debug("清理无效学生引用: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }
}
