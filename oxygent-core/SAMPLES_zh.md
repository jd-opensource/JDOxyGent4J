# JDOxyGent4J 示例索引（按目录分类）

本文档仅保留示例名称与能力描述，按目录分组，便于快速查阅。  
samples所在位置：[samples/](src/main/java/com/jd/oxygent/core/oxygent/samples/)

## agent（基础与多智能体架构）

| 示例 | 能力 |
| - | - |
| demo_single_agent | 最简单的单智能体系统；单个 ReActAgent 调用工具 |
| demo_react_agent | ReAct 推理循环：思考 → 行动 → 观察 → 再思考 |
| demo_chat_agent_stream | 对话型智能体 + SSE 流式输出；适合聊天场景 |
| demo_workflow_agent | 使用 WorkflowAgent 执行工作流；显式控制顺序与数据流 |
| demo_heterogeneous_agents | 异构智能体协作（ReAct/Chat/MCP）；各司其职协同工作 |
| demo_hierarchical_agents | 分层 master-sub 架构；权限管理与调用链路追踪 |
| demo_rag_agent | 检索增强生成（RAG）；向量检索增强回答 |

## tools（工具与 MCP 集成）

| 示例 | 能力 |
| - | - |
| demo_functionhub | 使用 FunctionHub 将普通函数注册为工具；参数类型校验 |
| demo_functionhub_annotation | 注解式工具注册；简化工具声明与注入 |
| demo_mcp | MCP 协议工具集成（Stdio/SSE/Streamable）；支持本地与远程 |

## advanced（高级功能）

| 示例 | 能力 |
| - | - |
| advanced/demo_continue_exec | 断点续传与重新生成；从指定节点恢复执行 |
| demo_custom_agent_input_schema | 自定义输入结构；支持复杂结构化参数传递 |
| demo_multimodal | 多模态输入（图片/视频/URL/Base64）；启用 is_multimodal_supported |
| demo_multimodal_transfer | 智能体间多模态数据传递；自动生成可访问链接 |
| demo_send_message_from_tool | 工具内主动发送消息；实时进度反馈与可观测性 |
| demo_trust_mode | 信任模式（原始输出）；跳过 LLM 二次解析与润色 |

## flows（流程编排）

| 示例 | 能力 |
| - | - |
| plan_and_solve_demo | Plan-and-Solve 两阶段：规划与执行；支持 enable_replanner |
| reflexion_agent_demo | Reflexion 反思机制；自我评估与改进响应质量 |

## backend（路由、附件、并发、配置、启动与日志）

| 示例 | 能力 |
| - | - |
| demo_add_router | 动态路由注册；扩展 Web 服务 API 端点 |
| demo_attachment | 附件处理；支持图片/视频上传与传递 |
| demo_batch_and_semaphore | 批处理与并发控制；信号量限制并发请求数 |
| demo_config | 配置系统；数据库/LLM/环境变量管理，支持多环境 |
| demo_data_scope | 数据作用域管理：请求/会话/组级别存储与访问 |
| demo_global_data | 全局数据共享；在所有智能体间同步状态 |
| demo_launch_mas | MAS 系统启动；CLI/Web/编程三种模式初始化 |
| demo_logger_setup | 日志系统配置；自定义格式与级别，追踪 trace_id/node_id |

## distributed（分布式与多节点协同）

| 示例 | 能力 |
| - | - |
| demo_distributed_mas | 分布式与多节点协同；支持 Python/Java 互相调用 |