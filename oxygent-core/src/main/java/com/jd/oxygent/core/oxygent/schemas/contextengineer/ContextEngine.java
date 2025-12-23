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
package com.jd.oxygent.core.oxygent.schemas.contextengineer;

import com.github.f4b6a3.uuid.UuidCreator;
import com.jd.oxygent.core.oxygent.infra.dataobject.vearch.VdbDO;
import com.jd.oxygent.core.oxygent.infra.rag.Knowledge;
import com.jd.oxygent.core.oxygent.schemas.contextengineer.model.AgentContext;
import com.jd.oxygent.core.oxygent.schemas.contextengineer.model.LongMemory;
import com.jd.oxygent.core.oxygent.schemas.contextengineer.rag.RagService;
import com.jd.oxygent.core.oxygent.schemas.contextengineer.rag.chunker.Chunker;
import com.jd.oxygent.core.oxygent.schemas.memory.Memory;
import com.jd.oxygent.core.oxygent.schemas.memory.Message;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <h3>Context Engineering Core Engine</h3>
 *
 * <p>This class is the core context engineering component of the OxyGent intelligent agent framework,
 * responsible for building and managing the runtime context environment for Agents.
 * It integrates core functions such as long-term and short-term memory management, RAG retrieval enhancement,
 * and knowledge base integration to provide intelligent context-aware capabilities for Agents.</p>
 *
 * <h3>Core Functions:</h3>
 * <ul>
 *   <li><strong>Context Construction</strong>: Build adaptive context environments for different scenarios</li>
 *   <li><strong>Memory Management</strong>: Integrate long-term and short-term memory to form complete context</li>
 *   <li><strong>Knowledge Retrieval</strong>: Enhance context relevance through RAG technology</li>
 *   <li><strong>History Indexing</strong>: Index conversation history to vector database</li>
 * </ul>
 *
 * <h3>Context Types:</h3>
 * <ul>
 *   <li><strong>React Context</strong>: Suitable for reasoning-action-observation loop Agent patterns</li>
 *   <li><strong>Default Context</strong>: Standard multi-turn conversation context construction</li>
 *   <li><strong>Enhanced Context</strong>: Rich context integrating RAG and knowledge base</li>
 * </ul>
 *
 * <h3>Memory Structure:</h3>
 * <ol>
 *   <li><strong>System Prompt</strong>: Basic behavioral guidance for Agent</li>
 *   <li><strong>Long-term Memory</strong>: Facts, history, and profile information</li>
 *   <li><strong>Short-term Memory</strong>: Message sequence of current session</li>
 *   <li><strong>Tool Results</strong>: Feedback information from tool execution</li>
 * </ol>
 *
 * <h3>Usage Scenarios:</h3>
 * <ul>
 *   <li>Intelligent Q&amp;A system context construction</li>
 *   <li>Multi-turn conversation scenario memory management</li>
 *   <li>Knowledge-enhanced Agent support</li>
 *   <li>Personalized conversation experience construction</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Component
public class ContextEngine {

    /**
     * RAG retrieval-augmented generation component for relevant knowledge retrieval
     */
    @Autowired
    private RagService ragService;

    /**
     * Knowledge base component providing structured knowledge access capabilities
     */
    @Autowired
    private Knowledge knowledge;

    private static String spaceName = "long_memory_space";

    /**
     * Build React mode context
     * <p>Build dedicated context environment for reasoning-action-observation (React) mode Agent</p>
     *
     * <h4>Construction Process:</h4>
     * <ol>
     *   <li>Integrate long-term memory information (history, facts, profile)</li>
     *   <li>Merge with system prompt to form system message</li>
     *   <li>Add historical conversations from short-term memory</li>
     *   <li>Append current user input</li>
     * </ol>
     *
     * <h4>Applicable Scenarios:</h4>
     * <ul>
     *   <li>Task processing requiring complex reasoning</li>
     *   <li>Multi-step problem solving</li>
     *   <li>Tool-call intensive scenarios</li>
     * </ul>
     *
     * @param agentContext Agent context configuration containing prompts and memory information
     * @return Constructed Memory object containing complete context information
     */
    public Memory buildReactContext(AgentContext agentContext) {
        Memory memory = new Memory();
        if (agentContext.getLongMemory() != null) {
            String longMemory = agentContext.getLongMemory().getHistory()
                    + agentContext.getLongMemory().getFacts()
                    + agentContext.getLongMemory().getProfile();
            memory.addMessage(Message.systemMessage(agentContext.systemPrompt + longMemory));
            log.info("Long memory content: {}", longMemory);
        }
        memory.addMessages(agentContext.shortMemory.getMessages());
        memory.addMessage(Message.userMessage(agentContext.userPrompt));
        return memory;
    }

    /**
     * Build default context
     * <p>Build standard multi-turn conversation context, supporting long-term and short-term memory integration and tool execution result processing</p>
     *
     * <h4>Context Composition Structure:</h4>
     * <ol>
     *   <li><strong>System Message</strong>: Basic prompt + categorized long-term memory</li>
     *   <li><strong>Historical Conversations</strong>: Message sequence from short-term memory</li>
     *   <li><strong>User Input</strong>: Current round user query</li>
     *   <li><strong>Tool Results</strong>: Feedback from previous tool execution (if any)</li>
     * </ol>
     *
     * <h4>Memory Organization Format:</h4>
     * <pre>
     * System Prompt
     *
     * 【Facts】
     * Factual information...
     *
     * 【History】
     * Historical interaction records...
     *
     * 【Profile】
     * User profile information...
     * </pre>
     *
     * @param ctx Agent context object containing complete context information
     * @return Constructed Memory object with 20-message limit to control context length
     */
    public Memory buildDefaultContext(AgentContext ctx) {
        Memory memory = new Memory(20);   // 20-message limit, can be adjusted as needed

        // 1. system: template first, then append long-term memory sequentially, separated by newlines + titles
        String system = ctx.systemPrompt
                + "\n\n【Facts】\n" + ctx.longMemory.getFacts()
                + "\n\n【History】\n" + ctx.longMemory.getHistory()
                + "\n\n【Profile】\n" + ctx.longMemory.getProfile();
        memory.addMessage(Message.systemMessage(system));

        // 2. multi-turn conversation: shortMemory already contains (A1,U1,S1…An,Un,Sn) sequence, copy directly
        memory.addMessages(ctx.shortMemory.getMessages());

        // 3. finally user input
        memory.addMessage(Message.userMessage(ctx.userPrompt));

        // 4. tool execution results (if any): role=tool, appended at the end
        if (ctx.toolResults != null) {
            for (Message m : ctx.toolResults) {
                memory.addMessage(m);
            }
        }

        return memory;
    }

    /**
     * Build long-term memory
     * <p>Build relevant long-term memory content based on user query through knowledge base retrieval and RAG technology</p>
     *
     * <h4>Construction Process:</h4>
     * <ol>
     *   <li>Extract key information from user query</li>
     *   <li>Retrieve relevant fact fragments from knowledge base</li>
     *   <li>Retrieve relevant historical conversations through RAG</li>
     *   <li>Assemble into structured long-term memory object</li>
     * </ol>
     *
     * <h4>Memory Sources:</h4>
     * <ul>
     *   <li><strong>Knowledge Base</strong>: Structured domain knowledge and factual information</li>
     *   <li><strong>Historical Conversations</strong>: Relevant history retrieved through vector search</li>
     *   <li><strong>User Profile</strong>: Personalized user information (to be implemented)</li>
     * </ul>
     *
     * @param oxyRequest User request object containing query content and context information
     * @return LongMemory object containing retrieved relevant long-term memory content
     */
    public LongMemory buildLongMemory(OxyRequest oxyRequest) {
        String query = oxyRequest.getQuery();
        List<String> factList = knowledge.getChunkList(query, null, null);
        List<String> historyList = ragService.defaultRag(query, 5, spaceName);
        return LongMemory.builder()
                .facts(factList.toString())
                .history(historyList.toString())
                .build();
    }

    /**
     * Index conversation history
     * <p>Index Agent's response results to vector database for subsequent history retrieval and context enhancement</p>
     *
     * <h4>Indexing Process:</h4>
     * <ol>
     *   <li>Extract user query and Agent response</li>
     *   <li>Use chunker to split query into retrieval fragments</li>
     *   <li>Build vector database record objects</li>
     *   <li>Batch index to RAG system</li>
     * </ol>
     *
     * <h4>Index Data Structure:</h4>
     * <ul>
     *   <li><strong>indexText</strong>: Text fragment for retrieval</li>
     *   <li><strong>text</strong>: Complete response content</li>
     *   <li><strong>sessionId</strong>: Session identifier</li>
     *   <li><strong>userId</strong>: User identifier</li>
     *   <li><strong>createTime</strong>: Creation timestamp</li>
     * </ul>
     *
     * <h4>Application Scenarios:</h4>
     * <ul>
     *   <li>Build personalized conversation history database</li>
     *   <li>Support cross-session context continuity</li>
     *   <li>Improve relevance of subsequent queries</li>
     * </ul>
     *
     * @param response Agent's response object containing query and answer information
     * @return List of indexing operation results, typically identifiers of successfully indexed records
     */
    public List<String> indexHistory(OxyResponse response) {
        OxyRequest oxyRequest = response.getOxyRequest();
        String answer = response.getOutput().toString();
        String query = oxyRequest.getQuery();
        Chunker chunker = Chunker.paragraphs().build();
        List<String> historyList = chunker.split(query);
        List<VdbDO> vdbDOList = historyList.stream()
                .map(s ->
                        VdbDO.builder().id(UuidCreator.getShortPrefixComb().toString())
                                .userId("default" + oxyRequest.getGroupData())
                                .msgTurn(1)
                                .sessionId(oxyRequest.getSessionName())
                                .indexText(s)
                                .text(answer)
                                .createTime(System.currentTimeMillis())
                                .build())
                .toList();

        return ragService.defaultIndex(vdbDOList, spaceName);

    }
}
