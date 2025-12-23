# 如何设置缓存消息方式？

OxyGent提供了两个种缓存方式，这样缓存方式足以帮助您完成基础的MAS构建使用消息存储，以下是简要介绍：

## Local

`Local`是内存缓存方式存储消息，流式输出完消息后释放内存信息，缓存方式您可以使用`Local`方式，配置MAS进行消息存储的工作，配置如下

```yaml
#请在oxygent-studio项目resources资源文件下的application.yml配置文件oxygent.cache
oxygent:
  cache: local
```

## Redis
`Redis`是基于第三缓存服务存储消息，流式输出完消息后释放信息，缓存方式您可以使用`Redis`方式，配置MAS进行消息存储的工作，配置如下

```yaml
#请在oxygent-studio项目resources资源文件下的application.yml配置文件oxygent.cache redis
oxygent:
  cache: redis
  redis:
    host: 127.0.0.1 #Redis 服务器地址（Docker 容器运行在本机时为 127.0.0.1）
    port: 6379 #Redis 端口
    database: 0 #Redis 数据库索引（0-15）
    password: you_password # 连接密码（与 Docker 启动参数一致）
    timeout: 5000 # 连接超时时间（单位：毫秒）
```

## 本地Docker启动Redis
本地开发便于快速启动Redis服务，命令参数如下

```bash
docker run --name my-redis -d -p 6379:6379 redis redis-server --requirepass you_password
```

[上一章：如何选择智能体](./01-05-select_agent.md)
[下一章：注册一个工具](./02-01-register_single_tool.md)
[回到首页](./readme.md)