# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Quality Workflow

每次生成/修改代码后，按顺序运行以下质量检查：

```
/code-review --fix     # 查找 Bug + 自动修复（必跑）
/simplify              # 精简重复代码、优化结构
/verify                # 启动项目端到端验证（有行为变更时必跑）
/security-review       # 安全检查（涉及认证/数据库/SQL 时跑）
```

## Project Identity

AI Agent 应用中心 — Spring Boot 3.5 + Vue 3 monorepo，基于 **Ollama**（qwen2.5:7b）本地大模型的智能对话平台。运行「AI 选车大师」和「AI 超级智能体」两个 Agent，支持 SSE 流式对话、RAG 知识库检索（pgvector）、ReAct 工具调用及五层失败兜底。

### 当前开发进度

| 阶段 | 状态 | 内容 |
|------|------|------|
| 一期 | ✅ 完成 | 项目骨架、两个 Agent、SSE 流式、8 个工具、ReAct 循环 |
| 二期 | ✅ 完成 | 五层工具失败兜底、熔断降级、README.md |
| **三期** | ✅ 刚完成 | 103 个单元测试、对话记忆 DB 持久化、pgvector 向量存储、JWT 安全加固 |
| 四期 | 📋 待开始 | Agent 配置落盘、对话历史深度集成、前端体验优化（暗色/移动端） |

### 三期关键变更（2026-07-13）

- **测试**：`src/test/java/.../fallback/` 下 4 个测试类，103 个用例，`mvn test` 全绿
- **记忆持久化**：`ChatRouterService.loadConversationHistory()` 每次请求从 DB 加载最近 20 条消息到 CcManus
- **pgvector**：`LoveAppVectorStoreConfig` 改为 PgVectorStore，需 `ollama pull nomic-embed-text`
- **安全**：`SecurityConfig` 启用 JWT 认证拦截（不再 permitAll），`users` 表 + BCrypt 密码，默认 admin/admin123
- **已知限制**：无（pgvector 0.8.5 已源码编译安装到 brew postgresql@16）

---

## Commands

### Backend (Spring Boot / Maven, Java 21)

```bash
# 开发启动（使用 application-local.yaml，端口 8123，context-path=/api）
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 打包
mvn clean package -DskipTests

# 运行 jar
java -jar target/ai-agent-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Frontend (Vue 3 + Vite)

```bash
cd cc-ai-agent-frontend
npm run dev        # 开发服务器（端口 5173，/api 代理到 localhost:8123）
npm run build      # 生产构建（含 sitemap.xml / robots.txt 自动生成）
```

### Docker Compose

```bash
docker compose up -d              # 全栈启动
docker compose up -d --build      # 重新构建镜像后启动
docker compose logs -f backend    # 查看后端日志
```

---

## Architecture Overview

### 整体拓扑

```
Browser → Vue 3 (Vite) → /api proxy → Spring Boot (8123)
                                         ├── ChatClient (CarApp)
                                         ├── ReActAgent → ToolCallAgent (CcManus)
                                         ├── SimpleVectorStore (RAG, 内存)
                                         └── Tools (WebSearch, FileOp, PDFGen, Terminal...)
```

### Agent 继承体系

```
BaseAgent                         # 状态机(IDLE→RUNNING→FINISHED/ERROR) + runStream() 模板
  └── ReActAgent                  # think() → act() 循环，step() 串联
        └── ToolCallAgent         # 管理工具调用：think() 解析 LLM 输出 → act() 执行工具并回写上下文
              └── CcManus         # 超级智能体（@Scope("prototype")，每次请求新实例，maxSteps=20）
```

`CarApp` 不走这个继承链，直接使用 Spring AI `ChatClient.Builder` + `InMemoryChatMemory` 构建。

### 请求链路（SSE 流式对话）

**CarApp 路径（选车大师）：**
```
Vue ChatRoom → useChat.js → fetch GET /api/ai/car/sse?message=&chatId=
  → CarController.sse() → CarApp.doChatStream()
    → ChatClient.stream().chatResponse()
      → Flux<ChatResponse> → SseEmitter → 逐 token 推送
```

**CcManus 路径（超级智能体）：**
```
Vue ChatRoom → useChat.js → fetch GET /api/ai/car/manus/chat?message=
  → CarController.doChatWithManus() → CcManus.runStream()
    → think()→act() 循环 → event:thinking / event:answer 分流推送
```

### SSE 前后端协议

| SSE event 类型 | 含义 | 前端处理 |
|---|---|---|
| `event:answer` | 最终回答，直接展示给用户 | 追加到 `content` |
| `event:thinking` | 内部思考/工具调用状态 | 追加到 `thoughts`（折叠展示） |
| `event:error` | 错误信息 | 展示错误提示 |

---

## Key Modules

### RAG 知识库

- **向量存储**：`SimpleVectorStore`（内存），由 `LoveAppVectorStoreConfig` 在启动时创建
- **文档源**：`src/main/resources/documents/*.md`（购车问答-买车篇/换车篇/增购篇）
- **加载流程**：`LoveAppDocumentLoader` 读取 markdown → `MyKeywordEnricher` 增强元数据 → 写入向量库
- **检索**：`LoveAppRagCustomAdvisorFactory` 按 `filename` metadata 过滤，topK=5
- **当前仅 `doChatWithRag()` 方法启用了本地 RAG**，通过 `CarApp` 的 `@Resource loveAppVectorStore` 注入

> 注：`LoveAppRagCloudAdvisorConfig`（DashScope 云知识库）和 `PgVectorVectorStoreConfig`（PostgreSQL 向量存储）的 Bean 和调用代码当前均为注释状态，供后续按需启用。

### 工具注册

`ToolRegistration.allTools()` 注册了 7 个 `@Tool` 注解的工具：

| 工具 | 类 | 能力 |
|---|---|---|
| 网页搜索 | `WebSearchTool` | 调用 searchapi.io 检索百度结果（需 `SEARCH_API_KEY`） |
| 网页抓取 | `WebScrapingTool` | Jsoup 抓取网页正文 |
| 文件操作 | `FileOperationTool` | 读写 `tmp/file/` 下的文件 |
| 资源下载 | `ResourceDownloadTool` | 下载 URL 资源到 `tmp/download/` |
| 终端命令 | `TerminalOperationTool` | 执行本地 shell 命令 |
| PDF 生成 | `PDFGenerationTool` | iText 生成 PDF 到 `tmp/pdf/` |
| 终止对话 | `TerminateTool` | Agent 主动结束任务 |

所有工具通过 `ToolCallbacks.from()` 聚合为 `ToolCallback[]` Bean，`CarApp` 和 `CcManus` 均可注入。

### 对话记忆

- 当前使用 `InMemoryChatMemory`（内存，重启丢失）
- 备选 `FileBasedChatMemory`（Kryo 序列化到 `tmp/chat-memory/{chatId}.kryo`），已实现未启用
- `chatId` 由前端生成（UUID），通过 `CHAT_MEMORY_CONVERSATION_ID_KEY` 参数传递

### 认证

- Spring Security + JJWT，`JwtAuthFilter` 已注册到过滤器链
- `AuthService` 硬编码了单用户（admin/admin123），`POST /auth/login` 返回 JWT token
- `SecurityConfig` 当前配置为 `anyRequest().permitAll()`（放行所有请求，开发阶段）
- 前端 `request.js`（axios）和 `chat.js`（fetch）均携带 `Authorization: Bearer <token>` Header
- 前端路由守卫 `router.beforeEach` 检查 `isLoggedIn()`，未登录跳转 Login 页

### 全局异常处理

`GlobalExceptionHandler`（`@RestControllerAdvice`）拦截所有异常，转为 `Result<T>` 统一响应：`{ code, message, data, timestamp }`。`Result` 位于 `com.chm.aiagent.common.Result`。

---

## Code Patterns（新增功能时遵循）

### 1. 新增 Agent

1. 后端创建类（继承 `ToolCallAgent` 或参照 `CarApp` 用 ChatClient）
2. 前端新建 `views/XxxChat.vue` + 路由注册 + `Home.vue` 添加入口卡片
3. `chat.js` 添加对应 SSE 调用函数

### 2. 新增 REST 接口

- 统一返回 `Result<T>`，成功用 `Result.ok(data)`，失败抛 `BusinessException(ErrorCode.xxx)`
- 返回列表时走分页：`{ list: [...], total: N, page: 1, size: 20 }`

### 3. 新增配置项

- 敏感值走环境变量：`${VAR_NAME:defaultValue}`，不要硬编码密钥
- 各环境差异化配置分别在 `application-{profile}.yaml` 中覆盖
- `application.yaml` 只放公共配置

---

## 环境变量速查

| 变量 | 说明 | 必填 |
|---|---|---|
| `DASHSCOPE_API_KEY` | 通义千问 API Key | 是 |
| `SEARCH_API_KEY` | searchapi.io Key（网页搜索工具用） | 否 |
| `JWT_SECRET` | JWT 签名密钥 | 否（有默认值，生产需更换） |
| `JWT_EXPIRATION` | JWT 过期时间（毫秒） | 否（默认 86400000） |
| `SERVER_PORT` | 后端端口 | 否（默认 8123） |
