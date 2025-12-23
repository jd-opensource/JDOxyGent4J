[English](./README.md) | [中文](./README_zh.md)

<p align="center">
  <a href="https://github.com/jd-opensource/JDOxyGent4J/pulls">
    <img src="https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square" alt="PRs Welcome">
  </a>
  <a href="https://github.com/jd-opensource/JDOxyGent4J/blob/main/LICENSE">
    <img src="https://img.shields.io/badge/License-Apache_2.0-blue.svg" alt="license"/>
  </a>
  <a href="https://search.maven.org/search?q=g:com.jd.oxygent%20AND%20a:JDOxyGent4J">
    <img src="https://img.shields.io/badge/maven_central-JDOxyGentJ1.0.0-blue" alt="maven"/>
  </a>
  <a href="https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html">
    <img src="https://img.shields.io/badge/Java-17-orange?logo=openjdk" alt="jdk17"/>
  </a>
</p>

![Banner](docs/images/images_banner.jpg)

<h2 align="center">An advanced Java framework that enables developers to quickly build production-grade intelligent systems.</h2>

<div align="center">

**🌐 Official Website:** [OxyGent](http://oxygent.jd.com) | **📚 Open Source:** [Python Version](https://github.com/jd-opensource/OxyGent) | [Java Version](https://github.com/jd-opensource/JDOxyGent4J)

</div>

## 🔍 1. Project Overview

OxyGent is an open-source multi-agent framework that unifies Agent/LLM/Tool as composable Oxy components, providing transparent end-to-end pipelines with orchestration capabilities, supporting continuous evolution and unlimited expansion in production environments.

JDOxyGent4J shares the same philosophy as the Python version and is deeply optimized for the Java ecosystem: native type safety and compile-time validation, seamless Spring Boot integration, enterprise-grade concurrency and stability, and developer-friendly APIs for Java developers.

JDOxyGent4J has been validated and applied in multiple real-world business scenarios, proving its stability and scalability in production environments.

---

## ⚡ 2. Core Features

⚙️ **Deep Java Ecosystem Integration**
- Seamlessly integrates with Spring Boot and Java EE standards, providing a type-safe intelligent agent development experience. Supports rapid definition of agent behavior through annotations and configuration files, leveraging Java ecosystem's mature toolchain and dependency injection mechanisms to accelerate enterprise AI application development.

🛡️ **Enterprise-Grade Reliability Assurance**
- Inherits Java platform's security model and exception handling mechanisms, ensuring stable operation of agent systems in production environments. Provides complete access control, audit logs, and observability support, meeting enterprise application security and compliance requirements.

⚡ **High-Performance Concurrent Processing**
- Based on Java's concurrent programming model, enabling efficient asynchronous execution of agent tasks. Supports large-scale concurrent agent execution, with non-blocking IO for inter-agent communication, showing significant performance improvements over the Python version in concurrency tests.

🔄 **Flexible Configuration and Extensible Storage**
- Provides unified infrastructure abstraction layer and flexible configuration system, supporting user-defined extensions for various storage implementations (databases, caches, vector libraries, etc.). Through configuration-driven architecture design, easily adapts to local development, cloud deployment, and enterprise runtime environments, achieving hot-pluggable storage layer and seamless migration.


---

## 🗂️ 3. Project Structure

```
JDOxyGent4J/
├── oxygent-core/                  # Core: Agent/LLM/Tool/MAS with examples
│   └── src/main/java/com/jd/oxygent/core/
│       ├── Config.java            # Global configuration
│       ├── Mas.java               # Multi-Agent System (MAS) core
│       ├── MasFactoryBean.java    # Spring integration and space management
│       ├── oxygent/oxy/           # Component implementations
│       │   ├── agents/            # Chat/ReAct/Workflow/RAG/SSE/Parallel etc.
│       │   ├── llms/              # HttpLlm, OpenAiLlm etc.
│       │   ├── function_tools/    # FunctionHub, FunctionTool
│       │   └── mcp/               # StdioMCPClient, SSEMCPClient
│       ├── oxygent/schemas/       # Request/Response/Memory/Context/Exception/Observation
│       ├── oxygent/infra/         # Database, RAG, multimodal abstraction
│       ├── oxygent/samples/       # Sample code and servers
│       ├── tools/                 # Preset tools and example tools
│       └── utils/                 # Common utility classes
├── oxygent-infra/                # Infrastructure implementation
├── oxygent-starter-core/          # Spring Boot Starter (auto-configuration)
├── oxygent-studio/                # Web examples and demo UI
├── docs/                          # Documentation (Chinese in docs/docs_zh)
└── cache_dir/                     # Local cache directory
```

## 🚀 4. Quick Start

> 💡 **Quick Start Recommendations**:
> - Read [docs/](docs/docs_zh/README.md) for complete architecture and design documentation
> - Run [samples/](oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/DemoInReadme.java) with 30+ examples covering mainstream Agent patterns and application scenarios


### Environment Requirements
- Java 17+
- Maven 3.6+
- Spring Boot 3.2.5+

### Installation Methods

#### Method 1: Maven Dependency (Recommended)
```xml
<dependency>
    <groupId>com.jd.oxygent</groupId>
    <artifactId>oxygent-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### Method 2: Local Build
```bash
git clone https://github.com/jd-opensource/JDOxyGent4J.git
cd JDOxyGent4J
mvn clean install -DskipTests
```

### Startup Parameters (Required)
Due to reflection and dynamic proxy usage, the following JVM parameters must be added:
```
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.lang.reflect=ALL-UNNAMED
--add-opens java.base/java.util=ALL-UNNAMED
--add-opens java.base/sun.util.calendar=ALL-UNNAMED
--add-opens java.base/java.math=ALL-UNNAMED
```
####  Refer to IDEA configuration [idea_env_vm_config](docs/images/idea_env_vm_config.png)
```
ENV Config
OXY_LLM_API_KEY= "your API key"
OXY_LLM_BASE_URL= "your base url"
OXY_LLM_MODEL_NAME= "your model name"

VM config
--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/sun.util.calendar=ALL-UNNAMED --add-opens java.base/java.math=ALL-UNNAMED --add-opens java.base/sun.security.action=ALL-UNNAMED --add-exports=java.base/sun.net.util=ALL-UNNAMED
```

####  Or run IDEA configuration script (Required)
```bash
# Return to project root directory
cd ../..
# For mac or linux systems use the following command
./setup-idea.sh
# For windows systems use the following command
setup-idea-windows.bat
# Or double-click setup-idea-windows.bat to configure
```

###  📚 Detailed Examples

The project provides multiple complete examples, located in [SAMPLES.md](oxygent-core/SAMPLES_zh.md); for more tutorials please read `docs/docs_zh`.

Example preview after startup:
![Example Preview](docs/images/demo_img.png)
- Example
```java
import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.tools.PresetTools;
import com.jd.oxygent.core.utils.EnvUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DemoInReadme {

    public static void main(String[] args) throws Exception {
        List<BaseOxy> oxySpaceList = getDefaultOxySpace();
        Mas mas = new Mas("app", oxySpaceList);
        mas.setOxySpace(oxySpaceList);
        mas.init();

        Map<String, Object> info = new HashMap<>();
        info.put("query", "What time is it now? Please save it into time.txt.");

        System.out.println("Starting chatWithAgent request");
        mas.chatWithAgent(info);
    }
    
    public static List<BaseOxy> getDefaultOxySpace() {
        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .llmParams(Map.of("temperature", 0.01)) // Use Map.of to create immutable Map
                        .timeout(30)
                        .build(),

                // 2. Time tools (assuming preset_tools.time_tools is a predefined Tool instance)
                PresetTools.TIME_TOOLS, // Need to define PresetTools class

                // 3. Time agent
                ReActAgent.builder()
                        .name("time_agent")
                        .desc("A tool that can query the time")
                        .tools(Arrays.asList("time_tools")) // Tool name list
                        .build(),
                // 4. File tools
                PresetTools.FILE_TOOLS,

                // 5. File agent
                ReActAgent.builder()
                        .name("file_agent")
                        .desc("A tool that can operate the file system")
                        .tools(Arrays.asList("file_tools"))
                        .build(),

                // 6. Math tools
                PresetTools.MATH_TOOLS,
                // 7. Math agent
                ReActAgent.builder()
                        .name("math_agent")
                        .desc("A tool that can perform mathematical calculations.")
                        .tools(Arrays.asList("math_tools"))
                        .build(),

                // 8. Master Agent
                ReActAgent.builder()
                        .isMaster(true) // Set as master agent
                        .name("master_agent")
                        .llmModel("default_llm")
                        .subAgents(Arrays.asList("time_agent", "file_agent", "math_agent")) // Sub-agent list
                        .build()
        );
    }
}
```


## 🔧 Running Web Application (Example UI + API)

### Startup Methods
- Method 1 (Recommended, Development Mode):
    - `cd oxygent-studio && mvn spring-boot:run`
- Method 2 (Run Main Class Directly):
    - Main class: `com.jd.oxygent.web.OpenOxySpringBootApplication`
    - Run in IDE or use `mvn -pl oxygent-studio exec:java -Dexec.mainClass=com.jd.oxygent.web.OpenOxySpringBootApplication`

- Method 3: Spring Boot
```java
@SpringBootApplication
public class OpenOxySpringBootApplication {
    public static void main(String[] args) {
        SpringApplication.run(OpenOxySpringBootApplication.class, args);
    }
}
```

### Access Address
- Web example page: `http://localhost:8080`



## 🤝 6. Contribution Guidelines

We welcome contributions of any form! Including but not limited to:
- 🐞 Submit bugs or reproduce issues
- 💡 Propose new features or improvement suggestions
- 📚 Improve documentation or supplement examples
- 🧑‍💻 Submit Pull Requests (please base on `dev` branch)

**Contribution Process**:
1. Fork this repository
2. Create feature branch (`git checkout -b feat/your-feature`)
3. Submit code and push
4. Create Pull Request

We appreciate contributions of all forms!🎉🎉🎉
If you encounter problems during development, please check our documentation: * **[Documentation](docs/)**
---

## 📣 7. Community & Support

If you encounter any issues along the way, you are welcomed to submit reproducible steps and log snippets in the project's Issues area, or contact the OxyGent Core team directly via your internal Slack.

Welcome to contact us:

<div align="center">
  <img src="https://pfst.cf2.poecdn.net/base/image/b1e96084336a823af7835f4fe418ff49da6379570f0c32898de1ffe50304d564?w=1760&h=2085&pmaid=425510216" alt="contact" width="50%" height="50%">
</div>

---

## 🙏 8. Acknowledgments

Thank you to all developers who have contributed to the OxyGent project!

---

## 📄 9. Open Source License

This project adopts the **Apache License 2.0** open source license.  
© 2025 JD OxyGent Team —— Making agent development simpler and more efficient!

---