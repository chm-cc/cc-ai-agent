# AI Agent

基于 Spring AI + DashScope 的 AI 智能选车顾问系统，支持多轮对话、RAG 知识库检索、工具调用和 MCP 协议。

## ✨ 功能特性

- **多轮对话**：支持上下文记忆的智能对话，自动管理对话历史
- **SSE 流式输出**：基于 Server-Sent Events 实现打字机效果的实时响应
- **RAG 知识库检索**：混合架构，支持本地向量存储（SimpleVectorStore）和云端知识库（DashScope）双通道检索
- **工具调用**：AI 可自主判断并调用多种工具辅助回答
    - 🌐 网页搜索与抓取
    - 📄 PDF 文档生成
    - 📁 文件读写操作
    - 💻 终端命令执行
    - 📥 资源下载
- **MCP 协议支持**：通过 Model Context Protocol 扩展外部服务能力（如图片搜索 MCP Server）
- **PgVector 向量数据库**：支持基于 PostgreSQL + PgVector 的文档向量化存储与检索

## 🛠 技术栈

| 分类 | 技术 |
|------|------|
| 运行环境 | Java 21 |
| 基础框架 | Spring Boot 3.5 |
| AI 框架 | Spring AI 1.0.0-M6 + Spring AI Alibaba |
| 大模型 | DashScope（通义千问）/ Ollama |
| 向量数据库 | PgVector (PostgreSQL) |
| AI 工具链 | LangChain4j |
| 协议标准 | MCP (Model Context Protocol) |
| API 文档 | Knife4j + SpringDoc OpenAPI |
| 工具库 | Hutool、Jsoup、iTextPDF |

## 📋 环境要求

- **Java** 21+
- **Maven** 3.8+
- **PostgreSQL**（需安装 PgVector 扩展，用于 RAG 向量检索）
- **DashScope API Key**（[申请地址](https://dashscope.console.aliyun.com/)）

## 🚀 快速开始

### 1. 克隆项目

