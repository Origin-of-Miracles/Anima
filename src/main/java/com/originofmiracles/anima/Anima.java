package com.originofmiracles.anima;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.originofmiracles.anima.agent.StudentAgentManager;
import com.originofmiracles.anima.client.renderer.StudentEntityRenderer;
import com.originofmiracles.anima.config.AnimaConfig;
import com.originofmiracles.anima.config.PersonaManager;
import com.originofmiracles.anima.entity.ModEntities;
import com.originofmiracles.anima.entity.StudentEntity;
import com.originofmiracles.anima.integration.BridgeIntegration;
import com.originofmiracles.anima.llm.LLMService;
import com.originofmiracles.anima.util.ResourceIntegrityChecker;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Anima - Origin of Miracles 的 AI 角色系统
 * 
 * 本模组提供：
 * - 智能 NPC 对话系统 (基于 LLM)
 * - 角色人格配置与记忆
 * - 多角色并发管理
 * 
 * 依赖 Miracle-Bridge 提供的 BridgeAPI 进行前端通信
 * 
 * @author Origin of Miracles Dev Team
 * @version 0.1.0-alpha
 */
@Mod(Anima.MOD_ID)
public class Anima {
    
    public static final String MOD_ID = "anima";
    public static final String MOD_NAME = "Anima";
    public static final String VERSION = "0.1.0-alpha";
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private static Anima instance;
    
    private LLMService llmService;
    private StudentAgentManager agentManager;
    private PersonaManager personaManager;
    
    public Anima() {
        instance = this;
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // 注册实体
        ModEntities.register(modEventBus);
        
        // 注册生命周期事件
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::loadComplete);
        modEventBus.addListener(this::registerEntityAttributes);
        
        // 注册到 Forge 事件总线
        modEventBus.addListener((FMLCommonSetupEvent event) -> {
            MinecraftForge.EVENT_BUS.register(instance);
        });
        
        LOGGER.info("Anima 已初始化 - AI 角色系统启动中...");
    }
    
    /**
     * 通用设置
     */
    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Anima 通用设置");
        
        event.enqueueWork(() -> {
            // 初始化配置系统
            AnimaConfig.init(FMLPaths.CONFIGDIR.get());
            
            // 初始化人格管理器
            personaManager = new PersonaManager(FMLPaths.CONFIGDIR.get());
            personaManager.loadAllPersonas();
            
            // 初始化 LLM 服务
            llmService = new LLMService(AnimaConfig.getInstance());
            
            // 初始化学生代理管理器
            agentManager = new StudentAgentManager(llmService, personaManager);
            
            // 执行资源完整性检查
            ResourceIntegrityChecker checker = new ResourceIntegrityChecker();
            checker.performCheck(personaManager);
            
            LOGGER.info("Anima 核心服务已初始化");
        });
    }
    
    /**
     * 仅客户端设置
     */
    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("Anima 客户端设置");
        
        event.enqueueWork(() -> {
            // 注册实体渲染器
            EntityRenderers.register(ModEntities.STUDENT.get(), StudentEntityRenderer::new);
            LOGGER.info("学生实体渲染器已注册");
        });
    }
    
    /**
     * 注册实体属性
     */
    private void registerEntityAttributes(final EntityAttributeCreationEvent event) {
        event.put(ModEntities.STUDENT.get(), StudentEntity.createAttributes().build());
        LOGGER.info("学生实体属性已注册");
    }
    
    /**
     * 加载完成 - 所有模组都已加载
     */
    private void loadComplete(final FMLLoadCompleteEvent event) {
        LOGGER.info("Anima 加载完成");
        
        event.enqueueWork(() -> {
            // 初始化 Bridge 集成
            // 会监听浏览器创建事件，在浏览器就绪时自动注册处理器
            BridgeIntegration.init(agentManager);
            
            LOGGER.info("Bridge 集成已初始化");
        });
    }
    
    /**
     * 服务器停止事件
     */
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Anima 正在清理资源...");
        
        if (llmService != null) {
            llmService.shutdown();
        }
        
        LOGGER.info("资源清理完成");
    }
    
    /**
     * 注册命令
     */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        com.originofmiracles.anima.command.AnimaCommands.register(event.getDispatcher());
        LOGGER.info("Anima 命令已注册");
    }
    
    /**
     * 获取模组实例
     */
    public static Anima getInstance() {
        return instance;
    }
    
    /**
     * 获取 LLM 服务
     */
    public LLMService getLLMService() {
        return llmService;
    }
    
    /**
     * 获取学生代理管理器
     */
    public StudentAgentManager getAgentManager() {
        return agentManager;
    }
    
    /**
     * 获取人格管理器
     */
    public PersonaManager getPersonaManager() {
        return personaManager;
    }
}
