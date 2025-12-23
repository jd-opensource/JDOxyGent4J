# 如何安装OxyGent？



## 环境要求

---
OxyGent Java版本需要以下环境：

- **Java 17 或更高版本**
- **Maven 3.6+**
- **Spring Boot 3.2.5**

## IDEA Quick Start

---
1. 克隆源码
```bash
git clone https://github.com/jd-opensource/JDOxyGent4J.git
cd oxygent
```
2. 启动IDEA配置脚本
```bash
# 回到项目根目录
cd ../..
# mac或linux系统使用如下命令
./setup-idea.sh
#windows系统使用如下命令
setup-idea-windows.bat
#或双击setup-idea-windows.bat配置即可
```
3. 进入[快速开始示例](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/DemoInReadme.java)，使用IDEA运行
## 环境配置

## 如何配置Java环境？

---
+ **安装Java 17**
```bash
# macOS (使用 Homebrew)
brew install openjdk@17

# Linux (Ubuntu/Debian)
sudo apt update
sudo apt install openjdk-17-jdk

# Windows
# 从 Oracle 或 OpenJDK 官网下载并安装 Java 17
```

+ **安装Maven**
```bash
# macOS (使用 Homebrew)
brew install maven

# Linux (Ubuntu/Debian)
sudo apt install maven

# Windows
# 从 Maven 官网下载并配置环境变量
```

+ **验证安装**
```bash
java -version
mvn -version
```

## 如何获取OxyGent项目？

---

#### 方式一：克隆源码（推荐用于开发）
```bash
# 克隆项目
git clone https://github.com/jd-opensource/JDOxyGent4J.git
cd oxygent

# 构建项目
mvn clean install
```

#### 方式二：使用Maven依赖
在您的项目中添加以下依赖：
```xml
<!-- 框架集成 -->
<dependency>
    <groupId>com.jd.framework</groupId>
    <artifactId>oxygent-starter-core</artifactId>
    <version>1.0.0</version>
</dependency>
```


## 运行时JVM参数配置

---

### IDEA脚本化配置
我们提供了一个脚本，用于快速配置IDEA的运行时JVM参数和LLM环境变量
```bash
# 回到项目根目录
cd ../..
# mac或linux系统使用如下命令
./setup-idea.sh
#windows系统使用如下命令
setup-idea-windows.bat
#或双击setup-idea-windows.bat配置即可
```

### 命令行配置

由于Java 17+模块系统的限制，使用命令行运行OxyGent应用时需要添加以下JVM参数和环境变量：

```bash
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.lang.reflect=ALL-UNNAMED
--add-opens java.base/java.util=ALL-UNNAMED
--add-opens java.base/sun.util.calendar=ALL-UNNAMED
--add-opens java.base/java.math=ALL-UNNAMED
```


```bash
export OXY_LLM_API_KEY="your-api-key"
export OXY_LLM_BASE_URL="your-llm-endpoint"
export OXY_LLM_MODEL_NAME="your-model-name"
```


## 验证安装

---

运行以下命令验证安装是否成功：

如果看到Spring Boot启动日志并且浏览器自动打开http://localhost:8080页面，说明安装成功。

[下一章：运行demo](./00-02-demo.md)
[回到首页](./readme.md)