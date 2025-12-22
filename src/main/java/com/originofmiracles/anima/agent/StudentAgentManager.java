package com.originofmiracles.anima.agent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.originofmiracles.anima.config.PersonaManager;
import com.originofmiracles.anima.llm.LLMService;
import com.originofmiracles.anima.llm.LLMService.ChatResponse;
import com.originofmiracles.anima.persona.Persona;

/**
 * 学生代理管理器
 * 
 * 管理所有学生代理的创建、获取和生命周期
 * 支持按需创建代理（懒加载）
 */
public class StudentAgentManager {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private final LLMService llmService;
    private final PersonaManager personaManager;
    
    /**
     * 已创建的代理实例
     * Key: studentId (小写)
     */
    private final Map<String, StudentAgent> agents = new HashMap<>();
    
    public StudentAgentManager(LLMService llmService, PersonaManager personaManager) {
        this.llmService = llmService;
        this.personaManager = personaManager;
        
        LOGGER.info("学生代理管理器已初始化");
    }
    
    /**
     * 获取或创建学生代理
     * 
     * @param studentId 学生 ID
     * @return 学生代理，如果人格配置不存在则返回 null
     */
    @Nullable
    public StudentAgent getOrCreateAgent(String studentId) {
        String id = studentId.toLowerCase();
        
        // 检查缓存
        StudentAgent cached = agents.get(id);
        if (cached != null) {
            return cached;
        }
        
        // 获取人格配置
        Persona persona = personaManager.getPersona(id);
        if (persona == null) {
            LOGGER.warn("未找到人格配置: {}", studentId);
            return null;
        }
        
        // 创建新代理
        StudentAgent agent = new StudentAgent(id, persona, llmService);
        agents.put(id, agent);
        
        LOGGER.info("已创建学生代理: {} ({})", persona.getName(), id);
        return agent;
    }
    
    /**
     * 处理聊天请求
     * 
     * @param studentId 学生 ID
     * @param message 用户消息
     * @return 包含回复的 CompletableFuture
     */
    public CompletableFuture<ChatResponse> chat(String studentId, String message) {
        StudentAgent agent = getOrCreateAgent(studentId);
        
        if (agent == null) {
            return CompletableFuture.completedFuture(
                ChatResponse.error("未找到学生: " + studentId));
        }
        
        return agent.chat(message);
    }
    
    /**
     * 获取所有可用学生列表
     * 
     * @return 学生信息 JSON 数组
     */
    public JsonArray getAvailableStudents() {
        JsonArray array = new JsonArray();
        
        for (Persona persona : personaManager.getAllPersonas().values()) {
            JsonObject student = new JsonObject();
            student.addProperty("id", persona.getId());
            student.addProperty("name", persona.getName());
            student.addProperty("nameEn", persona.getNameEn());
            student.addProperty("school", persona.getSchool());
            student.addProperty("club", persona.getClub());
            student.addProperty("role", persona.getRole());
            
            // 检查是否有活跃会话
            StudentAgent agent = agents.get(persona.getId().toLowerCase());
            boolean hasSession = agent != null && agent.getHistorySize() > 0;
            int historySize = agent != null ? agent.getHistorySize() : 0;
            
            student.addProperty("hasActiveSession", hasSession);
            student.addProperty("historySize", historySize);
            
            array.add(student);
        }
        
        return array;
    }
    
    /**
     * 获取单个学生信息
     * 
     * @param studentId 学生 ID
     * @return 学生信息 JSON，不存在则返回 null
     */
    @Nullable
    public JsonObject getStudentInfo(String studentId) {
        Persona persona = personaManager.getPersona(studentId);
        if (persona == null) {
            return null;
        }
        
        JsonObject student = new JsonObject();
        student.addProperty("id", persona.getId());
        student.addProperty("name", persona.getName());
        student.addProperty("nameEn", persona.getNameEn());
        student.addProperty("school", persona.getSchool());
        student.addProperty("club", persona.getClub());
        student.addProperty("role", persona.getRole());
        
        // 检查是否有活跃会话
        StudentAgent agent = agents.get(persona.getId().toLowerCase());
        student.addProperty("hasActiveSession", agent != null && agent.getHistorySize() > 0);
        if (agent != null) {
            student.addProperty("historySize", agent.getHistorySize());
        }
        
        return student;
    }
    
    /**
     * 清空指定学生的对话历史
     * 
     * @param studentId 学生 ID
     * @return 是否成功
     */
    public boolean clearHistory(String studentId) {
        StudentAgent agent = agents.get(studentId.toLowerCase());
        if (agent != null) {
            agent.clearHistory();
            return true;
        }
        return false;
    }
    
    /**
     * 清空所有学生的对话历史
     */
    public void clearAllHistory() {
        for (StudentAgent agent : agents.values()) {
            agent.clearHistory();
        }
        LOGGER.info("已清空所有学生对话历史");
    }
    
    /**
     * 移除学生代理（释放资源）
     * 
     * @param studentId 学生 ID
     */
    public void removeAgent(String studentId) {
        StudentAgent removed = agents.remove(studentId.toLowerCase());
        if (removed != null) {
            LOGGER.info("已移除学生代理: {}", studentId);
        }
    }
    
    /**
     * 获取当前活跃代理数量
     */
    public int getActiveAgentCount() {
        return agents.size();
    }
    
    /**
     * 重新加载人格配置
     * 不会影响已创建的代理
     */
    public void reloadPersonas() {
        personaManager.reload();
        LOGGER.info("已重新加载人格配置");
    }
}
