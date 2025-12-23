# OxyGent中文how-to指南

> 本系列文档将指导您使用OxyGent逐步搭建MAS（多智能体系统）

---

## 快速开始

+ [安装OxyGent](./00-01-install.md)
+ [运行demo](./00-02-demo.md)
+ [创建第一个智能体](./01-01-register_single_agent.md)
+ [和智能体交流](./01-02-chat_with_agent.md)

## 核心功能

### 智能体管理
+ [选择智能体使用的LLM](./01-03-select_llm.md)
+ [预设提示词](./01-04-select_prompt.md)
+ [选择智能体种类](./01-05-select_agent.md) - ChatAgent、ReActAgent等类型对比

### 工具系统
+ [注册一个工具](./02-01-register_single_tool.md)
+ [管理工具调用](./02-02-manage_tools.md)
+ [使用MCP开源工具](./02-03-use_opensource_tools.md)
+ [使用MCP自定义工具](./02-04-use_mcp_tools.md)

### 系统配置
+ [设置OxyGent Config](./03-01-set_config.md)
+ [设置数据库](./03-02-set_database.md)
+ [设置全局数据](./03-03-set_global.md)

## 高级功能

### 多智能体系统
+ [创建简单的多agent系统](./06-01-register_multi_agent.md)
+ [复制相同智能体](./06-02-moa.md)

### 性能与处理
+ [并行调用agent](./07-01-parallel.md)
+ [提供响应元数据](./08-01-trust_mode.md)
+ [处理LLM和智能体输出](./08-02-handle_output.md)

### 智能化功能
+ [反思重做模式](./08-03-reflexion.md) - Reflexion自我反思机制
+ [处理查询和提示词](./08-04-update_prompts.md)

### 流程控制
+ [获取记忆和重新生成](./09-01-continue_exec.md)
+ [创建流](./09-02-preset_flow.md) - PlanAndSolve等内置流程
+ [创建工作流](./09-03-workflow.md)

### 专项功能
+ [使用多模态智能体](./10-01-multimodal.md)
+ [处理附件和文件](./10-02-attachment_processing.md)
+ [创建分布式系统](./11-01-distributed.md)
+ [检索增强生成(RAG)](./12-01-rag.md)

## 开发工具

+ [可视化界面调试](./13-01-debugging.md)
+ [Web API接口](./14-01-web_api.md)

---

## 相关资源

- [项目主页](../../README_zh.md)
- [示例代码](../../oxygent-studio/src/main/java/com/jd/oxygent/examples/)