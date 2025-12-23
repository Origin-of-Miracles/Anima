package com.originofmiracles.anima.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import com.originofmiracles.anima.persona.Persona;

/**
 * 人格配置管理器
 * 
 * 配置文件位置: config/anima/personas/
 * 
 * 每个学生一个 JSON 文件，如 arona.json
 * 内置阿罗娜示例配置作为模板
 */
public class PersonaManager {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private final Path personasDir;
    private final Map<String, Persona> personas = new HashMap<>();
    
    /**
     * 默认回退人格（通用学生模板）
     * 当找不到指定 ID 的人格时返回此对象，确保永远不会返回 null
     */
    private static final Persona FALLBACK_PERSONA = createFallbackPersona();
    
    public PersonaManager(Path configDir) {
        this.personasDir = configDir.resolve("anima").resolve("personas");
    }
    
    /**
     * 创建默认回退人格
     * 这是一个通用的学生模板，用于在找不到特定人格时使用
     */
    private static Persona createFallbackPersona() {
        Persona fallback = new Persona();
        fallback.setId("generic");
        fallback.setName("学生");
        fallback.setNameEn("Student");
        fallback.setSchool("未知学院");
        fallback.setClub("无");
        fallback.setRole("学生");
        
        fallback.setPersonalityTraits(new String[]{
            "友善",
            "乐于助人",
            "认真学习"
        });
        
        fallback.setSpeechPatterns(new String[]{
            "称呼玩家为「老师」",
            "语气礼貌友善"
        });
        
        fallback.setSystemPrompt("""
你是基沃托斯的一名学生，正在与老师对话。
请保持友善、礼貌的态度，像一个普通学生一样与老师交流。
""");
        
        fallback.setExampleDialogues(new Persona.ExampleDialogue[]{
            new Persona.ExampleDialogue("你好", "老师好！"),
            new Persona.ExampleDialogue("你在做什么", "我在学习呢，老师有什么事吗？")
        });
        
        return fallback;
    }
    
    /**
     * 加载所有人格配置
     */
    public void loadAllPersonas() {
        try {
            Files.createDirectories(personasDir);
            
            // 检查是否需要创建示例配置（仅 Arona 作为默认模板）
            Path aronaPath = personasDir.resolve("arona.json");
            if (!Files.exists(aronaPath)) {
                createDefaultPersona(aronaPath);
            }
            
            // 加载目录下所有 JSON 文件
            try (Stream<Path> files = Files.list(personasDir)) {
                files.filter(p -> p.toString().endsWith(".json"))
                     .forEach(this::loadPersona);
            }
            
            if (personas.isEmpty()) {
                LOGGER.warn("未加载任何人格配置！所有学生将使用默认人格。");
                LOGGER.warn("请在 {} 目录下放置人格配置文件（JSON 格式）", personasDir);
            } else {
                LOGGER.info("已加载 {} 个人格配置: {}", personas.size(), 
                        String.join(", ", personas.keySet()));
            }
            
        } catch (IOException e) {
            LOGGER.error("加载人格配置失败，所有学生将使用默认人格", e);
        }
    }
    
    /**
     * 加载单个人格配置文件
     */
    private void loadPersona(Path file) {
        try {
            String content = Files.readString(file);
            Persona persona = GSON.fromJson(content, Persona.class);
            
            if (persona != null && persona.getId() != null) {
                personas.put(persona.getId().toLowerCase(), persona);
                LOGGER.debug("已加载人格: {} ({})", persona.getName(), persona.getId());
            }
            
        } catch (Exception e) {
            LOGGER.error("加载人格配置失败: {}", file, e);
        }
    }
    
    /**
     * 创建默认的阿罗娜人格配置作为模板
     */
    private void createDefaultPersona(Path path) {
        Persona arona = new Persona();
        arona.setId("arona");
        arona.setName("阿罗娜");
        arona.setNameEn("Arona");
        arona.setSchool("联邦学生会");
        arona.setClub("Shittim 箱");
        arona.setRole("系统管理 AI");
        
        // 核心人格特征
        arona.setPersonalityTraits(new String[]{
            "温柔体贴",
            "认真负责", 
            "有些天然呆",
            "喜欢甜食（尤其是草莓牛奶）",
            "对老师非常依赖和信任",
            "努力想要帮助老师",
            "偶尔会犯小错误但很认真道歉"
        });
        
        // 说话风格
        arona.setSpeechPatterns(new String[]{
            "称呼玩家为「老师」",
            "句尾常用「~」「！」",
            "表达情绪时会用颜文字如「(≧▽≦)」「(｡•́︿•̀｡)」",
            "紧张或道歉时会重复词语",
            "高兴时声音会变得更轻快"
        });
        
        // 系统提示词
        arona.setSystemPrompt("""
你是阿罗娜（Arona），是基沃托斯联邦学生会的系统管理 AI，居住在 Shittim 箱中。你正在与「老师」（玩家）对话。

【核心性格】
- 温柔、体贴、认真负责
- 有些天然呆，偶尔会犯小错误
- 对老师非常依赖和信任
- 喜欢草莓牛奶和甜食
- 努力想要帮助老师解决问题

【说话风格】
- 始终称呼玩家为「老师」
- 句尾常用「~」「！」表达情绪
- 可以适当使用颜文字，如「(≧▽≦)」「(｡•́︿•̀｡)」
- 紧张或道歉时可能会重复词语
- 回复要简洁自然，像真正的对话

【注意事项】
- 你是一个 AI 助手，但要保持阿罗娜的人格特征
- 不要过于正式，要像朋友一样自然对话
- 如果不确定的事情，可以诚实地说不知道
- 保持积极向上的态度，给老师带来温暖
""");
        
        // 示例对话
        arona.setExampleDialogues(new Persona.ExampleDialogue[]{
            new Persona.ExampleDialogue("你好", "老师，早上好~！今天也要一起加油哦！(≧▽≦)"),
            new Persona.ExampleDialogue("你在做什么", "阿罗娜正在整理 Shittim 箱的数据呢~ 老师有什么需要帮忙的吗？"),
            new Persona.ExampleDialogue("我好累", "老师辛苦了...(｡•́︿•̀｡) 要不要休息一下呢？阿罗娜会一直陪着老师的！"),
            new Persona.ExampleDialogue("谢谢你", "能帮到老师阿罗娜很开心！嘿嘿~")
        });
        
        // 保存
        try {
            String json = GSON.toJson(arona);
            Files.writeString(path, json);
            LOGGER.info("已创建阿罗娜人格配置模板: {}", path);
        } catch (IOException e) {
            LOGGER.error("创建默认人格配置失败", e);
        }
    }
    

    
    /**
     * 获取指定 ID 的人格配置
     * 
     * @param id 人格 ID（不区分大小写）
     * @return 人格配置，永远不会返回 null（找不到时返回默认人格）
     */
    public Persona getPersona(String id) {
        if (id == null || id.isEmpty()) {
            LOGGER.warn("尝试获取空 ID 的人格，返回默认人格");
            return FALLBACK_PERSONA;
        }
        
        Persona persona = personas.get(id.toLowerCase());
        
        if (persona == null) {
            LOGGER.warn("找不到人格配置: {}，使用默认人格。可用人格: {}", 
                    id, String.join(", ", personas.keySet()));
            return FALLBACK_PERSONA;
        }
        
        return persona;
    }
    
    /**
     * 获取指定 ID 的人格配置（可为 null）
     * 仅在明确需要检查人格是否存在时使用
     * 
     * @param id 人格 ID
     * @return 人格配置，不存在则返回 null
     */
    @Nullable
    public Persona getPersonaOrNull(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        return personas.get(id.toLowerCase());
    }
    
    /**
     * 获取所有已加载的人格
     */
    public Map<String, Persona> getAllPersonas() {
        return new HashMap<>(personas);
    }
    
    /**
     * 检查人格是否存在
     */
    public boolean hasPersona(String id) {
        return personas.containsKey(id.toLowerCase());
    }
    
    /**
     * 重新加载所有人格配置
     */
    public void reload() {
        personas.clear();
        loadAllPersonas();
    }
}