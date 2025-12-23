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
package com.jd.oxygent.core.oxygent.schemas.contextengineer.model;

import java.util.Map;

/**
 * <h3>Large Language Model (LLM) Configuration Record</h3>
 *
 * <p>This record class encapsulates the complete configuration information of a large language model (LLM), used to standardize connection and invocation parameters for LLM services.
 * Implemented as a Java Record type, it provides an immutable configuration object to ensure consistency and thread safety.</p>
 *
 * <h3>Configuration Components:</h3>
 * <ul>
 *   <li><strong>model</strong>: Model name identifier, e.g., GPT-4, Claude, etc.</li>
 *   <li><strong>apiKey</strong>: API access key for authentication</li>
 *   <li><strong>baseUrl</strong>: Base URL address of the LLM service</li>
 *   <li><strong>llmParams</strong>: Additional model parameter configuration</li>
 * </ul>
 *
 * <h3>Supported LLM Types:</h3>
 * <ul>
 *   <li><strong>Embedding Models</strong>: Text vectorization models, e.g., text-embedding-ada-002</li>
 *   <li><strong>Chat Models</strong>: Dialogue generation models, e.g., gpt-3.5-turbo, gpt-4</li>
 *   <li><strong>Reranker Models</strong>: Document reranking models for RAG optimization</li>
 *   <li><strong>Code Models</strong>: Code generation and understanding models</li>
 * </ul>
 *
 * <h3>Usage Scenarios:</h3>
 * <ul>
 *   <li>Initialization configuration for LLM clients</li>
 *   <li>Dynamic switching between multiple models</li>
 *   <li>Model configuration management in RAG systems</li>
 *   <li>Configuration passing in distributed environments</li>
 * </ul>
 *
 * <h3>Configuration Example:</h3>
 * <pre>{@code
 * // OpenAI GPT-4 configuration
 * LLMCfg gpt4Config = new LLMCfg(
 *     "gpt-4",
 *     "sk-your-api-key",
 *     "https://api.openai.com/v1",
 *     Map.of("temperature", 0.7, "maxTokens", 2000)
 * );
 *
 * // Local Embedding model configuration
 * LLMCfg embeddingConfig = new LLMCfg(
 *     "text-embedding-ada-002",
 *     "your-api-key",
 *     "https://your-service.com/v1",
 *     Map.of("dimensions", 1536)
 * );
 * }</pre>
 *
 * <h3>Security Notes:</h3>
 * <ul>
 *   <li>API keys should be managed via environment variables or secure configuration</li>
 *   <li>Avoid logging sensitive configuration information</li>
 *   <li>Production environments should use HTTPS protocol for baseUrl</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public record LLMCfg(
        /**
         * Model name identifier
         * <p>Specifies the exact LLM model name to use; different models have different capabilities and features.</p>
         *
         * <h4>Common Model Examples:</h4>
         * <ul>
         *   <li><strong>OpenAI Series</strong>: "gpt-4", "gpt-3.5-turbo", "text-embedding-ada-002"</li>
         *   <li><strong>Anthropic Series</strong>: "claude-3-opus", "claude-3-sonnet"</li>
         *   <li><strong>Open Source Models</strong>: "llama2-70b", "mistral-7b"</li>
         *   <li><strong>Chinese Models</strong>: "deepseek-R1", "chatglm-6b"</li>
         * </ul>
         */
        String model,

        /**
         * API access key
         * <p>Authentication key for accessing the LLM service, must be kept confidential.</p>
         *
         * <h4>Key Format Examples:</h4>
         * <ul>
         *   <li>OpenAI: "sk-xx"</li>
         *   <li>Other services: according to specific provider format requirements</li>
         * </ul>
         *
         * <h4>Security Recommendations:</h4>
         * <ul>
         *   <li>Pass via environment variables, avoid hardcoding</li>
         *   <li>Rotate keys regularly</li>
         *   <li>Restrict key access permissions</li>
         * </ul>
         */
        String apiKey,

        /**
         * LLM service base URL
         * <p>Root address of the LLM API service, usually does not include specific API path.</p>
         *
         * <h4>URL Format Examples:</h4>
         * <ul>
         *   <li>OpenAI official: "https://api.openai.com/v1"</li>
         *   <li>Azure OpenAI: "https://your-resource.openai.azure.com"</li>
         *   <li>Self-hosted: "https://your-llm-service.com/v1"</li>
         *   <li>Local service: "http://localhost:8080/v1"</li>
         * </ul>
         *
         * <p>Note: Production environments should use HTTPS protocol for secure transmission.</p>
         */
        String baseUrl,

        /**
         * LLM invocation parameters
         * <p>Additional model invocation parameters for fine-tuning model behavior and output.</p>
         *
         * <h4>Common Parameter Examples:</h4>
         * <ul>
         *   <li><strong>temperature</strong>: Controls output randomness (0.0-2.0)</li>
         *   <li><strong>maxTokens</strong>: Maximum output token limit</li>
         *   <li><strong>topP</strong>: Nucleus sampling parameter (0.0-1.0)</li>
         *   <li><strong>frequencyPenalty</strong>: Frequency penalty (-2.0-2.0)</li>
         *   <li><strong>presencePenalty</strong>: Presence penalty (-2.0-2.0)</li>
         *   <li><strong>dimensions</strong>: Vector dimension (for embedding models only)</li>
         * </ul>
         *
         * <p>If parameter is null, model default settings are used; if Map is empty, no extra parameters are passed.</p>
         */
        Map<String, Object> llmParams) {
}