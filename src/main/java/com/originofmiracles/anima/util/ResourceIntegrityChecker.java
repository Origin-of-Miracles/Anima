package com.originofmiracles.anima.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.originofmiracles.anima.Anima;
import com.originofmiracles.anima.config.PersonaManager;
import com.originofmiracles.anima.persona.Persona;
import com.originofmiracles.anima.util.ResourceValidator.ModelValidationResult;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * 资源完整性检查器
 * 
 * 在 Mod 启动时执行资源完整性检查，生成详细的状态报告。
 * 
 * 检查内容：
 * - 人格配置文件是否存在
 * - 模型/纹理/动画文件是否完整
 * - 交叉验证人格与模型的匹配关系
 * 
 * 生成报告：
 * - 在日志中输出详细的检查结果
 * - 标记 Critical/Warning/Info 级别的问题
 * - 提供修复建议
 */
public class ResourceIntegrityChecker {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private final List<CheckResult> results = new ArrayList<>();
    
    /**
     * 执行完整性检查
     * 
     * @param personaManager 人格管理器
     */
    public void performCheck(PersonaManager personaManager) {
        LOGGER.info("==================== Anima 资源完整性检查 ====================");
        
        results.clear();
        
        // 检查人格配置
        checkPersonas(personaManager);
        
        // 仅在客户端检查模型资源
        if (FMLEnvironment.dist == Dist.CLIENT) {
            checkModelsForPersonas(personaManager);
        }
        
        // 生成报告
        generateReport();
        
        LOGGER.info("==================== 检查完成 ====================");
    }
    
    /**
     * 检查人格配置
     */
    private void checkPersonas(PersonaManager personaManager) {
        Map<String, Persona> personas = personaManager.getAllPersonas();
        
        if (personas.isEmpty()) {
            results.add(CheckResult.critical(
                    "人格配置",
                    "未找到任何人格配置文件",
                    "请在 config/anima/personas/ 目录下添加学生的 JSON 配置文件"
            ));
        } else {
            results.add(CheckResult.info(
                    "人格配置",
                    String.format("已加载 %d 个人格配置: %s", 
                            personas.size(), 
                            String.join(", ", personas.keySet())),
                    null
            ));
            
            // 验证每个人格的必要字段
            for (Map.Entry<String, Persona> entry : personas.entrySet()) {
                validatePersona(entry.getKey(), entry.getValue());
            }
        }
    }
    
    /**
     * 验证单个人格配置的完整性
     */
    private void validatePersona(String id, Persona persona) {
        List<String> missingFields = new ArrayList<>();
        
        if (persona.getName() == null || persona.getName().isEmpty()) {
            missingFields.add("name");
        }
        if (persona.getSystemPrompt() == null || persona.getSystemPrompt().isEmpty()) {
            missingFields.add("systemPrompt");
        }
        
        if (!missingFields.isEmpty()) {
            results.add(CheckResult.warning(
                    "人格: " + id,
                    "缺少必要字段: " + String.join(", ", missingFields),
                    "请在配置文件中补充这些字段以获得完整体验"
            ));
        }
    }
    
    /**
     * 检查人格对应的模型资源
     */
    @OnlyIn(Dist.CLIENT)
    private void checkModelsForPersonas(PersonaManager personaManager) {
        Map<String, Persona> personas = personaManager.getAllPersonas();
        
        for (String studentId : personas.keySet()) {
            ModelValidationResult validation = ResourceValidator.validateGeckoLibModel(studentId);
            
            if (validation.isComplete()) {
                results.add(CheckResult.info(
                        "模型: " + studentId,
                        "资源完整（模型、纹理、动画）",
                        null
                ));
            } else if (validation.hasModel()) {
                results.add(CheckResult.warning(
                        "模型: " + studentId,
                        validation.getMissingResourcesDescription(),
                        "将使用默认资源替代缺失的文件"
                ));
            } else {
                results.add(CheckResult.warning(
                        "模型: " + studentId,
                        "未找到专属模型文件",
                        "将使用默认模型。如需自定义，请在 Anima-Assets 仓库添加模型文件"
                ));
            }
        }
    }
    
    /**
     * 生成并输出检查报告
     */
    private void generateReport() {
        int criticalCount = 0;
        int warningCount = 0;
        int infoCount = 0;
        
        for (CheckResult result : results) {
            switch (result.level) {
                case CRITICAL -> {
                    criticalCount++;
                    LOGGER.error("[CRITICAL] {}: {}", result.category, result.message);
                    if (result.suggestion != null) {
                        LOGGER.error("  建议: {}", result.suggestion);
                    }
                }
                case WARNING -> {
                    warningCount++;
                    LOGGER.warn("[WARNING] {}: {}", result.category, result.message);
                    if (result.suggestion != null) {
                        LOGGER.warn("  建议: {}", result.suggestion);
                    }
                }
                case INFO -> {
                    infoCount++;
                    LOGGER.info("[INFO] {}: {}", result.category, result.message);
                }
            }
        }
        
        LOGGER.info("==================== 检查摘要 ====================");
        LOGGER.info("Critical: {}  |  Warning: {}  |  Info: {}", 
                criticalCount, warningCount, infoCount);
        
        if (criticalCount > 0) {
            LOGGER.error("发现 {} 个严重问题，Mod 将使用默认配置运行", criticalCount);
        } else if (warningCount > 0) {
            LOGGER.warn("发现 {} 个警告，部分功能可能受限", warningCount);
        } else {
            LOGGER.info("所有资源检查通过！");
        }
    }
    
    /**
     * 获取检查结果列表
     */
    public List<CheckResult> getResults() {
        return new ArrayList<>(results);
    }
    
    /**
     * 检查结果
     */
    public static class CheckResult {
        private final CheckLevel level;
        private final String category;
        private final String message;
        private final String suggestion;
        
        private CheckResult(CheckLevel level, String category, String message, String suggestion) {
            this.level = level;
            this.category = category;
            this.message = message;
            this.suggestion = suggestion;
        }
        
        public static CheckResult critical(String category, String message, String suggestion) {
            return new CheckResult(CheckLevel.CRITICAL, category, message, suggestion);
        }
        
        public static CheckResult warning(String category, String message, String suggestion) {
            return new CheckResult(CheckLevel.WARNING, category, message, suggestion);
        }
        
        public static CheckResult info(String category, String message, String suggestion) {
            return new CheckResult(CheckLevel.INFO, category, message, suggestion);
        }
        
        public CheckLevel getLevel() {
            return level;
        }
        
        public String getCategory() {
            return category;
        }
        
        public String getMessage() {
            return message;
        }
        
        public String getSuggestion() {
            return suggestion;
        }
    }
    
    /**
     * 检查级别
     */
    public enum CheckLevel {
        /**
         * 严重问题：缺少核心资源，Mod 将使用默认配置
         */
        CRITICAL,
        
        /**
         * 警告：部分资源缺失，功能受限但可运行
         */
        WARNING,
        
        /**
         * 信息：资源正常或提示性信息
         */
        INFO
    }
}
