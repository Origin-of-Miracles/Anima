package com.originofmiracles.anima.persona;

/**
 * 角色人格配置
 * 
 * 从 JSON 文件加载，定义角色的性格、说话风格、系统提示词等
 */
public class Persona {
    
    // 基础信息
    private String id;
    private String name;
    private String nameEn;
    private String school;
    private String club;
    private String role;
    
    // 人格特征
    private String[] personalityTraits;
    private String[] speechPatterns;
    
    // LLM 配置
    private String systemPrompt;
    private ExampleDialogue[] exampleDialogues;
    
    // 可选的模型覆盖（如果为空则使用全局配置）
    private String modelOverride;
    private Double temperatureOverride;
    
    // ==================== YSM 模型配置 ====================
    
    /**
     * YSM 模型 ID（可选）
     * 格式: "namespace:model_name" 或 "model_name"
     * 例如: "anima:arona" 或 "arona"
     * 
     * 模型文件位置: config/yes_steve_model/custom/<model_name>/
     */
    private String ysmModelId;
    
    /**
     * YSM 纹理 ID（可选，如果不设置则使用模型默认纹理）
     */
    private String ysmTextureId;
    
    // ==================== Getters & Setters ====================
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getNameEn() {
        return nameEn;
    }
    
    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }
    
    public String getSchool() {
        return school;
    }
    
    public void setSchool(String school) {
        this.school = school;
    }
    
    public String getClub() {
        return club;
    }
    
    public void setClub(String club) {
        this.club = club;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String[] getPersonalityTraits() {
        return personalityTraits;
    }
    
    public void setPersonalityTraits(String[] personalityTraits) {
        this.personalityTraits = personalityTraits;
    }
    
    public String[] getSpeechPatterns() {
        return speechPatterns;
    }
    
    public void setSpeechPatterns(String[] speechPatterns) {
        this.speechPatterns = speechPatterns;
    }
    
    public String getSystemPrompt() {
        return systemPrompt;
    }
    
    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }
    
    public ExampleDialogue[] getExampleDialogues() {
        return exampleDialogues;
    }
    
    public void setExampleDialogues(ExampleDialogue[] exampleDialogues) {
        this.exampleDialogues = exampleDialogues;
    }
    
    public String getModelOverride() {
        return modelOverride;
    }
    
    public void setModelOverride(String modelOverride) {
        this.modelOverride = modelOverride;
    }
    
    public Double getTemperatureOverride() {
        return temperatureOverride;
    }
    
    public void setTemperatureOverride(Double temperatureOverride) {
        this.temperatureOverride = temperatureOverride;
    }
    
    public String getYsmModelId() {
        return ysmModelId;
    }
    
    public void setYsmModelId(String ysmModelId) {
        this.ysmModelId = ysmModelId;
    }
    
    public String getYsmTextureId() {
        return ysmTextureId;
    }
    
    public void setYsmTextureId(String ysmTextureId) {
        this.ysmTextureId = ysmTextureId;
    }
    
    /**
     * 检查是否配置了 YSM 模型
     */
    public boolean hasYsmModel() {
        return ysmModelId != null && !ysmModelId.isEmpty();
    }
    
    /**
     * 构建完整的系统提示词
     * 如果配置了自定义 systemPrompt 则直接使用，否则根据其他字段自动生成
     */
    public String buildFullSystemPrompt() {
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            return systemPrompt;
        }
        
        // 自动生成系统提示词
        StringBuilder sb = new StringBuilder();
        sb.append("你是").append(name);
        if (nameEn != null) {
            sb.append("（").append(nameEn).append("）");
        }
        sb.append("。");
        
        if (school != null) {
            sb.append("你来自").append(school).append("。");
        }
        if (club != null) {
            sb.append("你是").append(club).append("的成员。");
        }
        if (role != null) {
            sb.append("你的身份是").append(role).append("。");
        }
        
        if (personalityTraits != null && personalityTraits.length > 0) {
            sb.append("\n\n【性格特点】\n");
            for (String trait : personalityTraits) {
                sb.append("- ").append(trait).append("\n");
            }
        }
        
        if (speechPatterns != null && speechPatterns.length > 0) {
            sb.append("\n【说话风格】\n");
            for (String pattern : speechPatterns) {
                sb.append("- ").append(pattern).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * 示例对话
     */
    public static class ExampleDialogue {
        private String user;
        private String assistant;
        
        public ExampleDialogue() {}
        
        public ExampleDialogue(String user, String assistant) {
            this.user = user;
            this.assistant = assistant;
        }
        
        public String getUser() {
            return user;
        }
        
        public void setUser(String user) {
            this.user = user;
        }
        
        public String getAssistant() {
            return assistant;
        }
        
        public void setAssistant(String assistant) {
            this.assistant = assistant;
        }
    }
    
    @Override
    public String toString() {
        return "Persona{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", school='" + school + '\'' +
                '}';
    }
}
