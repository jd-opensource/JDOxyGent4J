# 如何注册一个本地工具？

在OxyGent中，建议通过[FunctionHub](../development/api/function_tools/function_hub.md)注册本地工具。Java版本支持两种方式：程序化注册和注解注册。您也可以使用MCP注册工具，具体参考[使用MCP自定义工具](./02-04-use_mcp_tools.md)或[使用MCP开源工具](./02-03-use_opensource_tools.md)。

## 步骤 1：创建工具类（推荐注解方式）
首先，您可以创建一个继承自 `FunctionHub` 的工具类，并使用注解注册工具：

```java
import com.jd.oxygent.core.oxygent.oxy.function_tools.FunctionHub;
import com.jd.oxygent.core.oxygent.tools.Tool;
import com.jd.oxygent.core.oxygent.tools.ParamMetaAuto;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

public class FileTools extends FunctionHub {

    public FileTools() {
        super("file_tools");
        this.setDesc("文件操作工具集合");
    }
}
```

## 步骤 2：使用注解注册工具
使用 `@Tool` 注解将 Java 方法注册为工具，例如，您可以注册一些基础的文件操作工具：

```java
@Tool(
    name = "write_file",
    description = "创建新文件或完全覆盖现有文件的新内容。谨慎使用，因为它会在没有警告的情况下覆盖现有文件。",
    paramMetas = {
        @ParamMetaAuto(name = "path", type = "String", description = "文件路径"),
        @ParamMetaAuto(name = "content", type = "String", description = "文件内容")
    }
)
public static String writeFile(String path, String content) {
    try {
        Files.writeString(Path.of(path), content, StandardCharsets.UTF_8);
        return "Successfully wrote to " + path;
    } catch (IOException e) {
        return "Error writing file: " + e.getMessage();
    }
}
```

## 步骤 3：将工具添加到 Agent

将注册的工具放入 Agent 可以调用的权限域中。Agent 将根据工具的描述自动调用相应工具：

```java
// 在 OxySpace 配置中
List<BaseOxy> oxySpace = Arrays.asList(
    HttpLlm.builder()
        .name("default_llm")
        .apiKey(System.getenv("OXY_LLM_API_KEY"))
        .baseUrl(System.getenv("OXY_LLM_BASE_URL"))
        .modelName(System.getenv("OXY_LLM_MODEL_NAME"))
        .llmParams(Map.of("temperature", 0.01))
        .timeout(240)
        .build(),
    new FileTools(), // 工具实例
    ReActAgent.builder()
        .name("master_agent")
        .isMaster(true)
        .tools(Arrays.asList("file_tools"))
        .llmModel("default_llm")
        .build()
);
```

## 完整的可运行样例

以下是完整的代码示例（参考 [DemoFunctionHub.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/tools/DemoFunctionHub.java)），展示了如何创建工具并将其集成到 Agent 中：

```java
package com.jd.oxygent.core.oxygent.samples.examples.tools;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.function_tools.FunctionHub;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;

/**
 * 功能中心演示类
 * 展示了如何创建和配置自定义功能工具，包括笑话工具的实现
 */
public class DemoFunctionHub {

    private static final Random RANDOM = new Random();
    private static final FunctionHub JOKE_TOOLS = new FunctionHub("joke_tools");

    /**
     * 初始化功能中心，注册笑话工具
     */
    private static void init() {
        // 设置功能中心描述
        JOKE_TOOLS.setDesc("用于讲笑话的工具集合");

        // 注册笑话工具，包含适当的参数定义
        JOKE_TOOLS.registerTool(
                "joke_tool",
                "一个能够生成各种类型笑话的工具",
                (args) -> {
                    try {
                        var jokeType = "any";
                        return jokeTool(jokeType);
                    } catch (Exception e) {
                        throw new RuntimeException("笑话工具执行失败: " + e.getMessage(), e);
                    }
                },
                Arrays.asList(
                        new FunctionHub.ParamMeta("joke_type", "String", "笑话的类型", "any")
                )
        );
    }

    /**
     * 笑话工具实现方法
     * 根据指定类型返回随机笑话
     */
    public static String jokeTool(String jokeType) {
        Objects.requireNonNull(jokeType, "笑话类型不能为空");

        var jokes = List.of(
                "为什么科学家不相信原子？因为它们组成了一切！",
                "为什么稻草人获奖了？因为他在自己的领域里很出色！",
                "为什么鸡蛋不讲笑话？因为它们会互相逗笑破壳！"
        );

        System.out.println("笑话类型: " + jokeType);
        return jokes.get(RANDOM.nextInt(jokes.size()));
    }

    @OxySpaceBean(value = "functionhubJavaOxySpace", defaultStart = true, query = "Please tell a joke")
    public static List<BaseOxy> getDefaultOxySpace() {
        init(); // 初始化功能中心

        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(System.getenv("OXY_LLM_API_KEY")) // 使用环境变量
                        .baseUrl(System.getenv("OXY_LLM_BASE_URL")) // 使用环境变量
                        .modelName(System.getenv("OXY_LLM_MODEL_NAME")) // 使用环境变量
                        .build(),
                JOKE_TOOLS,
                ReActAgent.builder()
                        .name("joke_agent")
                        .llmModel("default_llm")
                        .additionalPrompt("你是一个幽默的助手。当用户需要笑话时，请使用joke_tool来获取笑话。")
                        .tools(Arrays.asList("joke_tools"))
                        .build()
        );
    }

    public static void main(String[] args) throws Exception {
        Objects.requireNonNull(args, "命令行参数不能为空");

        var currentClassName = Thread.currentThread().getStackTrace()[1].getClassName();
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(currentClassName);
        ServerApp.main(args);
    }
}
```

## 高级注解式工具注册

OxyGent还支持更高级的注解式工具注册方式，简化工具的创建和注册过程（参考 [DemoFunctionHubAnnotation.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/tools/DemoFunctionHubAnnotation.java)）：

```java
public static class JokeTool extends FunctionHub {
    public JokeTool() {
        super("joke_tools");
        this.setDesc("用于讲笑话的工具集合");
    }

    // 使用@Tool注解直接注册方法为工具
    @Tool(
        name = "joke_tool",
        description = "一个能够生成各种类型笑话的工具",
        paramMetas = {@ParamMetaAuto(name = "joke_type", type = "String", description = "笑话的类型,默认值为any")}
    )
    public static String jokeTool(String jokeType) {
        var jokes = List.of(
            "为什么科学家不相信原子？因为它们组成了一切！",
            "为什么稻草人获奖了？因为他在自己的领域里很出色！",
            "为什么鸡蛋不讲笑话？因为它们会互相逗笑破壳！"
        );
        return jokes.get(RANDOM.nextInt(jokes.size()));
    }
}
```

### 注解式vs程序式注册对比

| 特性 | 注解式注册 | 程序式注册 |
|-----|-----------|-----------|
| **代码简洁性** | ✅ 更简洁，声明式 | 需要手动注册代码 |
| **类型安全** | ✅ 编译时检查 | 运行时绑定 |
| **IDE支持** | ✅ 更好的代码提示 | 一般 |
| **灵活性** | 适合固定工具 | ✅ 更灵活，动态注册 |
| **学习成本** | ✅ 更低 | 需要理解注册API |


**参考现有示例**:
- [DemoFunctionHub.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/tools/DemoFunctionHub.java) - 基础工具注册和使用
- [DemoFunctionHubAnnotation.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/tools/DemoFunctionHubAnnotation.java) - 高级注解式工具注册

## 工具类完整示例

以下是框架内置的完整工具类，包含常用的文件操作工具：

```java
package com.jd.oxygent.examples.tools;

import com.jd.oxygent.core.oxygent.oxy.function_tools.FunctionHub;
import com.jd.oxygent.core.oxygent.tools.ParamMetaAuto;
import com.jd.oxygent.core.oxygent.tools.Tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileTools extends FunctionHub {

    public FileTools() {
        super("file_tools");
        this.setDesc("文件操作工具集合");
    }

    @Tool(
        name = "write_file",
        description = "创建新文件或完全覆盖现有文件的新内容。谨慎使用，因为它会在没有警告的情况下覆盖现有文件。",
        paramMetas = {
            @ParamMetaAuto(name = "path", type = "String", description = "文件路径"),
            @ParamMetaAuto(name = "content", type = "String", description = "文件内容")
        }
    )
    public static String writeFile(String path, String content) {
        try {
            Files.writeString(Path.of(path), content, StandardCharsets.UTF_8);
            return "Successfully wrote to " + path;
        } catch (IOException e) {
            return "Error writing file: " + e.getMessage();
        }
    }

    @Tool(
        name = "read_file",
        description = "读取文件的内容。如果文件不存在，则返回错误消息。",
        paramMetas = {
            @ParamMetaAuto(name = "path", type = "String", description = "要读取的文件路径")
        }
    )
    public static String readFile(String path) {
        try {
            if (!Files.exists(Path.of(path))) {
                return "Error: The file at " + path + " does not exist.";
            }
            return Files.readString(Path.of(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    @Tool(
        name = "delete_file",
        description = "删除文件。如果文件被删除，则返回成功消息，如果文件不存在，则返回错误。",
        paramMetas = {
            @ParamMetaAuto(name = "path", type = "String", description = "要删除的文件路径")
        }
    )
    public static String deleteFile(String path) {
        try {
            Path filePath = Path.of(path);
            if (!Files.exists(filePath)) {
                return "Error: The file at " + path + " does not exist.";
            }
            Files.delete(filePath);
            return "Successfully deleted the file at " + path;
        } catch (IOException e) {
            return "Error deleting file: " + e.getMessage();
        }
    }
}
```

[上一章：选择智能体种类](./01-05-select_agent.md)
[下一章：使用MCP开源工具](./02-03-use_opensource_tools.md)
[回到首页](./readme.md)