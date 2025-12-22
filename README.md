# Anima

Origin of Miracles 的 AI 角色系统模组。

基于 Miracle-Bridge 提供的基础设施，实现智能 NPC 对话系统。

## 功能

- 🤖 **LLM 集成** - 支持 OpenAI 兼容 API（OpenAI、Claude via Adapter、Ollama 等）
- 👥 **多角色人格** - 可配置的角色人格系统，内置阿罗娜示例
- 💬 **MomoTalk 对话** - 与 Shittim-OS 前端集成的聊天界面
- 🔄 **异步通信** - 基于事件推送的非阻塞对话流程

## 依赖

- Minecraft Forge 1.20.1 (47.2.0+)
- [Miracle-Bridge](../Miracle-Bridge) 0.1.0-alpha+

## 安装

1. 确保已安装 Miracle-Bridge
2. 将 `anima-0.1.0-alpha.jar` 放入 `mods/` 目录
3. 启动游戏，配置文件会自动生成

## 配置

### LLM 配置

配置文件：`config/anima/config.json`

```json
{
  "baseUrl": "https://api.openai.com/v1",
  "model": "gpt-4o-mini",
  "apiKey": "your-api-key-here",
  "maxTokens": 1024,
  "temperature": 0.7,
  "requestTimeoutSeconds": 30
}
```

| 字段 | 说明 |
|------|------|
| `baseUrl` | API 基础地址，只到 `/v1`（代码会补全 `/chat/completions`）|
| `model` | 模型名称 |
| `apiKey` | API 密钥 |
| `maxTokens` | 最大生成 Token 数 |
| `temperature` | 温度参数 (0-2) |
| `requestTimeoutSeconds` | 请求超时时间 |

### 人格配置

配置目录：`config/anima/personas/`

每个角色一个 JSON 文件，首次启动会生成 `arona.json` 作为模板。

```json
{
  "id": "arona",
  "name": "阿罗娜",
  "nameEn": "Arona",
  "school": "联邦学生会",
  "club": "Shittim 箱",
  "role": "系统管理 AI",
  "personalityTraits": [
    "温柔体贴",
    "认真负责",
    "有些天然呆"
  ],
  "speechPatterns": [
    "称呼玩家为「老师」",
    "句尾常用「~」「！」"
  ],
  "systemPrompt": "你是阿罗娜...",
  "exampleDialogues": [
    { "user": "你好", "assistant": "老师，早上好~！" }
  ]
}
```

### 添加新角色

1. 复制 `arona.json` 为新文件，如 `aris.json`
2. 修改 `id`、`name` 等基础信息
3. 编写 `systemPrompt` 定义角色人格
4. 重启游戏或使用命令重载

## Bridge API

Anima 通过 Miracle-Bridge 注册以下 API：

| API | 说明 |
|-----|------|
| `anima.chat` | 发送聊天消息 |
| `anima.getStudents` | 获取所有可用学生 |
| `anima.getStudent` | 获取单个学生信息 |
| `anima.clearHistory` | 清空对话历史 |

### 事件

| 事件 | 说明 |
|------|------|
| `studentReply` | 学生回复（LLM 生成完成后推送）|

## 开发

### 构建

```bash
# 首次设置
./gradlew setupDecompWorkspace

# 构建
./gradlew build

# 复制依赖到 libs/
cp ../Miracle-Bridge/build/libs/miraclebridge-*.jar libs/
```

### 项目结构

```
src/main/java/com/originofmiracles/anima/
├── Anima.java                 # Mod 主类
├── config/
│   ├── AnimaConfig.java       # LLM 配置
│   └── PersonaManager.java    # 人格配置管理
├── persona/
│   └── Persona.java           # 人格数据模型
├── llm/
│   └── LLMService.java        # LLM 服务层
├── agent/
│   ├── StudentAgent.java      # 学生代理
│   └── StudentAgentManager.java
└── integration/
    └── BridgeIntegration.java # Bridge API 注册
```

## 许可证

MIT License