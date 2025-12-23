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

import com.jd.oxygent.core.oxygent.schemas.memory.Memory;
import com.jd.oxygent.core.oxygent.schemas.memory.Message;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * <h3>Agent Context Manager</h3>
 *
 * <p>This class is the core context management component of the OxyGent agent framework, responsible for unified management of all runtime context information of agents.
 * It integrates system prompts, user input, short-term memory, long-term memory, state information, and tool execution results, providing agents with a complete runtime environment.</p>
 *
 * <h3>Context Composition Structure:</h3>
 * <ul>
 *   <li><strong>systemPrompt</strong>: System prompt, defines agent's behavioral pattern and capability boundaries</li>
 *   <li><strong>userPrompt</strong>: User input, specific query or instruction for the current turn</li>
 *   <li><strong>shortMemory</strong>: Short-term memory, maintains conversation history of the current session</li>
 *   <li><strong>longMemory</strong>: Long-term memory, contains persistent knowledge and user information</li>
 *   <li><strong>agentState</strong>: Agent state, identifies current execution phase or mode</li>
 *   <li><strong>toolResults</strong>: Tool execution results, feedback from previous tool calls</li>
 * </ul>
 *
 * <h3>Core Functions:</h3>
 * <ul>
 *   <li><strong>Context Integration</strong>: Unified management of scattered context information</li>
 *   <li><strong>State Maintenance</strong>: Tracks agent's execution state and lifecycle</li>
 *   <li><strong>Memory Management</strong>: Coordinates use of short-term and long-term memory</li>
 *   <li><strong>Tool Integration</strong>: Manages context passing for tool calls</li>
 * </ul>
 *
 * <h3>Usage Scenarios:</h3>
 * <ul>
 *   <li>Session management for intelligent dialogue systems</li>
 *   <li>State tracking for task execution agents</li>
 *   <li>Context continuity for multi-turn interactions</li>
 *   <li>Coordinated management of toolchain calls</li>
 * </ul>
 *
 * <h3>Lifecycle:</h3>
 * <ol>
 *   <li><strong>Initialization</strong>: Set system prompt and basic configuration</li>
 *   <li><strong>User Input</strong>: Receive and parse user's query request</li>
 *   <li><strong>Memory Retrieval</strong>: Retrieve relevant info from long-term memory</li>
 *   <li><strong>Context Construction</strong>: Integrate all info to form complete context</li>
 *   <li><strong>Reasoning Execution</strong>: Agent performs reasoning and decision-making based on context</li>
 *   <li><strong>Result Handling</strong>: Processes execution results and updates state</li>
 * </ol>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * AgentContext context = AgentContext.builder()
 *     .systemPrompt("You are a professional technical assistant...")
 *     .userPrompt("Please help me analyze this code")
 *     .shortMemory(currentSessionMemory)
 *     .longMemory(userLongTermMemory)
 *     .agentState("REASONING")
 *     .toolResults(previousToolOutputs)
 *     .build();
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
public class AgentContext {

    /**
     * System prompt
     * <p>Defines the agent's basic behavioral pattern, role setting, and capability boundaries.</p>
     *
     * <h4>Prompt Content:</h4>
     * <ul>
     *   <li><strong>Role Positioning</strong>: Agent's identity and professional domain</li>
     *   <li><strong>Behavior Norms</strong>: Interaction style and response format</li>
     *   <li><strong>Capability Description</strong>: List of available tools and skills</li>
     *   <li><strong>Constraints</strong>: Safety restrictions and ethical boundaries</li>
     * </ul>
     *
     * <p>The system prompt is relatively stable throughout the agent's lifecycle and is the basis for agent personality and capabilities.</p>
     */
    public String systemPrompt;

    /**
     * User input prompt
     * <p>Specific query, instruction, or request content from the user for the current turn.</p>
     *
     * <h4>Input Types:</h4>
     * <ul>
     *   <li><strong>Query</strong>: User's specific question or requirement</li>
     *   <li><strong>Task Instruction</strong>: Specific task to be executed</li>
     *   <li><strong>Dialogue Response</strong>: Response to agent's previous reply</li>
     *   <li><strong>Context Supplement</strong>: Additional background info or clarification</li>
     * </ul>
     *
     * <p>User prompt is the core input for each round of dialogue, driving agent reasoning and responses.</p>
     */
    public String userPrompt;

    /**
     * Short-term memory
     * <p>Maintains dialogue history and context info of the current session, providing immediate context continuity.</p>
     *
     * <h4>Memory Scope:</h4>
     * <ul>
     *   <li><strong>Current Session</strong>: All interactions since the start of the current dialogue</li>
     *   <li><strong>Recent History</strong>: Q&amp;A records from recent rounds</li>
     *   <li><strong>Work Content</strong>: Execution process and intermediate results of current task</li>
     * </ul>
     *
     * <p>Usually has a length limit; excess is managed by sliding window or importance filtering strategies.</p>
     */
    public Memory shortMemory;

    /**
     * Long-term memory
     * <p>Contains persistent knowledge, user information, and historical experience, providing deep context background.</p>
     *
     * <h4>Memory Types:</h4>
     * <ul>
     *   <li><strong>Knowledge Facts</strong>: Relevant info retrieved from the knowledge base</li>
     *   <li><strong>Historical Experience</strong>: Experience and preferences learned from historical dialogues</li>
     *   <li><strong>User Profile</strong>: User's personal info and preference settings</li>
     * </ul>
     *
     * <p>Dynamically retrieved and assembled based on current query, provides personalized background knowledge.</p>
     */
    public LongMemory longMemory;

    /**
     * Agent execution state
     * <p>Identifies the agent's current execution phase, mode, or state, used to control processing flow and behavior pattern.</p>
     *
     * <h4>Common States:</h4>
     * <ul>
     *   <li><strong>INIT</strong>: Initialization state, preparing to receive user input</li>
     *   <li><strong>REASONING</strong>: Reasoning state, analyzing problems and formulating solutions</li>
     *   <li><strong>TOOL_CALLING</strong>: Tool calling state, executing external tools</li>
     *   <li><strong>RESPONDING</strong>: Responding state, generating final reply</li>
     *   <li><strong>ERROR</strong>: Error state, handling exceptions</li>
     * </ul>
     *
     * <p>Status information guides agent in choosing appropriate processing strategies and response patterns.</p>
     */
    public String agentState;

    /**
     * Tool execution result list
     * <p>Stores execution results and feedback from previous or current tool calls.</p>
     *
     * <h4>Result Content:</h4>
     * <ul>
     *   <li><strong>Tool Output</strong>: Specific result data from tool execution</li>
     *   <li><strong>Execution Status</strong>: Success, failure, or partial success status</li>
     *   <li><strong>Error Info</strong>: Detailed error if tool call fails</li>
     *   <li><strong>Metadata</strong>: Additional info such as execution time, resource consumption, etc.</li>
     * </ul>
     *
     * <p>These results provide feedback from tool calls for agent's subsequent reasoning and decision-making.</p>
     */
    public List<Message> toolResults;
}
