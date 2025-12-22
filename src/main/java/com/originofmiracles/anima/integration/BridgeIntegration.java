package com.originofmiracles.anima.integration;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.originofmiracles.anima.agent.StudentAgentManager;
import com.originofmiracles.anima.llm.LLMService.ChatResponse;
import com.originofmiracles.miraclebridge.MiracleBridge;
import com.originofmiracles.miraclebridge.bridge.BridgeAPI;
import com.originofmiracles.miraclebridge.browser.BrowserManager;
import com.originofmiracles.miraclebridge.event.BrowserCreatedEvent;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Miracle-Bridge 集成
 * 
 * 向 BridgeAPI 注册 Anima 的处理器，实现前端通信
 */
public class BridgeIntegration {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private static StudentAgentManager agentManagerRef;
    private static boolean handlersRegistered = false;
    
    /**
     * 初始化 Bridge 集成
     * 监听浏览器创建事件
     * 
     * @param agentManager 学生代理管理器
     */
    public static void init(StudentAgentManager agentManager) {
        agentManagerRef = agentManager;
        
        // 注册事件监听器
        MinecraftForge.EVENT_BUS.register(BridgeIntegration.class);
        
        // 尝试立即注册（如果浏览器已存在）
        tryRegisterHandlers();
        
        LOGGER.info("BridgeIntegration 已初始化，等待浏览器创建...");
    }
    
    /**
     * 监听浏览器创建事件
     */
    @SubscribeEvent
    public static void onBrowserCreated(BrowserCreatedEvent event) {
        // 只在主浏览器创建时注册处理器
        if (BrowserManager.DEFAULT_BROWSER_NAME.equals(event.getBrowserName())) {
            LOGGER.info("检测到主浏览器创建，注册 Anima 处理器...");
            tryRegisterHandlers();
        }
    }
    
    /**
     * 尝试注册处理器（如果 BridgeAPI 可用且尚未注册）
     */
    private static void tryRegisterHandlers() {
        if (handlersRegistered || agentManagerRef == null) {
            return;
        }
        
        BridgeAPI bridgeAPI = MiracleBridge.getBridgeAPI();
        
        if (bridgeAPI == null) {
            LOGGER.debug("BridgeAPI 尚不可用，等待浏览器创建事件...");
            return;
        }
        
        registerHandlers(bridgeAPI, agentManagerRef);
        handlersRegistered = true;
    }
    
    /**
     * 注册所有 Anima Bridge 处理器
     * 
     * @param bridgeAPI BridgeAPI 实例
     * @param agentManager 学生代理管理器
     */
    private static void registerHandlers(BridgeAPI bridgeAPI, StudentAgentManager agentManager) {
        // 注册 anima.chat - 发送聊天消息
        bridgeAPI.register("anima.chat", request -> {
            return handleChat(bridgeAPI, agentManager, request);
        });
        
        // 注册 anima.getStudents - 获取所有学生列表
        bridgeAPI.register("anima.getStudents", request -> {
            return handleGetStudents(agentManager);
        });
        
        // 注册 anima.getStudent - 获取单个学生信息
        bridgeAPI.register("anima.getStudent", request -> {
            return handleGetStudent(agentManager, request);
        });
        
        // 注册 anima.clearHistory - 清空对话历史
        bridgeAPI.register("anima.clearHistory", request -> {
            return handleClearHistory(agentManager, request);
        });
        
        // 注册 anima.getMood - 获取学生情绪状态
        bridgeAPI.register("anima.getMood", request -> {
            return handleGetMood(agentManager, request);
        });
        
        // 注册 anima.triggerMood - 触发情绪事件
        bridgeAPI.register("anima.triggerMood", request -> {
            return handleTriggerMood(agentManager, request);
        });
        
        LOGGER.info("Anima Bridge 处理器注册完成");
    }
    
    /**
     * 处理聊天请求
     * 
     * 请求格式:
     * {
     *   "studentId": "arona",
     *   "message": "你好"
     * }
     * 
     * 同步响应（立即返回）:
     * {
     *   "success": true,
     *   "requestId": "uuid"
     * }
     * 
     * 异步事件推送（LLM 完成后）:
     * Event: "studentReply"
     * {
     *   "requestId": "uuid",
     *   "studentId": "arona",
     *   "content": "老师，早上好~！",
     *   "success": true
     * }
     */
    private static JsonObject handleChat(BridgeAPI bridgeAPI, StudentAgentManager agentManager, JsonObject request) {
        JsonObject response = new JsonObject();
        
        // 验证参数
        if (!request.has("studentId") || !request.has("message")) {
            response.addProperty("success", false);
            response.addProperty("error", "缺少必要参数: studentId, message");
            return response;
        }
        
        String studentId = request.get("studentId").getAsString();
        String message = request.get("message").getAsString();
        String requestId = UUID.randomUUID().toString();
        
        LOGGER.debug("收到聊天请求: studentId={}, message={}", studentId, message);
        
        // 立即返回 requestId
        response.addProperty("success", true);
        response.addProperty("requestId", requestId);
        
        // 异步调用 LLM
        CompletableFuture<ChatResponse> future = agentManager.chat(studentId, message);
        
        future.thenAccept(chatResponse -> {
            // 构建事件数据
            JsonObject eventData = new JsonObject();
            eventData.addProperty("requestId", requestId);
            eventData.addProperty("studentId", studentId);
            eventData.addProperty("success", chatResponse.isSuccess());
            
            if (chatResponse.isSuccess()) {
                eventData.addProperty("content", chatResponse.getContent());
                eventData.addProperty("promptTokens", chatResponse.getPromptTokens());
                eventData.addProperty("completionTokens", chatResponse.getCompletionTokens());
            } else {
                eventData.addProperty("error", chatResponse.getError());
            }
            
            // 推送事件到前端
            bridgeAPI.pushEvent("studentReply", eventData);
            
            LOGGER.debug("已推送回复事件: requestId={}, success={}", requestId, chatResponse.isSuccess());
        });
        
        return response;
    }
    
    /**
     * 处理获取学生列表请求
     * 
     * 响应格式:
     * {
     *   "success": true,
     *   "students": [
     *     { "id": "arona", "name": "阿罗娜", ... }
     *   ]
     * }
     */
    private static JsonObject handleGetStudents(StudentAgentManager agentManager) {
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.add("students", agentManager.getAvailableStudents());
        return response;
    }
    
    /**
     * 处理获取单个学生信息请求
     * 
     * 请求格式:
     * { "studentId": "arona" }
     * 
     * 响应格式:
     * {
     *   "success": true,
     *   "student": { "id": "arona", "name": "阿罗娜", ... }
     * }
     */
    private static JsonObject handleGetStudent(StudentAgentManager agentManager, JsonObject request) {
        JsonObject response = new JsonObject();
        
        if (!request.has("studentId")) {
            response.addProperty("success", false);
            response.addProperty("error", "缺少 studentId 参数");
            return response;
        }
        
        String studentId = request.get("studentId").getAsString();
        JsonObject student = agentManager.getStudentInfo(studentId);
        
        if (student != null) {
            response.addProperty("success", true);
            response.add("student", student);
        } else {
            response.addProperty("success", false);
            response.addProperty("error", "未找到学生: " + studentId);
        }
        
        return response;
    }
    
    /**
     * 处理清空对话历史请求
     * 
     * 请求格式:
     * { "studentId": "arona" }  // 清空指定学生
     * { }                       // 清空所有
     */
    private static JsonObject handleClearHistory(StudentAgentManager agentManager, JsonObject request) {
        JsonObject response = new JsonObject();
        
        if (request.has("studentId")) {
            String studentId = request.get("studentId").getAsString();
            boolean success = agentManager.clearHistory(studentId);
            response.addProperty("success", success);
            if (!success) {
                response.addProperty("error", "未找到学生: " + studentId);
            }
        } else {
            agentManager.clearAllHistory();
            response.addProperty("success", true);
        }
        
        return response;
    }
    
    /**
     * 处理获取学生情绪状态请求
     * 
     * 请求格式:
     * { "studentId": "arona" }
     * 
     * 响应格式:
     * {
     *   "success": true,
     *   "mood": {
     *     "state": "happy",
     *     "displayName": "开心",
     *     "intensity": 0.7,
     *     "description": "明显开心",
     *     "animationId": "emote_happy"
     *   }
     * }
     */
    private static JsonObject handleGetMood(StudentAgentManager agentManager, JsonObject request) {
        JsonObject response = new JsonObject();
        
        if (!request.has("studentId")) {
            response.addProperty("success", false);
            response.addProperty("error", "缺少 studentId 参数");
            return response;
        }
        
        String studentId = request.get("studentId").getAsString();
        
        // 获取代理（不创建新的）
        com.originofmiracles.anima.agent.StudentAgent agent = agentManager.getOrCreateAgent(studentId);
        if (agent == null) {
            response.addProperty("success", false);
            response.addProperty("error", "未找到学生: " + studentId);
            return response;
        }
        
        // 构建情绪信息
        JsonObject moodInfo = new JsonObject();
        moodInfo.addProperty("state", agent.getCurrentMood().getId());
        moodInfo.addProperty("displayName", agent.getCurrentMood().getDisplayName());
        moodInfo.addProperty("intensity", agent.getMoodIntensity());
        moodInfo.addProperty("description", agent.getMoodSystem().getMoodDescription());
        moodInfo.addProperty("animationId", agent.getMoodAnimationId());
        moodInfo.addProperty("isPositive", agent.getCurrentMood().isPositive());
        moodInfo.addProperty("isNegative", agent.getCurrentMood().isNegative());
        
        response.addProperty("success", true);
        response.add("mood", moodInfo);
        
        return response;
    }
    
    /**
     * 处理触发情绪事件请求
     * 
     * 请求格式:
     * {
     *   "studentId": "arona",
     *   "trigger": "received_gift",
     *   "multiplier": 1.0  // 可选
     * }
     */
    private static JsonObject handleTriggerMood(StudentAgentManager agentManager, JsonObject request) {
        JsonObject response = new JsonObject();
        
        if (!request.has("studentId") || !request.has("trigger")) {
            response.addProperty("success", false);
            response.addProperty("error", "缺少必要参数: studentId, trigger");
            return response;
        }
        
        String studentId = request.get("studentId").getAsString();
        String triggerId = request.get("trigger").getAsString();
        float multiplier = request.has("multiplier") ? request.get("multiplier").getAsFloat() : 1.0f;
        
        com.originofmiracles.anima.agent.StudentAgent agent = agentManager.getOrCreateAgent(studentId);
        if (agent == null) {
            response.addProperty("success", false);
            response.addProperty("error", "未找到学生: " + studentId);
            return response;
        }
        
        com.originofmiracles.anima.mood.MoodTrigger trigger = 
                com.originofmiracles.anima.mood.MoodTrigger.fromId(triggerId);
        
        if (trigger == null) {
            response.addProperty("success", false);
            response.addProperty("error", "未知的情绪触发器: " + triggerId);
            return response;
        }
        
        agent.getMoodSystem().applyTrigger(trigger, multiplier);
        
        response.addProperty("success", true);
        response.addProperty("newState", agent.getCurrentMood().getId());
        response.addProperty("newIntensity", agent.getMoodIntensity());
        
        return response;
    }
}
