# 数据库与存储配置

## 简介

OxyGent4J 通过 `Config` 类管理所有存储后端配置。系统支持本地开发模式（无需外部依赖）和生产模式（接入真实 Redis/Elasticsearch），通过配置自动切换。

## 配置加载机制

配置采用三层优先级机制：

1. Java 程序中的默认值（最低优先级）
2. `config.json` 文件中的配置
3. `application.yml` 中的配置（最高优先级）

```java
// 指定配置文件路径和环境
Config.loadConfigPath("/path/to/config.json", "prod");
```

## Redis 配置

通过 `Config.RedisConfig` 管理 Redis 连接：

```java
@Data
public static class RedisConfig {
    private String host = "local";   // "local" 使用本地模拟
    private int port = 6379;
    private int database = 0;
    private String password = "";
    private int timeout = 5000;      // 超时时间 (ms)
}
```

对应的 `config.json` 配置：

```json
{
  "default": {
    "redis": {
      "host": "local",
      "port": 6379,
      "database": 0,
      "password": "",
      "timeout": 5000
    }
  }
}
```

## Elasticsearch 配置

通过 `Config.EsConfig` 管理 ES 连接：

```java
@Data
public static class EsConfig {
    private String type = "local";           // "local" 使用本地文件模拟
    private List<String> hosts = new ArrayList<>();
    private String user = "";
    private String password = "";
    private Integer numberOfShards;
    private Integer numberOfReplicas;
}
```

## LocalCache - 本地 Redis 模拟

当 `redis.host` 设置为 `"local"` 时，系统自动使用 `LocalCache` 实现：

- 基于 `ConcurrentHashMap` 的高性能内存缓存
- 支持 SET/GET/MSET/MGET/EXISTS/EXPIRE 等字符串操作
- 支持 LPUSH/RPOP/BRPOP/LRANGE 等列表操作
- 支持 HSET/HGETALL/HDEL 等哈希操作
- 内置 TTL 自动过期清理
- 线程安全，支持高并发

```java
// 无需额外配置，host = "local" 即自动启用
// config.json:
{
  "redis": { "host": "local" }
}
```

## LocalEs - 本地 ES 模拟

当 `es.type` 设置为 `"local"` 时，系统自动使用 `LocalEs` 实现：

- 基于本地文件系统存储 JSON 文档
- 每个索引对应一个 JSON 文件
- 使用 `Files.move` 原子写入，避免数据损坏
- 支持 UTF-8 编码，跨平台兼容
- 使用 `ReentrantLock` 保证并发安全
- 损坏文件自动备份为 `.bak` 格式

```java
// 无需安装 ES 服务，开发测试零依赖
// config.json:
{
  "es": { "type": "local" }
}
```

## 切换到生产环境

将 `host`/`type` 从 `"local"` 改为实际服务地址即可切换到生产环境：

```json
{
  "prod": {
    "redis": {
      "host": "redis-cluster.internal.jd.com",
      "port": 6379,
      "password": "your_password",
      "database": 0
    },
    "es": {
      "type": "remote",
      "hosts": ["http://es-node1:9200", "http://es-node2:9200"],
      "user": "elastic",
      "password": "your_password",
      "number_of_shards": 3,
      "number_of_replicas": 1
    }
  }
}
```

通过环境变量指定使用的配置环境：

```bash
export CONFIG_FILE_ENV=prod
export CONFIG_FILE_PATH=/app/config.json
```

## Vearch 向量数据库与文件存储

`Config.VearchConfig` 管理向量检索，`Config.FileConfig` 管理文件缓存目录（默认 `./cache_dir`）。

---

[上一章：RAG](../advanced/rag.md)
[下一章：调试](./debugging.md)
[回到首页](../readme.md)
