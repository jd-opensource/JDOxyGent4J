# Flows API
---

> OxyGent4J 流程编排智能体参考文档：PlanAndSolveAgent 与 WorkflowAgent。

---

## PlanAndSolveAgent

**包路径**: `com.jd.oxygent.core.oxygent.oxy.agents`

### 简介

`PlanAndSolveAgent` 实现"规划-执行-总结"三阶段范式，将复杂任务先分解为可执行计划，再逐步执行，最终汇总生成完整答案。

### 类层次结构

```
BaseOxy → BaseFlow → BaseAgent → LocalAgent → PlanAndSolveAgent
```

### 参数

| 参数 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| `maxReplanRounds` | `int` | `30` | 最大重规划轮次，防止无限循环 |
| `plannerAgent` | `String` | `"planner_agent"` | 负责生成计划的智能体名称 |
| `executorAgent` | `String` | `"executor_agent"` | 负责执行具体任务的智能体名称 |

> 同时继承 `LocalAgent` 的所有参数（`llmModel`、`prompt`、`tools` 等）。

### 执行流程

```
用户查询 → [Planner 生成计划] → [Executor 逐步执行]
                ↑                         ↓
                └── 动态重规划（如需要） ──┘
                                          ↓
                              [汇总结果 → 返回答案]
```

### 方法

| 方法 | 返回值 | 描述 |
|------|--------|------|
| `init()` | `void` | 初始化并注册 planner/executor 为许可工具 |
| `_execute(OxyRequest)` | `OxyResponse` | 完整的规划-执行-总结循环 |

### 使用示例

```java
// 1. 定义 Planner Agent
ChatAgent planner = ChatAgent.builder()
    .name("planner_agent")
    .llmModel("default_llm")
    .prompt("针对给定目标，创建简洁的分步执行计划...")
    .build();

// 2. 定义 Executor Agent
ReActAgent executor = ReActAgent.builder()
    .name("executor_agent")
    .llmModel("default_llm")
    .tools(List.of("web_search", "calculator"))
    .build();

// 3. 定义 PlanAndSolve Agent
PlanAndSolveAgent solver = PlanAndSolveAgent.builder()
    .name("problem_solver")
    .maxReplanRounds(20)
    .plannerAgent("planner_agent")
    .executorAgent("executor_agent")
    .llmModel("default_llm")
    .build();

OxyResponse response = solver.execute(
    OxyRequest.builder().query("分析竞争对手产品并给出改进建议").build()
);
```

---

## WorkflowAgent

**包路径**: `com.jd.oxygent.core.oxygent.oxy.agents`

### 简介

`WorkflowAgent` 通过 `Function<OxyRequest, Object>` 接口实现自定义工作流逻辑。适合需要精确流程控制的场景，如业务审批、数据处理管道等。

### 类层次结构

```
BaseOxy → BaseFlow → BaseAgent → LocalAgent → WorkflowAgent
```

### 参数

| 参数 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| `funcWorkflow` | `Function<OxyRequest, Object>` | `null` | 工作流执行函数（必填） |

> 同时继承 `LocalAgent` 的所有参数。

### 方法

| 方法 | 返回值 | 描述 |
|------|--------|------|
| `_execute(OxyRequest)` | `OxyResponse` | 调用 `funcWorkflow` 并包装结果 |

### 使用示例

```java
WorkflowAgent orderAgent = WorkflowAgent.builder()
    .name("order_workflow")
    .desc("订单处理工作流")
    .llmModel("default_llm")
    .funcWorkflow(request -> {
        Map<String, Object> args = request.getArguments();
        String orderId = (String) args.get("orderId");

        // Step 1: 验证订单
        if (!validateOrder(orderId)) {
            throw new IllegalArgumentException("订单验证失败");
        }

        // Step 2: 检查库存
        if (!checkInventory(orderId)) {
            return "库存不足";
        }

        // Step 3: 执行扣款
        processPayment(orderId);

        return "订单处理成功: " + orderId;
    })
    .build();
```

### 设计原则

- 幂等性：多次执行产生相同结果
- 可观测性：提供详细的执行日志
- 容错性：优雅处理异常情况
- 可测试性：支持单元测试和集成测试

### 适用场景

- 业务流程自动化（订单处理、审批流程）
- 数据处理管道（ETL、数据转换）
- 多系统集成（数据同步与协调）
- 批量处理任务
