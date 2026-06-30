# 如何安装 OxyGent4J？

### 系统要求
---

- **JDK 17+**（推荐使用 JDK 17 或 JDK 21）
- **Maven 3.6+** 或 **Gradle 7.x+**
- 可选：[Node.js](https://nodejs.org)（用于内置 Web UI）

### 如何添加 Maven 依赖？
---

在项目的 `pom.xml` 中添加 OxyGent4J 核心依赖：

```xml
<dependency>
    <groupId>com.jd.oxygent</groupId>
    <artifactId>oxygent-core</artifactId>
    <version>1.0.10</version>
</dependency>
```

如果您使用 Spring Boot，还可以添加基础设施模块：

```xml
<dependency>
    <groupId>com.jd.oxygent</groupId>
    <artifactId>oxygent-infra</artifactId>
    <version>1.0.10</version>
</dependency>
```

### 如何使用 Gradle？
---

```groovy
implementation 'com.jd.oxygent:oxygent-core:1.0.10'
implementation 'com.jd.oxygent:oxygent-infra:1.0.10'
```

### 是否需要配置环境变量？
---

是的，OxyGent4J 需要配置 LLM 相关的环境变量才能正常工作。请在系统环境中设置以下变量：

```bash
export OXY_LLM_API_KEY="your_api_key"
export OXY_LLM_BASE_URL="your_base_url"
export OXY_LLM_MODEL_NAME="your_model_name"
```

或在 Spring Boot 项目中，通过 `application.yml` 配置：

```yaml
oxygent:
  llm:
    temperature: 0.1
    max-tokens: 4096
```

环境变量也可以通过 `.env` 文件或 IDE 的 Run Configuration 设置。

### 可不可以使用其他 JDK 版本？
---

建议使用 **JDK 17** 或更高版本运行 OxyGent4J。项目使用了 JDK 17 的语言特性（如 text blocks、sealed classes、pattern matching 等），低版本 JDK 无法编译运行。

### 如何验证安装？
---

创建一个简单的测试类：

```java
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;

public class InstallVerify {
    public static void main(String[] args) {
        HttpLlm llm = HttpLlm.builder()
                .name("test_llm")
                .apiKey("test")
                .baseUrl("http://localhost")
                .modelName("test")
                .build();
        System.out.println("OxyGent4J 安装成功！LLM 组件: " + llm.getName());
    }
}
```

如果输出正确信息，说明依赖已正确引入。

### Maven 下载太慢怎么办？
---

您可以配置国内镜像仓库，在 `~/.m2/settings.xml` 中添加：

```xml
<mirror>
    <id>aliyun</id>
    <mirrorOf>central</mirrorOf>
    <url>https://maven.aliyun.com/repository/central</url>
</mirror>
```

或者从 GitHub 克隆源码后本地安装：

```bash
git clone https://github.com/jd-opensource/JDOxyGent4J.git
cd JDOxyGent4J
mvn clean install -DskipTests
```

[下一章：快速上手](./quickstart.md)
[回到首页](../readme.md)

---

## 相关示例

- [单 Agent 示例](../../examples/agents/demo_single_agent.md) -- 最简单的 ChatAgent 配置
- [Config 设置示例](../../examples/backend/demo_config.md) -- 配置相关示例
