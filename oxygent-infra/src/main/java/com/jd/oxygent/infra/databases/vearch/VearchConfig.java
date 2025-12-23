/*
 * Copyright 2025 JD.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this project except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.oxygent.infra.databases.vearch;

import com.jd.oxygent.core.oxygent.infra.dataobject.vearch.client.SpaceClient;
import com.jd.oxygent.core.oxygent.infra.dataobject.vearch.client.SpaceOperation;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Vearch vector database configuration class
 * <p>
 * Provides unified configuration management for Vearch vector database for OxyGent system,
 * supporting multi-environment deployment and dynamic configuration.
 * This configuration class integrates a complete configuration system including vector database
 * connections, RAG model parameters, authentication information, etc.
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
@Data
@EnableConfigurationProperties(VearchConfig.VearchProperties.class)
public class VearchConfig {

    /**
     * Vearch configuration properties
     */
    @Autowired
    private VearchProperties vearchProperties;

    public SpaceClient getSpaceClient(String spaceName) {
        boolean hasUsername = StringUtils.hasText(vearchProperties.username);
        boolean hasToken = StringUtils.hasText(vearchProperties.token);
        if (hasUsername && hasToken) {
            // 1. Use authentication mode only when both username and token exist
            return new SpaceOperation(vearchProperties.url, vearchProperties.username, vearchProperties.token, spaceName);
        } else if (!hasUsername && !hasToken) {
            // 2. Use non-authentication mode when neither username nor token exists
            return new SpaceOperation(vearchProperties.url, spaceName);
        } else {
            // 3. Other cases (one exists, one doesn't) are configuration errors
            throw new IllegalStateException(
                    "Vearch configuration is inconsistent: Both username and token must be provided for authentication, or neither."
            );
        }
    }


    /**
     * Vearch database configuration properties class
     * <p>
     * Defines all configuration parameters required for Vearch database connection and RAG functionality.
     * Supports externalized configuration through Spring Boot configuration files.
     */
    @ConfigurationProperties(prefix = "vearch")
    @Data
    public static class VearchProperties {

        /**
         * Vearch service connection address
         * <p>
         * Format: http://host:port or https://host:port
         * Example: http://vearch.example.com:9001
         */
        private String url;

        /**
         * Vearch username
         * <p>
         * Username for authentication, must be provided if Vearch has authentication enabled
         */
        private String username;

        /**
         * Vearch Token
         * <p>
         * Token for authentication, must be provided if Vearch has authentication enabled
         */
        private String token;

    }
}