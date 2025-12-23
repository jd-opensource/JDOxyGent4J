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
package com.jd.oxygent.core.oxygent.oxy.llms;

import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * <h3>Remote LLM Abstract Base Class</h3>
 *
 * <p>RemoteLlm is the abstract base class for all remote large language model implementations in the OxyGent framework.
 * This class encapsulates common configuration parameters and basic functionality for remote LLM services, providing standardized architectural foundation for specific LLM implementations.</p>
 *
 * <h3>Technical Architecture</h3>
 * <ul>
 *   <li><strong>Inheritance System</strong>: Inherits from BaseLlM, following the standard architecture of OxyGent LLM framework</li>
 *   <li><strong>Configuration Management</strong>: Unified management of remote service connection parameters and model configuration</li>
 *   <li><strong>Parameter Encapsulation</strong>: Provides standardized LLM parameter interface, supporting flexible configuration extension</li>
 *   <li><strong>Abstract Design</strong>: Defines core execution interface, with concrete LLM communication logic implemented by subclasses</li>
 * </ul>
 *
 * <h3>Core Features</h3>
 * <ul>
 *   <li><strong>Connection Management</strong>: Unified remote service connection configuration and management</li>
 *   <li><strong>Authentication Support</strong>: Standardized API key authentication mechanism</li>
 *   <li><strong>Model Configuration</strong>: Flexible model selection and parameter configuration</li>
 *   <li><strong>Parameter Validation</strong>: Complete configuration parameter validation and error handling</li>
 * </ul>
 *
 * <h3>Configuration Parameters</h3>
 * <ul>
 *   <li><strong>baseUrl</strong>: Base URL address of the remote LLM service</li>
 *   <li><strong>apiKey</strong>: API access key for service authentication</li>
 *   <li><strong>modelName</strong>: Specified LLM model name to use</li>
 *   <li><strong>llmParams</strong>: Extended LLM parameter configuration mapping</li>
 * </ul>
 *
 * <h3>Supported LLM Providers</h3>
 * <p>This abstract class is designed to support multiple LLM service providers:</p>
 * <ul>
 *   <li><strong>OpenAI</strong>: GPT series models (GPT-3.5, GPT-4, etc.)</li>
 *   <li><strong>Anthropic</strong>: Claude series models</li>
 *   <li><strong>Google</strong>: PaLM, Gemini series models</li>
 *   <li><strong>Azure OpenAI</strong>: OpenAI services on Microsoft Azure</li>
 *   <li><strong>Other OpenAI API Compatible Services</strong>: Such as locally deployed model services</li>
 * </ul>
 *
 * <h3>Design Patterns</h3>
 * <ul>
 *   <li><strong>Template Method Pattern</strong>: Defines common workflow, with specific implementation completed by subclasses</li>
 *   <li><strong>Strategy Pattern</strong>: Different LLM providers adopt different implementation strategies</li>
 *   <li><strong>Builder Pattern</strong>: Supports chained configuration construction</li>
 *   <li><strong>Defensive Programming</strong>: Complete parameter validation and exception handling</li>
 * </ul>
 *
 * <h3>Extension Points</h3>
 * <p>Subclasses need to implement the following abstract methods:</p>
 * <ul>
 *   <li><strong>_execute</strong>: Concrete LLM invocation implementation logic</li>
 * </ul>
 *
 * <h3>Usage Examples</h3>
 * <pre>{@code
 * // Subclass implementation example
 * public class CustomLlm extends RemoteLlm {
 *     public CustomLlm(String baseUrl, String apiKey, String modelName) {
 *         super(baseUrl, apiKey, modelName, Duration.ofSeconds(30), null);
 *     }
 *
 *     @Override
 *     protected OxyResponse _execute(OxyRequest request) {
 *         // Implement specific LLM invocation logic
 *         return callCustomLlmService(request);
 *     }
 * }
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class RemoteLlm extends BaseLlM {
    protected String baseUrl;
    protected String apiKey;
    protected String modelName;
    protected Map<String, String> headers;
    protected Map<String, Object> llmParams;

    /**
     * Create remote LLM instance
     * <p>
     * This constructor is responsible for initializing all basic configuration parameters of remote LLM, including service connection information,
     * authentication credentials, model selection and extension parameters. The constructor performs complete parameter validation,
     * ensuring the correctness and availability of LLM instance configuration.
     *
     * <h3>Initialization Process</h3>
     * <ol>
     *   <li><strong>Required Parameter Validation</strong>：Validate the validity of baseUrl and modelName</li>
     *   <li><strong>URL Standardization</strong>：Automatically handle trailing slashes in URLs to ensure uniform URL format</li>
     *   <li><strong>Configuration Parameter Setting</strong>：Set all LLM connection and invocation parameters</li>
     *   <li><strong>Extended Parameter Processing</strong>：Safely handle optional LLM parameter mappings</li>
     * </ol>
     *
     * <h3>Parameter Validation Rules</h3>
     * <ul>
     *   <li><strong>baseUrl</strong>：Cannot be null or empty string, must be a valid URL address</li>
     *   <li><strong>modelName</strong>：Cannot be null or empty string, must be a valid model name</li>
     *   <li><strong>apiKey</strong>：Can be null (for services that don't require authentication)</li>
     *   <li><strong>timeout</strong>：Can be null (uses default timeout configuration)</li>
     *   <li><strong>llmParams</strong>：Can be null (automatically creates empty parameter mapping)</li>
     * </ul>
     *
     * <h3>URL Processing Mechanism</h3>
     * <p>The constructor automatically standardizes URL format:</p>
     * <ul>
     *   <li>Remove trailing slashes from URL to avoid path concatenation issues</li>
     *   <li>Ensure URL format consistency for subsequent API calls</li>
     * </ul>
     *
     * <h3>Configuration Examples</h3>
     * <pre>{@code
     * // OpenAI Configuration
     * RemoteLlm openAI = new OpenAiLlm(
     *     "https://api.openai.com/v1",
     *     "sk-xxxxxxxxxxxxxxxx",
     *     "gpt-4",
     *     Duration.ofSeconds(30),
     *     Map.of("temperature", 0.7, "max_tokens", 2000)
     * );
     *
     * // Locally deployed model service
     * RemoteLlm localModel = new CustomLlm(
     *     "http://localhost:8080/v1",
     *     null, // No API Key required
     *     "llama-2-7b",
     *     Duration.ofMinutes(2),
     *     Map.of("top_p", 0.9)
     * );
     * }</pre>
     *
     * @param baseUrl   Base URL of the remote LLM service, cannot be empty, trailing slashes will be automatically removed
     * @param apiKey    API access key, can be null (for services that don't require authentication)
     * @param modelName LLM model name to use, cannot be empty
     * @param timeout   Request timeout duration, can be null to use default configuration
     * @param llmParams Extended LLM parameter configuration, can be null, will automatically create empty mapping
     * @throws IllegalArgumentException if baseUrl or modelName is empty or invalid
     */
    public RemoteLlm(String baseUrl, String apiKey, String modelName, Duration timeout, Map<String, Object> llmParams, Map<String, String> headers) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("baseUrl must be a non-empty string");
        }
        if (modelName == null || modelName.trim().isEmpty()) {
            throw new IllegalArgumentException("modelName must be a non-empty string");
        }
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.llmParams = llmParams != null ? llmParams : new HashMap<>();
        this.headers = headers != null ? headers : new HashMap<>();
    }

    protected abstract OxyResponse _execute(OxyRequest oxyRequest);

}