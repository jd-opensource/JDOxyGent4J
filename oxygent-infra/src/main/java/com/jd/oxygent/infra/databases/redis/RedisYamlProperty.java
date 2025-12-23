package com.jd.oxygent.infra.databases.redis;

import com.jd.oxygent.core.Config;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author xianmeng
 * @date 2025/11/26
 */
@ConfigurationProperties(prefix = "oxygent.redis")
@Component
@Data
public class RedisYamlProperty {
    private String host = Config.getRedis().getHost();
    private int port = Config.getRedis().getPort();
    private int database = Config.getRedis().getDatabase();
    private String password = Config.getRedis().getPassword();
    private int timeout = Config.getRedis().getTimeout(); // ms

}