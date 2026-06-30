# 如何进行检索增强生成（RAG）？

## 简介

OxyGent4J 提供了专用的 `RAGAgent`，支持通过知识检索函数向 prompt 中动态注入知识上下文。RAGAgent 继承自 `ChatAgent`，在每次请求前自动执行知识检索并将结果注入到 prompt 模板的指定占位符位置。

## 基本用法

```java
// 定义知识检索函数
Function<OxyRequest, String> retrieveKnowledge = (oxyRequest) -> {
    String query = oxyRequest.getQuery();
    // 替换为实际的向量数据库或搜索引擎查询
    return "knowledge1\nknowledge2\nknowledge3";
};

// 构建 RAG Agent
RAGAgent.builder()
    .name("qa_agent")
    .llmModel("default_llm")
    .prompt("基于以下知识回答问题：\n${knowledge}")
    .knowledgePlaceholder("knowledge")
    .funcRetrieveKnowledge(retrieveKnowledge)
    .build()
```

## RAGAgent 核心参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `knowledgePlaceholder` | `String` | `"knowledge"` | prompt 中知识注入的占位符名 |
| `funcRetrieveKnowledge` | `Function<OxyRequest, String>` | 空实现 | 知识检索函数 |
| `prompt` | `String` | 内置 RAG 模板 | 包含 `${knowledge}` 占位符的提示词 |

## 工作流程

```
用户查询 -> preProcess() -> funcRetrieveKnowledge(request)
         -> 将知识注入 arguments["knowledge"]
         -> buildInstruction() 替换 ${knowledge} 占位符
         -> 调用 LLM 生成回答
```

## 完整示例

```java
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.RAGAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

import java.util.*;
import java.util.function.Function;

public class DemoRagAgent {

    // 知识检索函数 - 实际场景中查询向量数据库
    public static final Function<OxyRequest, String> FUNC_RETRIEVE_KNOWLEDGE = (oxyRequest) -> {
        String query = oxyRequest.getQuery();
        log.info("Retrieving knowledge for query: " + query);
        // 模拟检索结果，实际应接入 Vearch、ES 或其他向量存储
        return """
            Pi is 3.141592653589793238462643383279502.
            """;
    };

    @OxySpaceBean(value = "ragAgentOxySpace", defaultStart = true,
                  query = "Please calculate the 20 positions of Pi")
    public static List<BaseOxy> getDefaultOxySpace() {
        var promptTemplate = """
            You are a helpful assistant! Refer to the following knowledge:
            ${knowledge}
            """;

        return Arrays.asList(
            HttpLlm.builder()
                .name("default_llm")
                .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                .build(),

            RAGAgent.builder()
                .name("qa_agent")
                .llmModel("default_llm")
                .prompt(promptTemplate)
                .knowledgePlaceholder("knowledge")
                .funcRetrieveKnowledge(FUNC_RETRIEVE_KNOWLEDGE)
                .build()
        );
    }
}
```

## 向量存储集成

OxyGent4J 通过 `VearchConfig` 支持向量数据库集成：

```json
{
  "vearch": {
    "enabled": true,
    "router_url": "http://vearch-router:9001",
    "master_url": "http://vearch-master:8817",
    "db_name": "knowledge_db",
    "embedding_model_url": "http://embedding-service/v1/embeddings"
  }
}
```

RAG 系统的核心常量在 `RagConfig` 中定义：

| 常量 | 默认值 | 说明 |
|------|--------|------|
| `DEFAULT_SIMILARITY_THRESHOLD` | `0.3f` | 向量相似度阈值 |
| `DEFAULT_RERANK_SIZE` | `5` | 重排序返回结果数 |
| `DEFAULT_BATCH_TOKEN_LIMIT` | `32000` | 批处理 token 上限 |
| `DEFAULT_DIMENSION` | `1024` | 默认向量维度 |

## 容错机制

RAGAgent 内置完善的容错处理：
- 检索函数未配置时使用空实现，不影响基本对话功能
- 检索异常时注入空字符串，确保 LLM 调用不中断
- 异常信息记录到日志，便于后续排查

---

[上一章：多模态](./multimodal.md)
[下一章：数据库配置](../backend/database.md)
[回到首页](../readme.md)
