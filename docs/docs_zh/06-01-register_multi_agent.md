# 如何建立简单的多智能体系统？

如果您认为单个智能体无法满足业务需求，使用多智能体系统可以有效地解决这个问题。

在下面的简单示例中，我们将功能相关的工具使用子智能体（subagent）进行管理。我们推荐新用户使用 ReActAgent 来调用这些工具：

```java
ReActAgent.builder()
    .name("file_agent")
    .desc("A tool that can operate the file system")
    .tools(Arrays.asList("file_tools"))
    .build(),
ReActAgent.builder()
    .name("time_agent")
    .desc("A tool that can get current time")
    .tools(Arrays.asList("time_tools"))
    .build(),
ReActAgent.builder()
    .name("math_agent")
    .desc("A tool that can do math calculates")
    .tools(Arrays.asList("math_tools"))
    .build()
```

接下来，您需要注册一个 **master_agent**，它负责在 MAS 中总调度其他智能体。将其他子智能体声明为 **master_agent** 的 `subAgents`：

```java
ReActAgent.builder()
    .name("master_agent")
    .isMaster(true)
    .subAgents(Arrays.asList("file_agent","time_agent","math_agent"))
    .build()
```

OxyGent 的智能体系统结构非常灵活，这意味着您可以注册多层子智能体（subagent），而无需手动管理它们之间的协作关系。

## 完整的可运行样例

以下是可运行的完整代码示例（参考 [DemoInReadme.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/DemoInReadme.java)）：

```java
package com.jd.oxygent.web.examples;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.tools.PresetTools;
import com.jd.oxygent.web.OpenOxySpringBootApplication;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * README文档中的演示示例类，展示了如何配置和使用OxyGent框架的基本功能
 * 包含了LLM模型、工具和代理的完整配置示例
 */
public class DemoInReadme {

    @OxySpaceBean(value = "defaultJavaOxySpace", defaultStart = true, query = "What time is it now? Please save it into time.txt.")
    public static List<BaseOxy> getDefaultOxySpace() {
        return Arrays.asList(
                // 1. HTTP LLM配置
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(System.getenv("OXY_LLM_API_KEY")) // 使用环境变量
                        .baseUrl(System.getenv("OXY_LLM_BASE_URL")) // 使用环境变量
                        .modelName(System.getenv("OXY_LLM_MODEL_NAME")) // 使用环境变量
                        .llmParams(Map.of("temperature", 0.01)) // 使用Map.of创建不可变Map
                        .timeout(30)
                        .build(),

                // 2. 时间工具
                PresetTools.TIME_TOOLS,

                // 3. 时间代理
                ReActAgent.builder()
                        .name("time_agent")
                        .desc("能够查询时间的工具代理")
                        .tools(Arrays.asList("time_tools")) // 工具名列表
                        .build(),

                // 4. 文件工具
                PresetTools.FILE_TOOLS,

                // 5. 文件代理
                ReActAgent.builder()
                        .name("file_agent")
                        .desc("能够操作文件系统的工具代理")
                        .tools(Arrays.asList("file_tools"))
                        .build(),

                // 6. 数学工具
                PresetTools.MATH_TOOLS,

                // 7. 数学代理
                ReActAgent.builder()
                        .name("math_agent")
                        .desc("能够执行数学计算的工具代理")
                        .tools(Arrays.asList("math_tools"))
                        .build(),

                // 8. 主代理（Master Agent）
                ReActAgent.builder()
                        .isMaster(true) // 设置为主代理
                        .name("master_agent")
                        .llmModel("default_llm")
                        .subAgents(Arrays.asList("time_agent", "file_agent", "math_agent")) // 子代理列表
                        .build()
        );
    }

    public static void main(String[] args) throws Exception {
        Objects.requireNonNull(args, "命令行参数不能为空");

        var currentClassName = Thread.currentThread().getStackTrace()[1].getClassName();
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(currentClassName);
        OpenOxySpringBootApplication.main(args);
    }
}
```

[上一章：设置全局数据](./03-03-set_global.md)
[下一章：复制相同智能体](./06-02-moa.md)
[回到首页](./readme.md)