package com.originofmiracles.anima.entity;

import com.originofmiracles.anima.Anima;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Anima 实体注册
 * 
 * 使用 DeferredRegister 延迟注册所有实体类型
 */
public class ModEntities {
    
    /**
     * 实体延迟注册器
     */
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = 
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Anima.MOD_ID);
    
    /**
     * 学生实体类型
     * 
     * 配置：
     * - 大小：0.6 x 1.8（与玩家相同）
     * - 分类：CREATURE（友好生物）
     * - 追踪范围：80 格
     * - 客户端追踪间隔：3 tick
     */
    public static final RegistryObject<EntityType<StudentEntity>> STUDENT = ENTITY_TYPES.register("student",
            () -> EntityType.Builder.<StudentEntity>of(StudentEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(80)
                    .updateInterval(3)
                    .build(new ResourceLocation(Anima.MOD_ID, "student").toString())
    );
    
    /**
     * 注册到 MOD 事件总线
     * 
     * @param eventBus MOD 事件总线
     */
    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
