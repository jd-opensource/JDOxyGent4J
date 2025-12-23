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
package com.jd.oxygent.core.oxygent.oxy.agents;

import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.function.Function;

/**
 * RAG Agent - Retrieval-Augmented Generation Agent
 *
 * <p>RAGAgent is an agent based on Retrieval-Augmented Generation (RAG) technology,
 * inheriting from ChatAgent with added knowledge retrieval capabilities. It can retrieve relevant information
 * from knowledge bases before generating answers, significantly improving answer accuracy and reliability.</p>
 *
 * <p>Core features:</p>
 * <ul>
 *     <li>Knowledge retrieval: Supports dynamic knowledge retrieval and context enhancement</li>
 *     <li>Template injection: Automatically injects retrieved knowledge into prompt templates</li>
 *     <li>Flexible configuration: Supports custom knowledge placeholders and retrieval functions</li>
 *     <li>Fault tolerance: Retrieval failures do not affect basic conversational functionality</li>
 *     <li>Multi-source knowledge: Supports integration of multiple knowledge sources</li>
 * </ul>
 *
 * <p>Workflow:</p>
 * <ol>
 *     <li>Receive user query</li>
 *     <li>Retrieve relevant knowledge based on query content</li>
 *     <li>Inject knowledge into prompt template</li>
 *     <li>Call LLM to generate knowledge-based answer</li>
 *     <li>Return enhanced response result</li>
 * </ol>
 *
 * <p>Application scenarios:</p>
 * <ul>
 *     <li>Enterprise knowledge Q&amp;A: Q&amp;A based on internal documents and knowledge bases</li>
 *     <li>Technical support: Technical consulting combined with product documentation</li>
 *     <li>Academic research: Academic Q&amp;A based on papers and research materials</li>
 *     <li>Legal consulting: Legal Q&amp;A combined with legal provisions and cases</li>
 *     <li>Medical consulting: Health Q&amp;A based on medical knowledge bases</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * // Create knowledge retrieval function
 * Function<OxyRequest, String> knowledgeRetriever = request -> {
 *     String query = request.getQuery();
 *     // Implement specific knowledge retrieval logic
 *     return searchKnowledgeBase(query);
 * };
 *
 * // Build RAG agent
 * RAGAgent ragAgent = RAGAgent.builder()
 *     .name("Knowledge Q&A Assistant")
 *     .prompt("You are a professional knowledge Q&A assistant. Please answer user questions based on the following knowledge:\n${knowledge}")
 *     .llmModel("gpt-4")
 *     .knowledgePlaceholder("knowledge")
 *     .funcRetrieveKnowledge(knowledgeRetriever)
 *     .build();
 *
 * // Execute query
 * OxyRequest request = OxyRequest.builder()
 *     .query("What is machine learning?")
 *     .build();
 *
 * OxyResponse response = ragAgent.execute(request);
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@ToString(callSuper = true)
@Slf4j
public class RAGAgent extends ChatAgent {

    /**
     * Default knowledge placeholder
     * Use ${knowledge} in prompt template to identify knowledge injection position
     */
    protected static String DEFAULT_KNOWLEDGE_PLACEHOLDER = "knowledge";

    /**
     * Default RAG prompt template
     * Contains knowledge placeholder, used when custom prompt is not set
     */
    protected String DEFAULT_RAG_PROMPT = """
            You are a professional knowledge Q&A assistant. Please answer user questions based on the provided relevant knowledge.
            
            Relevant knowledge:
            ${knowledge}
            
            Please answer user questions accurately based on the above knowledge. If there is no relevant information in the knowledge, please be honest about it.
            """;

    /**
     * Knowledge placeholder
     * Variable name that identifies knowledge injection position in prompt template, default is "knowledge"
     * -- GETTER --
     * Get current knowledge placeholder
     */
    @Builder.Default
    protected String knowledgePlaceholder = DEFAULT_KNOWLEDGE_PLACEHOLDER;

    /**
     * Knowledge retrieval function
     *
     * <p>Functional interface for retrieving relevant knowledge based on user requests. This function receives
     * OxyRequest object as input and returns retrieved knowledge string. Supports various retrieval strategies:</p>
     * <ul>
     *     <li>Vector similarity retrieval: Semantic retrieval based on embeddings</li>
     *     <li>Keyword retrieval: Keyword matching based on algorithms like BM25</li>
     *     <li>Hybrid retrieval: Comprehensive methods combining multiple retrieval strategies</li>
     *     <li>Database queries: Retrieve information from structured databases</li>
     * </ul>
     * -- GETTER --
     *  Get knowledge retrieval function
     *  <p>Returns currently configured knowledge retrieval function, mainly for testing and debugging purposes.</p>
     */
    protected Function<OxyRequest, String> funcRetrieveKnowledge;

    /**
     * Set default RAG prompt
     *
     * <p>If user has not provided custom prompt, use default template containing knowledge placeholder.
     * Default template guides LLM to answer questions based on retrieved knowledge.</p>
     */
    @Override
    protected void setDefaultPrompt() {
        if (this.prompt == null || this.prompt.trim().isEmpty()) {
            this.prompt = DEFAULT_RAG_PROMPT.replace("${knowledge}", "${" + this.knowledgePlaceholder + "}");
            log.debug("Set default prompt for RAGAgent: {}", this.getName());
        }
    }

    /**
     * RAG agent initialization
     *
     * <p>Execute complete initialization process of RAG agent:</p>
     * <ol>
     *     <li>Call initialization logic of parent class ChatAgent</li>
     *     <li>Set default RAG prompt template</li>
     *     <li>Validate configuration of knowledge retrieval function</li>
     *     <li>Set default empty knowledge retrieval function (if not configured)</li>
     * </ol>
     *
     * @throws IllegalStateException if necessary configuration is not properly set
     */
    @Override
    public void init() {
        super.init();
        setDefaultPrompt();

        // Validate and configure knowledge retrieval function
        if (funcRetrieveKnowledge == null) {
            log.warn("RAGAgent {} has no knowledge retrieval function configured, will use empty implementation", this.getName());
            // Set default empty implementation to avoid runtime errors
            funcRetrieveKnowledge = request -> "";
        }

        log.debug("RAGAgent initialization completed: {}, knowledge placeholder: {}",
                this.getName(), this.knowledgePlaceholder);
    }

    /**
     * Preprocess request - Retrieve and inject knowledge
     *
     * <p>This is the core method of RAG agent, executing knowledge retrieval and injection before LLM call:</p>
     * <ol>
     *     <li>Call parent class preprocessing logic</li>
     *     <li>Use configured retrieval function to retrieve relevant knowledge</li>
     *     <li>Inject retrieved knowledge into request parameters</li>
     *     <li>Handle exceptions during retrieval process to ensure system stability</li>
     * </ol>
     *
     * <p>Knowledge injection strategy:</p>
     * <ul>
     *     <li>Successful retrieval: Inject knowledge content to specified placeholder position</li>
     *     <li>Retrieval failure: Inject empty string to ensure basic conversational functionality is not affected</li>
     *     <li>Exception handling: Log error messages, continue execution with empty knowledge</li>
     * </ul>
     *
     * @param oxyRequest Original request object, cannot be null
     * @return Enhanced request object containing retrieved knowledge
     * @throws IllegalArgumentException if oxyRequest is null
     */
    @Override
    protected OxyRequest preProcess(OxyRequest oxyRequest) {
        Objects.requireNonNull(oxyRequest, "OxyRequest cannot be null");

        // Call parent class preprocessing logic
        var processedRequest = super.preProcess(oxyRequest);

        try {
            // Execute knowledge retrieval
            var knowledge = retrieveKnowledge(processedRequest);

            // Inject knowledge into request parameters
            processedRequest.getArguments().put(knowledgePlaceholder, knowledge);

            log.debug("Retrieved knowledge for query '{}': {} characters",
                    processedRequest.getQuery(),
                    knowledge != null ? knowledge.length() : 0);

        } catch (Exception e) {
            log.error("RAGAgent knowledge retrieval failed: {}", e.getMessage(), e);
            // Set empty knowledge to ensure basic functionality is not affected
            processedRequest.getArguments().put(knowledgePlaceholder, "");
        }

        return processedRequest;
    }

    /**
     * Execute knowledge retrieval
     *
     * <p>Call configured knowledge retrieval function to get knowledge content related to user query.</p>
     *
     * @param request Processed request object
     * @return Retrieved knowledge string, returns empty string if retrieval fails or no relevant knowledge
     */
    private String retrieveKnowledge(OxyRequest request) {
        if (funcRetrieveKnowledge == null) {
            log.debug("Knowledge retrieval function not configured, returning empty knowledge");
            return "";
        }

        try {
            var knowledge = funcRetrieveKnowledge.apply(request);
            var knowledgeLength = knowledge != null ? knowledge.length() : 0;

            if (knowledgeLength > 0) {
                var preview = knowledgeLength > 200 ?
                        knowledge.substring(0, 200) + "..." : knowledge;
                log.debug("Successfully retrieved knowledge, length: {} characters, preview: {}", knowledgeLength, preview);
            } else {
                log.debug("Knowledge retrieval completed, but no relevant content found");
            }

            return knowledge != null ? knowledge : "";

        } catch (Exception e) {
            log.warn("Exception occurred during knowledge retrieval: {}", e.getMessage());
            return "";
        }
    }


    /**
     * Set knowledge retrieval function
     *
     * <p>Configure functional interface for retrieving knowledge. This function will be called every time
     * a user request is processed, retrieving relevant knowledge based on request content.</p>
     *
     * <p>Retrieval function design recommendations:</p>
     * <ul>
     *     <li>Efficiency: Implementation should respond quickly, avoid long-term blocking</li>
     *     <li>Relevance: Return knowledge content most relevant to the query</li>
     *     <li>Exception handling: Handle exceptions internally, avoid affecting overall process</li>
     *     <li>Formatting: Return formatted knowledge content for easy LLM understanding</li>
     * </ul>
     *
     * @param retrieveFunction Knowledge retrieval function, receives OxyRequest and returns knowledge string, cannot be null
     * @throws IllegalArgumentException if retrieveFunction is null
     */
    public void setFuncRetrieveKnowledge(Function<OxyRequest, String> retrieveFunction) {
        Objects.requireNonNull(retrieveFunction, "Knowledge retrieval function cannot be null");
        this.funcRetrieveKnowledge = retrieveFunction;
        log.debug("RAGAgent {} knowledge retrieval function updated", this.getName());
    }

    /**
     * Set knowledge placeholder
     *
     * <p>Update placeholder name used to identify knowledge injection position in prompt template.
     * If currently using default prompt, will automatically update template to use new placeholder.</p>
     *
     * @param placeholder New knowledge placeholder name, cannot be empty
     * @throws IllegalArgumentException if placeholder is null or empty string
     */
    public void setKnowledgePlaceholder(String placeholder) {
        Objects.requireNonNull(placeholder, "Knowledge placeholder cannot be null");
        if (placeholder.trim().isEmpty()) {
            throw new IllegalArgumentException("Knowledge placeholder cannot be empty string");
        }

        this.knowledgePlaceholder = placeholder.trim();

        // If currently using default prompt, update template to use new placeholder
        if (this.prompt != null && this.prompt.contains("${knowledge}")) {
            setDefaultPrompt();
            log.debug("RAGAgent {} knowledge placeholder updated to: {}", this.getName(), this.knowledgePlaceholder);
        }
    }

}