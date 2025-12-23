# 如何设置数据库？

OxyGent支持设置外部工具，比如您的数据库。现在OxyGent支持三种类型的外部数据库：

+ Elasticsearch: https://www.elastic.co/elasticsearch
+ Redis: https://redis.io/
+ Vearch: https://github.com/vearch/vearch

以Elasticsearch为例，您可以在配置文件中输入数据库信息：

```java
// 在application.yml中设置Elasticsearch配置
oxygent:
  elasticsearch:
    hosts:
      - ${PROD_ES_HOST_1}
      - ${PROD_ES_HOST_2}
      - ${PROD_ES_HOST_3}
    username: ${PROD_ES_USER}
    password: ${PROD_ES_PASSWORD}
```

或者通过代码配置：

```java
// 通过BaseEs接口配置Elasticsearch
BaseEs elasticsearchClient = RemoteEs.builder()
    .hosts(Arrays.asList(
        System.getenv("PROD_ES_HOST_1"),
        System.getenv("PROD_ES_HOST_2"),
        System.getenv("PROD_ES_HOST_3")
    ))
    .username(System.getenv("PROD_ES_USER"))
    .password(System.getenv("PROD_ES_PASSWORD"))
    .build();
```

在设置好数据库后，agent会自动使用数据库进行存储与检索。如果您没有设置数据库，OxyGent将会使用本地文件系统模拟数据库运行。

## 完整的可运行样例

以下是可运行的完整代码示例：

```java
/**
 * Demo for using OxyGent with multiple LLMs and an agent.
 */
import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.*;
import com.jd.oxygent.core.oxygent.infra.impl.RemoteEs;
import com.jd.oxygent.core.oxygent.schemas.Message;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DatabaseExample {
    public static void main(String[] args) {
        // 配置Elasticsearch
        var elasticsearchClient = RemoteEs.builder()
            .hosts(Arrays.asList(
                System.getenv("PROD_ES_HOST_1"),
                System.getenv("PROD_ES_HOST_2"),
                System.getenv("PROD_ES_HOST_3")
            ))
            .username(System.getenv("PROD_ES_USER"))
            .password(System.getenv("PROD_ES_PASSWORD"))
            .build();

        var oxySpace = Arrays.asList(
            HttpLlm.builder()
                .name("default_llm")
                .apiKey(System.getenv("OXY_LLM_API_KEY"))
                .baseUrl(System.getenv("OXY_LLM_BASE_URL"))
                .modelName(System.getenv("OXY_LLM_MODEL_NAME"))
                .llmParams(Map.of("temperature", 0.01))
                .semaphoreCount(4)
                .timeout(240)
                .build(),

            ReActAgent.builder()
                .name("master_agent")
                .isMaster(true)
                .llmModel("default_llm")
                .build(),

            // 添加Elasticsearch到OxySpace
            elasticsearchClient
        );

        var mas = new Mas("database_app", oxySpace);

        try {
            var messages = List.of(
                Message.builder()
                    .role("system")
                    .content("You are a helpful assistant")
                    .build(),
                Message.builder()
                    .role("user")
                    .content("hello")
                    .build()
            );

            var result = mas.call("master_agent", Map.of("messages", messages));
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

[上一章：设置OxyGent Config](./03-01-set_config.md)
[下一章：设置全局数据](./03-03-set_global.md)
[回到首页](./readme.md)