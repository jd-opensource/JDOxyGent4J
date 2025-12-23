# 状态码定义

```json
200: 成功
400: 失败
500: 服务器错误
```

# Response结构定义

```json
{
    "code": 200,
    "message": "SUCCESS",
    "data": {}
}
```

# 接口定义

## 获取agent架构

```json
url = "/get_organization"
method = "GET"
params = {}
response = {
    "code": 200,
    "message": "SUCCESS",
    "data": {
        "id_dict": {"math_agent": 0, "time_agent": 1},
        "organization": {
            "name": "math_agent",
            "type": "agent",
            "children": [
                {
                    "name": "time_agent",
                    "type": "agent",
                    "children": [
                        {
                            "name": "get_current_time",
                            "type": "tool",
                            "path": ["math_agent", "time_agent", "get_current_time"],
                            "is_remote": true,
                        },
                        {
                            "name": "convert_time",
                            "type": "tool",
                            "path": ["math_agent", "time_agent", "convert_time"],
                            "is_remote": true,
                        },
                    ],
                    "path": ["math_agent", "time_agent"],
                },
                {"name": "power", "type": "tool", "path": ["math_agent", "power"]},
                {"name": "pi", "type": "tool", "path": ["math_agent", "pi"]},
            ],
            "path": ["math_agent"],
        },
    }
}
```

## 获取问候语

```json
url = "/get_welcome_message"
method = "GET"
params = {}
response = {
    "code": 200,
    "message": "SUCCESS",
    "data": {
        "first_query": "Hi, I’m OxyGent. How can I assist you?"
    }
}
```

## 获取首问query

```json
url = "/get_first_query"
method = "GET"
params = {}
response = {
    "code": 200,
    "message": "SUCCESS",
    "data": {
        "first_query": "Please calculate the 20 positions of Pi"
    }
}
```

## 上传附件

```json
url = "/upload"
method = "POST"
datas = {
    file
}
response = {
    "code": 200,
    "message": "SUCCESS",
    "data": {
        "file_name": "123.jpg"
    }
}
```

## 提问（SSE连接）

```json
url = "/sse/chat"
method = "POST"
datas = {
    "query": "现在几点",
    "from_trace_id": "from_trace_id"
}

// 前端监听以下四类消息：
{
    "type": "tool_call",
    "content": {
        "node_id": "njU9muAZqAXtnTCR",
        "call_stack": ["user", "math_agent", "time_agent"],
        "caller": "math_agent",
        "caller_category": "agent",
        "callee": "time_agent",
        "callee_category": "agent",
        "arguments": {"query": "现在几点"}
    }
}

{
    "type": "observation",
    "content": {
        "node_id": "njU9muAZqAXtnTCR",
        "current_trace_id": "R7nPP3b5MGVMLczk",
        "call_stack": ["user", "math_agent", "time_agent"],
        "caller": "math_agent",
        "caller_category": "agent",
        "callee": "time_agent",
        "callee_category": "agent",
        "output": "当前时间是北京时间14:37。"
    }
}

{
    "type": "think", 
    "content": "用户想知道当前的时间，应该调用get_current_time函数来获取当前时间。"
}

{
    "type": "answer",
    "content": "保留小数点后30位: 3.14159265358979323846264338328"
}
```

## 如何运行 Web API 示例

你可以通过以下方式运行 Web API 相关的示例：

**参考现有示例**:

- [RouteController.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/backend/DemoApiController.java) - Web API 控制器实现
- [DemoAddRouter.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/backend/DemoAddRouter.java) - 路由添加示例

[上一章：可视化界面调试](./13_debugging.md)
[回到首页](./readme.md)
