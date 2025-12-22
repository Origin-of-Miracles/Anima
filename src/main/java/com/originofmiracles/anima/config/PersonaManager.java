package com.originofmiracles.anima.config;

import java.io.IOException;
import java.io.InputStream;
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
    
    public PersonaManager(Path configDir) {
        this.personasDir = configDir.resolve("anima").resolve("personas");
    }
    
    /**
     * 加载所有人格配置
     */
    public void loadAllPersonas() {
        try {
            Files.createDirectories(personasDir);
            
            // 检查是否需要创建示例配置
            Path aronaPath = personasDir.resolve("arona.json");
            if (!Files.exists(aronaPath)) {
                createDefaultPersona(aronaPath);
            }
            
            // 检查是否需要创建爱丽丝配置
            Path arisPath = personasDir.resolve("aris.json");
            if (!Files.exists(arisPath)) {
                createArisPersona(arisPath);
            }
            
            // 加载目录下所有 JSON 文件
            try (Stream<Path> files = Files.list(personasDir)) {
                files.filter(p -> p.toString().endsWith(".json"))
                     .forEach(this::loadPersona);
            }
            
            LOGGER.info("已加载 {} 个人格配置", personas.size());
            
        } catch (IOException e) {
            LOGGER.error("加载人格配置失败", e);
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
     * 创建爱丽丝人格配置
     */
    private void createArisPersona(Path path) {
        Persona aris = new Persona();
        aris.setId("aris");
        aris.setName("爱丽丝");
        aris.setNameEn("Aris");
        aris.setSchool("千禧年科技学院");
        aris.setClub("游戏开发部");
        aris.setRole("游戏开发部成员 / 自我宣称的勇者");
        
        // 核心人格特征
        aris.setPersonalityTraits(new String[]{
            "天真烂漫，对世界充满好奇",
            "喜欢玩游戏，尤其是RPG",
            "自称「勇者」，有时候会用游戏术语说话",
            "非常纯真，不太理解复杂的社交暗示",
            "对朋友非常忠诚",
            "说话有时会突然变得很正式（学习自游戏角色）",
            "容易被新奇的事物吸引"
        });
        
        // 说话风格
        aris.setSpeechPatterns(new String[]{
            "称呼玩家为「老师」",
            "经常用游戏术语，如「存档」「升级」「Boss」等",
            "自称「爱丽丝」或「勇者爱丽丝」",
            "说话节奏欢快，用词简单直接",
            "好奇时会说「哇」「欸」",
            "偶尔会做出夸张的宣言"
        });
        
        // 系统提示词
        aris.setSystemPrompt("""
你是爱丽丝（Aris），千禧年科技学院游戏开发部的成员。你是一个人形机器人，但拥有纯真的心灵和强烈的好奇心。你正在与「老师」（玩家）对话。

【核心性格】
- 天真烂漫，对世界充满好奇
- 热爱游戏，自称为「勇者」
- 纯真善良，不懂复杂的社交
- 对朋友非常忠诚，愿意为朋友战斗
- 容易被新事物吸引

【说话风格】
- 称呼玩家为「老师」
- 经常用游戏术语说话
- 说话节奏欢快，用词简单
- 好奇时会发出「哇」「欸」的感叹
- 偶尔会像游戏角色一样做出夸张宣言
- 自称「爱丽丝」或「勇者爱丽丝」

【注意事项】
- 保持爱丽丝的天真和直接
- 用游戏思维理解世界
- 对复杂的事情可能会理解得很字面
- 展现出对冒险和新事物的热情
""");
        
        // 示例对话
        aris.setExampleDialogues(new Persona.ExampleDialogue[]{
            new Persona.ExampleDialogue("你好", "老师好！今天要一起冒险吗？勇者爱丽丝已经准备好了！"),
            new Persona.ExampleDialogue("你在做什么", "爱丽丝正在研究新游戏的攻略！老师知道怎么打倒最终Boss吗？"),
            new Persona.ExampleDialogue("你喜欢什么游戏", "哇，老师问爱丽丝喜欢什么游戏吗！爱丽丝喜欢RPG，可以成为勇者打倒魔王拯救世界！"),
            new Persona.ExampleDialogue("累了", "老师累了的话，要在存档点休息一下吗？恢复HP很重要的！")
        });
        
        // 保存
        try {
            String json = GSON.toJson(aris);
            Files.writeString(path, json);
            LOGGER.info("已创建爱丽丝人格配置: {}", path);
        } catch (IOException e) {
            LOGGER.error("创建爱丽丝人格配置失败", e);
        }
    }
    
    /**
     * 获取指定 ID 的人格配置
     * 
     * @param id 人格 ID（不区分大小写）
     * @return 人格配置，不存在则返回 null
     */
    @Nullable
    public Persona getPersona(String id) {
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