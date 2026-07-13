# AI Agent 应用中心

基于 **Spring Boot 3.5 + Vue 3 + Ollama** 的 AI 智能体平台，支持多轮对话、SSE 流式输出、RAG 知识库检索、ReAct 工具调用及多层失败兜底。

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?logo=openjdk" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring_Boot-3.5.16-brightgreen?logo=springboot" alt="Spring Boot 3.5">
  <img src="https://img.shields.io/badge/Vue-3.5-4fc08d?logo=vuedotjs" alt="Vue 3">
  <img src="https://img.shields.io/badge/Ollama-qwen2.5:7b-blue?logo=ollama" alt="Ollama">
  <img src="https://img.shields.io/badge/PostgreSQL-16-4169e1?logo=postgresql" alt="PostgreSQL 16">
</p>

---

## 目录

- [功能特性](#-功能特性)
- [系统架构](#-系统架构)
- [技术栈](#-技术栈)
- [快速开始](#-快速开始)
- [项目结构](#-项目结构)
- [配置说明](#-配置说明)
- [Agent 体系](#-agent-体系)
- [工具系统](#-工具系统)
- [前端说明](#-前端说明)
- [API 概览](#-api-概览)
- [Docker 部署](#-docker-部署)

---

## ✨ 功能特性

### 两个开箱即用的 Agent

| Agent | 引擎 | 特点 |
|-------|------|------|
| 🚗 **AI 选车大师** | `CarApp` + ChatClient | RAG 知识库检索、多轮对话、购车建议 |
| 🤖 **AI 超级智能体** (CcManus) | `ToolCallAgent` + ReAct | 工具调用、多步推理、自主规划 |

### 核心能力

- **💬 多轮对话** — 自动管理会话上下文与历史记录，支持对话持久化
- **⚡ SSE 流式输出** — 基于 Server-Sent Events 的 token 级实时推送，配合前端打字机效果
- **📚 RAG 知识库** — 本地向量存储（SimpleVectorStore），购车文档自动加载与检索
- **🔧 ReAct 工具调用** — think → act 循环，LLM 自主决定何时调用工具、调哪个工具
- **🛡️ 五层失败兜底** — LLM 重试 → 工具重试 → 能力域降级 → LLM 自主决策 → 熔断保护
- **🔐 JWT 认证** — 登录鉴权，前端路由守卫
- **📊 运营仪表盘** — 对话统计、Token 用量、活跃度分析
- **🎨 多图标选择** — 创建 Agent 时提供 5 大类 60+ 个 emoji 图标

---

## 🏗 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                       Browser (Vue 3)                        │
│  Login → Home → AgentChat / AgentManage / Dashboard          │
└──────────────────────────┬──────────────────────────────────┘
                           │ SSE / REST
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                 Spring Boot 3.5 (port 8123)                  │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │                    ChatRouterService                   │   │
│  │           根据 agentId 路由到对应 AI 引擎               │   │
│  └───────────────┬──────────────────────┬────────────────┘   │
│                  │                      │                    │
│                  ▼                      ▼                    │
│  ┌──────────────────────┐  ┌──────────────────────────┐     │
│  │   CarApp (选车大师)   │  │  CcManus (超级智能体)      │     │
│  │                      │  │                          │     │
│  │  ChatClient.stream() │  │  ReActAgent               │     │
│  │  + RAG Advisor       │  │    ├─ ToolCallAgent       │     │
│  │  + ChatMemory        │  │    │   ├─ think() ← Ollama │     │
│  └──────────────────────┘  │    │   ├─ act()  ← 8 tools │     │
│                            │    │   └─ fallback handler │     │
│                            │    └─ CcManus              │     │
│                            └──────────────────────────┘     │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │                  Shared Services                       │   │
│  │  AuthService │ AgentService │ ConversationService     │   │
│  │  MessageService │ StatisticsService                   │   │
│  └──────────────────────────────────────────────────────┘   │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              PostgreSQL 16 + pgvector                        │
│  agents │ conversations │ messages │ RAG vectors             │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                     Ollama (qwen2.5:7b)                      │
│                  http://localhost:11434                       │
└─────────────────────────────────────────────────────────────┘
```

---

## 🛠 技术栈

| 分类 | 技术 | 说明 |
|------|------|------|
| 运行环境 | Java 21 | Amazon Corretto |
| 后端框架 | Spring Boot 3.5.16 | Web + JDBC + Security |
| AI 框架 | Spring AI 1.0.0-M6 | ChatClient / ToolCallingManager / ChatMemory |
| 大模型 | **Ollama** `qwen2.5:7b` | 本地部署，无需云端 API |
| 数据库 | PostgreSQL 16 + pgvector | JdbcTemplate（无 ORM） |
| 前端框架 | Vue 3.5 + Vite 6 | Composition API + Vue Router 4 |
| HTTP 客户端 | Axios | 前端 API 层 |
| 工具库 | Hutool 5.8 | HTTP / JSON / 文件操作 |
| PDF 生成 | iText 7 | 中文 PDF 输出 |
| 网页抓取 | Jsoup | HTML 解析 |
| 认证 | JJWT 0.12 | JWT 签发与校验 |
| 容器化 | Docker + Compose | 一键部署 |

---

## 🚀 快速开始

### 前置要求

| 依赖 | 版本 | 用途 |
|------|------|------|
| Java | 21+ | 后端运行时 |
| Maven | 3.8+ | 后端构建 |
| Node.js | 18+ | 前端构建 |
| PostgreSQL | 16+ | 数据持久化 |
| Ollama | 最新 | 本地 LLM 推理 |

### 1. 启动 PostgreSQL

```bash
# Docker 方式（含 pgvector 扩展）
docker run -d --name ai-agent-db \
  -e POSTGRES_DB=yu_ai_agent \
  -e POSTGRES_USER=aiagent \
  -e POSTGRES_PASSWORD=changeme \
  -p 5432:5432 \
  pgvector/pgvector:pg16
```

### 2. 启动 Ollama

```bash
# 安装 Ollama（macOS）
brew install ollama

# 拉取模型
ollama pull qwen2.5:7b

# 启动服务（默认 http://localhost:11434）
ollama serve
```

### 3. 启动后端

```bash
cd ai-agent

# 开发环境（端口 8123，context-path=/api）
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 或生产环境
mvn clean package -DskipTests
java -jar target/ai-agent-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### 4. 启动前端

```bash
cd cc-ai-agent-frontend
npm install
npm run dev        # http://localhost:5173
```

### 5. 访问系统

| 地址 | 说明 |
|------|------|
| `http://localhost:5173` | 前端页面 |
| `http://localhost:8123/api` | 后端 API |
| 默认账号 | `admin` / `admin123` |

---

## 📁 项目结构

```
ai-agent/
├── pom.xml                          # Maven 配置
├── Dockerfile                       # 后端容器化
├── docker-compose.yml               # 全栈编排
├── CLAUDE.md                        # AI 编码指南
│
├── src/main/java/com/chm/aiagent/
│   ├── AiAgentApplication.java      # 启动类
│   │
│   ├── agent/                       # Agent 引擎
│   │   ├── BaseAgent.java           # 状态机 + runStream() 模板
│   │   ├── ReActAgent.java          # think → act 循环
│   │   ├── ToolCallAgent.java       # 工具调用 + 流式输出 + 失败兜底
│   │   ├── CcManus.java             # 超级智能体（prototype bean）
│   │   ├── model/AgentState.java    # IDLE→RUNNING→FINISHED/ERROR
│   │   └── fallback/                # 五层失败兜底
│   │       ├── CapabilityDomain.java
│   │       ├── ToolCapabilityRegistry.java
│   │       ├── ToolFailureContext.java
│   │       ├── ToolFallbackHandler.java
│   │       └── ToolFallbackProperties.java
│   │
│   ├── app/CarApp.java              # 选车大师（ChatClient 模式）
│   │
│   ├── tools/                       # 8 个 @Tool 注解工具
│   │   ├── ToolRegistration.java    # 工具注册中心
│   │   ├── WeatherTool.java         # 天气查询（wttr.in）
│   │   ├── WebSearchTool.java       # 网页搜索（searchapi.io）
│   │   ├── WebScrapingTool.java     # 网页抓取（Jsoup）
│   │   ├── FileOperationTool.java   # 文件读写
│   │   ├── ResourceDownloadTool.java# 资源下载
│   │   ├── TerminalOperationTool.java# 终端命令
│   │   ├── PDFGenerationTool.java   # PDF 生成（iText）
│   │   └── TerminateTool.java       # 终止对话
│   │
│   ├── rag/                         # RAG 知识库
│   │   ├── LoveAppDocumentLoader.java
│   │   ├── LoveAppVectorStoreConfig.java
│   │   ├── LoveAppRagCustomAdvisorFactory.java
│   │   └── MyKeywordEnricher.java
│   │
│   ├── auth/                        # 认证模块
│   │   ├── controller/AuthController.java
│   │   ├── service/AuthService.java
│   │   ├── service/JwtTokenProvider.java
│   │   ├── filter/JwtAuthFilter.java
│   │   └── config/SecurityConfig.java
│   │
│   ├── controller/                  # REST 接口
│   │   ├── AgentController.java     # Agent CRUD
│   │   ├── ConversationController.java # 会话 + SSE 对话
│   │   ├── StatisticsController.java
│   │   └── HealthController.java
│   │
│   ├── service/                     # 业务层
│   │   ├── AgentService.java
│   │   ├── ConversationService.java
│   │   ├── MessageService.java
│   │   ├── ChatRouterService.java   # Agent 路由
│   │   └── StatisticsService.java
│   │
│   ├── model/                       # 数据模型
│   ├── repository/                  # 数据访问（JdbcTemplate）
│   ├── dto/                         # 数据传输对象
│   ├── config/                      # Spring 配置
│   ├── exception/                   # 异常处理
│   └── common/Result.java           # 统一响应
│
├── src/main/resources/
│   ├── application.yaml             # 公共配置
│   ├── application-local.yaml       # 本地开发配置
│   ├── application-prod.yaml        # 生产配置
│   ├── schema.sql                   # 数据库初始化 DDL
│   └── documents/                   # RAG 知识库文档
│       ├── 购车问答-买车篇.md
│       ├── 购车问答-换车篇.md
│       └── 购车问答-增购篇.md
│
└── cc-ai-agent-frontend/            # Vue 3 前端
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── main.js                  # 入口
        ├── App.vue
        ├── router/index.js          # 路由配置
        ├── api/                     # API 层
        │   ├── request.js           # Axios 封装
        │   ├── auth.js
        │   ├── agent.js
        │   ├── chat.js
        │   └── dashboard.js
        ├── composables/useChat.js   # SSE 流式对话核心
        ├── components/
        │   ├── ChatRoom.vue         # 对话界面
        │   ├── TypewriterText.vue   # 打字机效果
        │   ├── StatCard.vue         # 统计卡片
        │   └── SiteFooter.vue
        └── views/
            ├── Login.vue            # 登录页
            ├── Home.vue             # Agent 入口
            ├── AgentChat.vue        # 对话页
            ├── AgentManage.vue      # Agent 管理
            └── Dashboard.vue        # 运营仪表盘
```

---

## ⚙️ 配置说明

### 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama 服务地址 |
| `OLLAMA_MODEL` | `qwen2.5:7b` | 使用的模型 |
| `DB_URL` | `jdbc:postgresql://localhost:5432/yu_ai_agent` | 数据库连接 |
| `DB_USERNAME` | `aiagent` | 数据库用户 |
| `DB_PASSWORD` | `changeme` | 数据库密码 |
| `SEARCH_API_KEY` | — | searchapi.io 密钥（网页搜索） |
| `JWT_SECRET` | 内置默认值 | JWT 签名密钥 |
| `JWT_EXPIRATION` | `86400000` (24h) | Token 过期时间（ms） |
| `SERVER_PORT` | `8123` | 后端端口 |

### Agent 失败兜底配置

```yaml
agent:
  tool-fallback:
    llm:
      max-retries: 3             # LLM 调用最大重试次数
      backoff-ms: [1000, 2000, 4000]  # 指数退避间隔
    tool:
      max-retries: 2             # 同工具最大重试次数
      timeout-seconds: 15        # 单次工具调用超时
    degradation:
      max-rounds: 2              # 最大降级轮次
      same-domain-only: true     # 仅限同能力域降级
    circuit-breaker:
      failure-threshold: 3       # 连续失败 N 次触发熔断
```

---

## 🧠 Agent 体系

### 类继承关系

```
BaseAgent                          # 状态机 + runStream() SSE 模板
  └── ReActAgent                   # think() → act() 循环
        └── ToolCallAgent          # 管理 8 个工具 + 流式输出 + 失败兜底
              └── CcManus          # 超级智能体（@Scope("prototype")）

CarApp                             # 选车大师（ChatClient + RAG + ChatMemory）
```

### SSE 协议

| 事件类型 | 含义 | 前端处理 |
|----------|------|---------|
| `event:thinking` | token 级流式文本 / 工具执行状态 | 追加到思考面板（折叠），打字机渲染 |
| `event:answer` | 最终回答文本 | 追加到主内容区 |
| `event:error` | 错误信息 | 展示错误提示 |

### 对话流程（超级智能体）

```
用户发送消息
  → ConversationController.chatStream()
    → ChatRouterService.route()
      → CcManus.runStream()
        ┌─────────────────────────────────┐
        │ for step in 1..maxSteps:         │
        │   think() → LLM 决定调哪个工具    │
        │     ├─ 有工具调用 → act() 执行     │
        │     │   ├─ 成功 → 继续             │
        │     │   └─ 失败 → 五层兜底         │
        │     └─ 无工具调用 → FINISHED       │
        │         → event:answer 推送       │
        └─────────────────────────────────┘
```

---

## 🔧 工具系统

### 8 个内置工具

| 工具 | 方法 | 能力域 | 需要外部服务 |
|------|------|--------|-------------|
| 🌤️ `WeatherTool` | `queryWeather(city)` | 信息获取 | wttr.in（免费） |
| 🔍 `WebSearchTool` | `searchWeb(query)` | 信息获取 | searchapi.io |
| 📄 `WebScrapingTool` | `scrapeWebPage(url)` | 信息获取 | — |
| 📁 `FileOperationTool` | `readFile` / `writeFile` | 文件操作 | — |
| 📥 `ResourceDownloadTool` | `downloadResource(url, name)` | 资源处理 | — |
| 📝 `PDFGenerationTool` | `generatePDF(name, content)` | 资源处理 | — |
| 💻 `TerminalOperationTool` | `executeTerminalCommand(cmd)` | 终端执行 | — |
| ⏹️ `TerminateTool` | `doTerminate()` | 流程控制 | — |

### 五层失败兜底

```
Layer 0: 参数预处理与校验
Layer 1: LLM 调用指数退避重试（1s → 2s → 4s）
Layer 2: 工具能力域内降级（天气失败 → 搜索 → 抓取）
Layer 3: LLM 自主决策备用方案
Layer 4: 降级耗尽，引导 LLM 以文本告知用户
Layer 5: 熔断保护（同工具连续失败 3 次 = 熔断）
```

---

## 🎨 前端说明

### 页面路由

| 路径 | 页面 | 说明 |
|------|------|------|
| `/login` | Login.vue | 登录页 |
| `/` | Home.vue | Agent 入口大厅 |
| `/chat/:agentId` | AgentChat.vue | SSE 流式对话 |
| `/agents` | AgentManage.vue | Agent CRUD 管理 |
| `/dashboard` | Dashboard.vue | 运营数据仪表盘 |

### 核心组件

| 组件 | 功能 |
|------|------|
| `ChatRoom.vue` | 消息列表 + 输入框 + SSE 连接管理 |
| `TypewriterText.vue` | 自适应打字机：落后 > 80 字符 → 8 chars/frame，> 40 → 4，> 10 → 2，默认 1 |
| `useChat.js` | SSE 解析状态机：sending → streaming → done / error |
| `request.js` | Axios 封装，自动携带 JWT token |

---

## 📡 API 概览

### 认证

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 登录获取 JWT Token |

### Agent 管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/agents` | Agent 列表 |
| POST | `/api/agents` | 创建 Agent |
| PUT | `/api/agents/{id}` | 更新 Agent |
| DELETE | `/api/agents/{id}` | 删除 Agent（car-advisor / super-agent 受保护） |

### 对话

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/conversations` | 创建会话 |
| GET | `/api/v1/conversations?agentId=` | 会话列表 |
| POST | `/api/v1/conversations/{id}/chat/stream` | **SSE 流式对话** |
| GET | `/api/v1/conversations/{id}/messages` | 历史消息 |
| POST | `/api/v1/conversations/{id}/generate-title` | AI 生成标题 |

### 统计

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/statistics/overview` | 概览统计 |

### 统一响应格式

```json
{
  "code": 0,
  "message": "ok",
  "data": { ... },
  "timestamp": 1783912040894,
  "success": true
}
```

---

## 🐳 Docker 部署

```bash
# 全栈启动（PostgreSQL + 后端）
docker compose up -d

# 查看日志
docker compose logs -f backend

# 重新构建并启动
docker compose up -d --build

# 停止
docker compose down
```

**服务端口**：
- 后端 API：`http://localhost:8123/api`
- PostgreSQL：`localhost:5432`

> **注意**：Docker Compose 不包含 Ollama 和前端。Ollama 需在宿主机运行，前端建议用 Nginx 部署 `npm run build` 产物。

---

## 📄 License

MIT

---

🤖 Generated with [Claude Code](https://claude.com/claude-code)
