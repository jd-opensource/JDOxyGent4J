# Schemas API
---

> OxyGent4J 核心数据模型参考文档：OxyRequest、OxyResponse 和 OxyState。

---

## OxyRequest

**包路径**: `com.jd.oxygent.core.oxygent.schemas.oxy`

任务执行请求的包装类，包含路由、执行和追踪请求所需的全部信息。

### 核心字段

| 字段 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| `requestId` | `String` | UUID 自动生成 | 请求唯一标识 |
| `groupId` | `String` | UUID 自动生成 | 请求组标识 |
| `fromTraceId` | `String` | `null` | 来源追踪 ID |
| `currentTraceId` | `String` | UUID 自动生成 | 当前执行链追踪 ID |
| `rootTraceIds` | `List<String>` | `[]` | 根追踪 ID 列表 |
| `caller` | `String` | `"user"` | 发起请求的调用者名称 |
| `callee` | `String` | — | 目标执行者（Agent/Tool）名称 |
| `query` | `String` | — | 用户查询内容 |
| `arguments` | `Map<String, Object>` | `{}` | 执行参数字典 |
| `sharedData` | `Map<String, Object>` | `{}` | 跨 Agent 共享数据 |

### 关键方法

| 方法 | 返回值 | 描述 |
|------|--------|------|
| `call(Map<String, Object> kwargs)` | `OxyResponse` | 调用其他 Agent/Tool（核心路由方法） |
| `getQuery()` | `String` | 获取查询内容 |
| `getShortMemory(boolean isMaster)` | `List<Map>` | 获取短期记忆 |
| `sendMessage(Map<String, Object>)` | `void` | 发送流式消息（SSE） |
| `getOxy(String name)` | `BaseOxy` | 从 MAS 中获取组件实例 |
| `getSessionName()` | `String` | 获取当前会话名称 |
| `clone()` | `OxyRequest` | 深拷贝请求对象 |

### 使用示例

```java
OxyRequest request = OxyRequest.builder()
    .query("帮我查询北京的天气")
    .arguments(Map.of("city", "北京"))
    .build();

// 在 Agent 内部调用其他组件
OxyResponse toolResult = request.call(Map.of(
    "callee", "weather_tool",
    "arguments", Map.of("city", "北京")
));
```

---

## OxyResponse

**包路径**: `com.jd.oxygent.core.oxygent.schemas.oxy`

任务执行响应的包装类，封装执行结果、状态和元信息。

### 字段

| 字段 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| `state` | `OxyState` | `CREATED` | 执行状态 |
| `output` | `Object` | `null` | 执行输出结果（可为任意类型） |
| `extra` | `Map<String, Object>` | `{}` | 额外元数据（如 usage、react_memory） |
| `oxyRequest` | `OxyRequest` | `null` | 关联的原始请求 |

### 使用示例

```java
// 构建成功响应
OxyResponse success = OxyResponse.builder()
    .state(OxyState.COMPLETED)
    .output("任务执行成功")
    .extra(Map.of("duration_ms", 1500))
    .build();

// 构建失败响应
OxyResponse failure = OxyResponse.builder()
    .state(OxyState.FAILED)
    .output("错误: 网络超时")
    .build();

// 检查状态
if (response.getState().isSuccessful()) {
    System.out.println(response.getOutput());
}
```

---

## OxyState

**包路径**: `com.jd.oxygent.core.oxygent.schemas.oxy`

定义 Agent 和 Tool 执行的生命周期状态。

### 状态值

| 枚举值 | 显示名 | 描述 |
|--------|--------|------|
| `CREATED` | Created | 已创建，等待执行 |
| `RUNNING` | Running | 正在执行中 |
| `COMPLETED` | Completed | 执行完成 |
| `SUCCESS` | Success | 执行成功（与 COMPLETED 类似） |
| `FAILED` | Failed | 执行失败 |
| `PAUSED` | Paused | 已暂停，可恢复 |
| `SKIPPED` | Skipped | 已跳过 |
| `CANCELED` | Canceled | 已取消 |
| `RATE_LIMIT_EXCEEDED` | Rate Limit Exceeded | 触发速率限制 |

### 状态转换

```
CREATED → RUNNING → COMPLETED / SUCCESS / FAILED
        ↘ PAUSED → RUNNING
        ↘ SKIPPED
        ↘ CANCELED
```

### 辅助方法

| 方法 | 返回值 | 描述 |
|------|--------|------|
| `isFinalState()` | `boolean` | 是否为终态（不可再转换） |
| `isSuccessful()` | `boolean` | 是否为成功状态 |
| `isError()` | `boolean` | 是否为错误状态 |
| `isRecoverable()` | `boolean` | 是否可恢复执行 |

### 使用示例

```java
OxyState state = response.getState();

if (state.isFinalState()) {
    if (state.isSuccessful()) {
        // 处理成功
    } else if (state.isError()) {
        // 处理失败
    }
} else if (state.isRecoverable()) {
    // 尝试恢复
}
```
