# JDOxyGent4J Samples Index (English)

This document summarizes examples under [samples/](src/main/java/com/jd/oxygent/core/oxygent/samples/) .
It only lists sample names and capabilities, grouped by directory for quick scanning.

## agent (basic and multi-agent architecture)

| Sample | Capability |
| - | - |
| demo_single_agent | Smallest single-agent system; a single ReActAgent calling tools |
| demo_react_agent (supported) | ReAct reasoning loop: Think → Act → Observe → Reflect |
| demo_chat_agent_stream (supported) | Chat agent with SSE streaming output; ideal for chatbot scenarios |
| demo_workflow_agent | WorkflowAgent executes custom workflows; explicit call order and data flow |
| demo_heterogeneous_agents | Heterogeneous agents collaboration (ReAct/Chat/MCP) |
| demo_hierarchical_agents | Hierarchical master–sub agents; permissions and call-chain tracing |
| demo_rag_agent (supported) | Retrieval-Augmented Generation (RAG); vector search to enhance answers |

## tools (Tool Hub and MCP integration)

| Sample | Capability |
| - | - |
| demo_functionhub (supported) | Register functions as tools via FunctionHub; decorator-style registration; type validation |
| demo_functionhub_annotation | Annotation-based tool registration; simplified declaration and injection |
| demo_mcp | MCP protocol tools via Stdio/SSE/Streamable clients; supports local and remote MCP servers |

## advanced (advanced features)

| Sample | Capability |
| - | - |
| advanced/demo_continue_exec | Resume from a specific node and regenerate; helpful for debugging and iteration |
| demo_custom_agent_input_schema | Custom agent input schema; structured parameter passing |
| demo_multimodal | Multimodal input; enable `is_multimodal_supported`; attachments via URL/Base64/images/videos |
| demo_multimodal_transfer | Cross-agent multimodal data transfer; auto-generate accessible web links |
| demo_send_message_from_tool | Tools push intermediate messages via `send_message()`; real-time progress and observability |
| demo_trust_mode | Trust mode returns raw tool output; skip LLM post-processing |
| demo_save_message | Fine-grained message persistence control (e.g., Elasticsearch); optimize storage cost |

## flows (orchestration flows)

| Sample | Capability |
| - | - |
| plan_and_solve_demo | Two-phase Plan-and-Solve: planner builds a plan, executor follows it; optional replanning; structured output parsing |
| reflexion_agent_demo | Reflexion mechanism: self-evaluation and improvement; feedback when response quality is insufficient |

## backend (routes, attachments, concurrency, config, bootstrap and logging)

| Sample | Capability |
| - | - |
| demo_add_router | Dynamically register routes; extend web service API endpoints |
| demo_attachment | Attachment handling; file upload and pass-through; images/videos; path and URL conversions |
| demo_batch_and_semaphore | Batch processing and concurrency control; semaphore-limited parallelism; throughput optimization |
| demo_config | Config system usage; DB/LLM/env variable management; multi-environment support |
| demo_data_scope | Data scopes: request (`arguments`), session (`shared_data`), group (`group_data`) |
| demo_global_data | Global data shared across agents; `get_global_data()` and `set_global_data()` |
| demo_launch_mas | MAS system bootstrap; component registration, DB initialization, organization graph; CLI/Web/Programmatic modes |
| demo_logger_setup | Logging configuration; custom formats and levels; trace `trace_id` and `node_id` for request tracing |

## distributed (Distributed and multi-node collaboration)

| Sample | Capability |
| - | - |
| demo_distributed_mas | Distributed and multi-node collaboration; supports Python↔Java interop calls |

---
