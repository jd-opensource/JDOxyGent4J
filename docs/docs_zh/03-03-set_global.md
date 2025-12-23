# 如何设置系统全局数据？

OxyGent支持使用非常简单的方式设置和修改系统全局数据，这些数据类似于全局变量，能够在MAS中使用`OxyRequest`进行更改与访问。

支持的方法包括：
+ `getGlobalData`：使用`(key,defaultValue)`按键值访问全局数据
+ `setGlobalData`：使用`(key,value)`按键值修改全局数据

## 基础用法示例

下面使用全局数据实现简单的计数器。

```java
public class CounterAgent extends BaseAgent {
    @Override
    public OxyResponse execute(OxyRequest oxyRequest) {
        // 获取计数器值，如果不存在则返回默认值0
        int cnt = oxyRequest.getGlobalData("counter", 0) + 1;

        // 存储计数+1
        oxyRequest.setGlobalData("counter", cnt);

        return OxyResponse.builder()
            .state(OxyState.COMPLETED)
            .output(String.format("This MAS has been called %d time(s).", cnt))
            .oxyRequest(oxyRequest)
            .build();
    }
}
```

将这个`CounterAgent`作为`master`，就可以输出MAS被调用的次数。

```java
import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.*;
import com.jd.oxygent.core.oxygent.schemas.*;

import java.util.Arrays;
import java.util.Map;

public class GlobalDataExample {
    public static void main(String[] args) {
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

            new CounterAgent("master_agent", true)
        );

        var mas = new Mas("global_demo", oxySpace);

        try {
            // 第一次调用 → counter = 1
            var r1 = mas.chatWithAgent(Map.of("query", "first"));
            System.out.println(r1.getOutput());

            // 第二次调用 → counter = 2 (global_data persisted inside MAS)
            var r2 = mas.chatWithAgent(Map.of("query", "second"));
            System.out.println(r2.getOutput());

            // 直接从MAS中获取:
            System.out.println("Current global_data: " + mas.getGlobalData());
            // 全局数据的生命周期和MAS相同
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## 高级数据作用域管理

OxyGent支持多种数据作用域，除了全局数据还包括shared_data和group_data。以下是完整的数据作用域示例（参考 [DemoDataScope.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/backend/DemoDataScope.java)）：

```java
// 输入处理函数，用于展示不同数据作用域
private static final Function<OxyRequest, OxyRequest> PROCESS_INPUT_FUNCTION = oxyRequest -> {
    log.info("--- agent name --- : {}", oxyRequest.getCallee());
    log.info("--- arguments --- : {}", oxyRequest.getArguments());
    log.info("--- shared_data --- : {}", oxyRequest.getSharedData());
    log.info("--- group_data --- : {}", oxyRequest.getGroupData());
    log.info("--- global_data --- : {}", oxyRequest.getGlobalData());
    return oxyRequest;
};

// 配置数据作用域演示的智能体
ReActAgent.builder()
    .name("master_agent")
    .isMaster(true)
    .llmModel("default_llm")
    .subAgents(List.of("time_agent"))
    .funcProcessInput(PROCESS_INPUT_FUNCTION)  // 打印所有数据作用域
    .build(),

ReActAgent.builder()
    .name("time_agent")
    .desc("A tool for time query.")
    .tools(List.of("time_tools"))
    .funcProcessInput(PROCESS_INPUT_FUNCTION)  // 同样打印数据作用域
    .build()
```

### 数据作用域说明

- **global_data**: 全局数据，在整个MAS生命周期内持续存在
- **shared_data**: 共享数据，在请求处理过程中的所有代理间共享
- **group_data**: 分组数据，用于特定代理组间的数据共享
- **arguments**: 当前请求的参数数据


**参考现有示例**:
- [DemoGlobalData.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/backend/DemoGlobalData.java) - 基础全局数据管理
- [DemoDataScope.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/backend/DemoDataScope.java) - 完整数据作用域演示

[上一章：设置数据库](./03-02-set_database.md)
[下一章：创建简单的多agent系统](./06-01-register_multi_agent.md)
[回到首页](./readme.md)