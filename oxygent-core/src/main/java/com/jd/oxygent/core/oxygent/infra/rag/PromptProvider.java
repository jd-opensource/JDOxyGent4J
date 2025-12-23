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
package com.jd.oxygent.core.oxygent.infra.rag;

import java.util.List;

/**
 * Prompt Provider Interface
 * <p>
 * Provides intelligent prompt generation and management capabilities for the OxyGent system,
 * supporting dynamic prompt construction based on context.
 * This interface is a key component in the RAG (Retrieval-Augmented Generation) architecture,
 * responsible for combining retrieved knowledge with user intent to generate high-quality LLM prompts.
 *
 * <h3>Architecture Design</h3>
 * <ul>
 *     <li>Template Management: Provides configurable prompt templates with support for dynamic parameter replacement</li>
 *     <li>Context Awareness: Adjusts prompt strategies based on business scenarios and user intent</li>
 *     <li>Knowledge Fusion: Organically integrates retrieved relevant knowledge into prompts</li>
 *     <li>Diversified Generation: Supports specialized prompt generation for different task types</li>
 * </ul>
 *
 * <h3>Core Functions</h3>
 * <ul>
 *     <li>Dynamic Construction: Dynamically builds personalized prompts based on input data and scenarios</li>
 *     <li>Knowledge Injection: Reasonably injects relevant knowledge fragments into prompts</li>
 *     <li>Format Optimization: Ensures prompt format follows best practices for target LLM</li>
 *     <li>Length Control: Intelligently controls prompt length based on model limitations</li>
 * </ul>
 *
 * <h3>Application Scenarios</h3>
 * <ul>
 *     <li>Q&A Systems: Generates prompts containing relevant background knowledge for Q&A tasks</li>
 *     <li>Code Generation: Combines API documentation and example code to generate programming prompts</li>
 *     <li>Document Writing: Generates writing assistance prompts based on reference materials</li>
 *     <li>Task Planning: Generates task execution prompts based on historical experience</li>
 * </ul>
 *
 * <h3>Prompt Optimization Strategies</h3>
 * <ul>
 *     <li>Structured Organization: Clear role definition, task description, example demonstration</li>
 *     <li>Context Control: Reasonable control of context length, highlighting important information</li>
 *     <li>Clear Instructions: Uses clear, specific instructions to avoid ambiguity</li>
 *     <li>Format Standards: Follows format preferences and best practices of target model</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public interface PromptProvider {

    /**
     * Generates prompts based on business data and scenarios
     * <p>
     * Dynamically generates prompts suitable for current tasks based on input business data object list
     * and scenario identifier. This method intelligently converts business data into LLM-understandable
     * context and constructs optimal prompt structure combined with scenario characteristics.
     *
     * <h4>Data Processing Flow</h4>
     * <ul>
     *     <li>Data Parsing: Extracts key information and attributes from business objects</li>
     *     <li>Relevance Filtering: Filters the most relevant data based on scenario requirements</li>
     *     <li>Structured Organization: Organizes data into prompt fragments according to logical structure</li>
     *     <li>Template Application: Applies prompt templates corresponding to scenarios</li>
     * </ul>
     *
     * <h4>Scenario Adaptation Strategies</h4>
     * <ul>
     *     <li>Q&A Scenarios: Highlights relevant knowledge and context information</li>
     *     <li>Code Scenarios: Includes API documentation, example code, best practices</li>
     *     <li>Analysis Scenarios: Provides data background, analysis framework, reference standards</li>
     *     <li>Creative Scenarios: Gives writing requirements, reference materials, style guidance</li>
     * </ul>
     *
     * <h4>Quality Assurance Mechanisms</h4>
     * <ul>
     *     <li>Length Control: Intelligently truncates and optimizes based on model token limits</li>
     *     <li>Format Check: Ensures prompt format meets target model requirements</li>
     *     <li>Content Deduplication: Avoids duplicate information affecting model understanding</li>
     *     <li>Sensitive Word Filtering: Ensures prompt content is compliant and safe</li>
     * </ul>
     *
     * @param beans Business data object list containing context information needed to build prompts, cannot be null
     * @param key   Scenario identifier used to select corresponding prompt template and generation strategy, cannot be null or empty string
     * @return Complete prompt generated based on input data and scenario, formatted and ready for direct LLM invocation
     * @throws IllegalArgumentException when parameters are invalid or scenario identifier is not supported
     * @throws RuntimeException         when errors occur during prompt generation process
     */
    String getPrompt(List<?> beans, String key);
}
