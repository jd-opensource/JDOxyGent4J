# JDOxyGent4J 示例索引（按目录分类）

本文档仅保留示例名称与能力描述，按目录分组，便于快速查阅。  
samples所在位置：[samples/](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/)

## advanced（高级功能）

| 示例                                                                                                                                  | 能力                                                 |
|-------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------|
| [DemoContinueExec](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/advanced/DemoContinueExec.java)                     | 断点续传与重新生成；从指定节点恢复执行                                |
| [DemoCustomAgentInputSchema](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/advanced/DemoCustomAgentInputSchema.java) | 自定义输入结构；支持复杂结构化参数传递                                |
| [DemoMultimodal](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/advanced/DemoMultimodal.java)                         | 多模态输入（图片/视频/URL/Base64）；启用 is_multimodal_supported |
| [DemoMultimodalNew.java](src/main/java/com/jd/oxygent/core/oxygent/samples/examples/advanced/DemoMultimodalNew.java)                | 多模态输入（图片/视频/URL/Base64）；图片分析                       |
| [DemoMultimodalTransfer](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/advanced/DemoMultimodalTransfer.java)         | 智能体间多模态数据传递；自动生成可访问链接                              |
| [DemoSendMessageFromTool](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/advanced/DemoSendMessageFromTool.java)       | 工具内主动发送消息；实时进度反馈与可观测性                              |
| [DemoTrustMode](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/advanced/DemoTrustMode.java)                           | 信任模式（原始输出）；跳过 LLM 二次解析与润色                          |

## agent（基础与多智能体架构）

| 示例                                                                                                                        | 能力                                         |
|---------------------------------------------------------------------------------------------------------------------------|--------------------------------------------|
| [DemoSingleAgent](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/agent/DemoSingleAgent.java)                | 最简单的单智能体系统；单个 ReActAgent 调用工具              |
| [DemoReactAgent](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/agent/DemoReactAgent.java)                  | ReAct 推理循环：思考 → 行动 → 观察 → 再思考              |
| [DemoChatAgentStream](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/agent/DemoChatAgentStream.java)        | 对话型智能体 + SSE 流式输出；适合聊天场景                   |
| [DemoWorkflowAgent](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/agent/DemoWorkflowAgent.java)            | 使用 WorkflowAgent 执行工作流；显式控制顺序与数据流          |
| [DemoHeterogeneousAgents](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/agent/DemoHeterogeneousAgents.java) | 异构智能体协作（ReAct/Chat/MCP）；各司其职协同工作           |
| [DemoHierarchicalAgents](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/agent/DemoHierarchicalAgents.java)  | 分层 master-sub 架构；权限管理与调用链路追踪               |
| [DemoRagAgent](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/agent/DemoRagAgent.java)                      | 检索增强生成（RAG）；向量检索增强回答                       |
| [DemoSseAgent](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/agent/DemoSseAgent.java)                      | 访问SSE流式输出的server                           |
| [DemoTeamSizeAgent.java](src/main/java/com/jd/oxygent/core/oxygent/samples/examples/agent/DemoTeamSizeAgent.java)                     | 在智能体中自定义funcProcessInput和funcProcessOutput方法 |
| [EvaluateAndEvolveDemo.java](src/main/java/com/jd/oxygent/core/oxygent/samples/examples/agent/EvaluateAndEvolveDemo.java)                     | 通过数据处理与分析来评估并提升智能体的性能                      |
| [ParallelDemo.java](src/main/java/com/jd/oxygent/core/oxygent/samples/examples/agent/ParallelDemo.java)                    | 智能体并行执行                                    |

## application（mcp服务提供方）

| 示例                                                                                                                                   | 能力                                   |
|--------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------|
| [BankManagerByApiRouter.java](src/main/java/com/jd/oxygent/core/oxygent/samples/examples/applications/BankManagerByApiRouter.java)   | 资产库远程服务，手动保存资产库内容，自动获取所有api的endpoint |
| [BankManagerByBankRouter.java](src/main/java/com/jd/oxygent/core/oxygent/samples/examples/applications/BankManagerByBankRouter.java) | 资产库远程服务，用大模型保存资产库内容 |
| [BankManagerByManualApi.java](src/main/java/com/jd/oxygent/core/oxygent/samples/examples/applications/BankManagerByManualApi.java)   | 资产库远程服务，手动保存资产库内容，手动提供所有api的endpoint |


## backend（路由、附件、并发、配置、启动与日志）

| 示例                                                                                                                              | 能力                                        |
|---------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------|
| [DemoAddRouter](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/backend/DemoAddRouter.java)                        | 动态路由注册；扩展 Web 服务 API 端点                   |
| [DemoAttachment](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/backend/DemoAttachment.java)                      | 附件处理；支持图片/视频上传与传递                         |
| [DemoBatchAndSemaphore](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/backend/DemoBatchAndSemaphore.java)        | 批处理与并发控制；信号量限制并发请求数                       |
| [DemoConfig](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/backend/DemoConfig.java)                              | 配置系统；数据库/LLM/环境变量管理，支持多环境                 |
| [DemoCustomHeader.java](src/main/java/com/jd/oxygent/core/oxygent/samples/examples/backend/DemoCustomHeader.java)               | 自定义大模型请求的header数据                         |
| [DemoDataScope](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/backend/DemoDataScope.java)                        | 数据作用域管理：请求/会话/组级别存储与访问                    |
| [DemoGlobalData](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/backend/DemoGlobalData.java)                      | 全局数据共享；在所有智能体间同步状态                        |
| [DemoHumanInTheLoop.java](src/main/java/com/jd/oxygent/core/oxygent/samples/examples/backend/DemoHumanInTheLoop.java)           | 手动发送反馈，阻塞获取                               |
| [DemoLaunchMas](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/backend/DemoLaunchMas.java)                        | MAS 系统启动；CLI/Web/编程三种模式初始化                |
| [DemoLoggerSetup](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/backend/DemoLoggerSetup.java)                    | 日志系统配置；自定义格式与级别，追踪 trace_id/node_id       |
| [DemoMasFunction.java](src/main/java/com/jd/oxygent/core/oxygent/samples/examples/backend/DemoMasFunction.java)                 | 自定义Mas.funcProcessMessageBody方法，用于自定义处理消息 |
| [DemoProcessLlmException.java](src/main/java/com/jd/oxygent/core/oxygent/samples/examples/backend/DemoProcessLlmException.java) | 自定义大模型报错时的方法，用于自定义处理异常                    |
| [DemoProcessMessage.java](src/main/java/com/jd/oxygent/core/oxygent/samples/examples/backend/DemoProcessMessage.java)           | 自定义Mas.funcProcessMessage方法，用于自定义处理消息体字段  |
| [DemoSaveMessage.java](src/main/java/com/jd/oxygent/core/oxygent/samples/examples/backend/DemoSaveMessage.java)                 | 自定义Mas.funcProcessInput方法，用于自定义消息的保存和发送策略 |
| [DemoDisableMessage.java](src/main/java/com/jd/oxygent/core/oxygent/samples/examples/backend/DemoDisableMessage.java)           | 不发送think,observation,tool_call，用于加快响应速度   |

## banks (资产库)

| 示例                                                                                                                                            | 能力                                                                                       |
|-----------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------|
| [DemoBankChatAgentDumpMemory.java](src/main/java/com/jd/oxygent/core/oxygent/samples/examples/banks/DemoBankChatAgentDumpMemory.java) | 根据资产库的内容，用修改系统提示词的方式，更新大模型的知识，并将更新立刻反映在新的提问中（ChatAgent示例），需要启动BankManagerByApiRouter服务   |
| [DemoBankReactAgentRigid.java](src/main/java/com/jd/oxygent/core/oxygent/samples/examples/banks/DemoBankReactAgentRigid.java) | 根据资产库的内容，用修改系统提示词的方式，更新大模型的知识，并将更新立刻反映在新的提问中（ReActAgent示例） ，需要启动BankManagerByApiRouter服务 |
| [DemoBankReactAgentAutonomy.java](src/main/java/com/jd/oxygent/core/oxygent/samples/examples/banks/DemoBankReactAgentAutonomy.java) | 根据资产库的内容，更新大模型的知识，并将更新立刻反映在新的提问中（和其他MCP工具一起使用），需要启动BankManagerByApiRouter服务 |
| [DemoBankReactAgentAutonomyByMCP.java](src/main/java/com/jd/oxygent/core/oxygent/samples/examples/banks/DemoBankReactAgentAutonomyByMCP.java) | 根据资产库的内容，更新大模型的知识，并将更新立刻反映在新的提问中（资产库本身作为MCP工具调用），需要启动BankManagerByApiRouter服务|

## distributed（分布式与多节点协同）

| 示例                                                                                                             | 能力 |
|----------------------------------------------------------------------------------------------------------------| - |
| [AppMasterAgent](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/distributed/AppMasterAgent.java) | 分布式与多节点协同；支持 Python/Java 互相调用 |
| [AppMathAgent](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/distributed/AppMathAgent.java)     | 分布式与多节点协同；支持 Python/Java 互相调用 |
| [AppTimeAgent](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/distributed/AppTimeAgent.java)   | 分布式与多节点协同；支持 Python/Java 互相调用 |

## flows（流程编排）

| 示例                                                                                                              | 能力 |
|-----------------------------------------------------------------------------------------------------------------| - |
| [DemoPlanAndSolve](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/flows/DemoPlanAndSolve.java)    | Plan-and-Solve 两阶段：规划与执行；支持 enable_replanner |
| [DemoReflexionFlow](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/flows/DemoReflexionFlow.java) | Reflexion 反思机制；自我评估与改进响应质量 |

## livePrompt
| 示例 | 能力      |
|----|---------|
| [DemoLivePrompt.java](src/main/java/com/jd/oxygent/core/oxygent/samples/examples/liveprompt/DemoLivePrompt.java)   | 动态修改提示词 |

## llms (大模型)
| 示例                                                                                                                           | 能力                                      |
|------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------|
| [DemoDisableSystemPrompt.java](src/main/java/com/jd/oxygent/core/oxygent/samples/examples/llms/DemoDisableSystemPrompt.java) | 大模型禁用系统提示词                              |
| [DemoHttpVersion.java](src/main/java/com/jd/oxygent/core/oxygent/samples/examples/llms/DemoHttpVersion.java)                 | 大模型网关在不支持http/2的情况下，需要指定http版本为http/1.1 |

## mcptools (MCP工具)
| 示例 | 能力                                         |
|----|--------------------------------------------|
|  [DemoBrowser.java](src/main/java/com/jd/oxygent/core/oxygent/samples/examples/mcptools/DemoBrowser.java)  | 网页自动抓取，Browser Use能力，使用chrome-devtools-mcp |

## tools（工具与 MCP 集成）

| 示例                                                                                                                             | 能力                                                   |
|--------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------|
| [DemoFunctionHub](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/tools/DemoFunctionHub.java)                     | 使用 FunctionHub 将普通函数注册为工具；参数类型校验                     |
| [DemoFunctionHubAnnotation](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/tools/DemoFunctionHubAnnotation.java) | 注解式工具注册；简化工具声明与注入                                    |
| [DemoMCP](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/tools/DemoMCP.java)                                     | MCP 协议工具集成（Stdio/SSE/Streamable）；支持本地与远程             |
| [DemoMCPToolAuthorization](./src/main/java/com/jd/oxygent/core/oxygent/samples/examples/tools/DemoMCPToolAuthorization.java)                                     | MCP 协议工具集成（Stdio/SSE/Streamable）；支持本地与远程，Header带认证信息 |
