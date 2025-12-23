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

import lombok.Builder;
import lombok.Data;

/**
 * <h3>Long-term Memory Data Model</h3>
 *
 * <p>This class encapsulates the long-term memory information of the OxyGent agent,
 * used for storing and managing knowledge, history records, and user profile information accumulated during long-term interactions.
 * This information provides agents with persistent context background, enhancing dialogue continuity and personalization.</p>
 *
 * <h3>Memory Composition Structure:</h3>
 * <ul>
 *   <li><strong>facts</strong>: Factual knowledge, including domain knowledge, rules, common sense, etc.</li>
 *   <li><strong>history</strong>: Historical interaction records, including important conversation fragments and learning content</li>
 *   <li><strong>profile</strong>: User profile information, including user preferences, habits, background, etc.</li>
 * </ul>
 *
 * <h3>Usage Scenarios:</h3>
 * <ul>
 *   <li>Personalized dialogue experience construction</li>
 *   <li>Context continuity across sessions</li>
 *   <li>User behavior pattern learning</li>
 *   <li>Accumulation and application of domain knowledge</li>
 * </ul>
 *
 * <h3>Data Sources:</h3>
 * <ul>
 *   <li><strong>Knowledge Base Retrieval</strong>: Retrieve relevant facts from structured knowledge base</li>
 *   <li><strong>RAG Retrieval</strong>: Retrieve relevant fragments from historical dialogue</li>
 *   <li><strong>User Modeling</strong>: Extract preference information from user behavior</li>
 *   <li><strong>System Learning</strong>: Learn new knowledge points from interactions</li>
 * </ul>
 *
 * <h3>Update Strategies:</h3>
 * <ul>
 *   <li>Dynamic retrieval and update based on relevance</li>
 *   <li>Persistent storage of important information</li>
 *   <li>Automatic cleanup of expired information</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
public class LongMemory {

    /**
     * Factual knowledge
     * <p>Stores factual information related to the current dialogue, including domain knowledge, rules, common sense, etc.</p>
     *
     * <h4>Content Types:</h4>
     * <ul>
     *   <li><strong>Domain Knowledge</strong>: Concepts, definitions, and principles in the professional field</li>
     *   <li><strong>Business Rules</strong>: Rules and constraints for specific business scenarios</li>
     *   <li><strong>Common Sense Info</strong>: General common-sense knowledge</li>
     *   <li><strong>References</strong>: Related documents, links, data</li>
     * </ul>
     *
     * <p>Usually obtained through knowledge base retrieval, provides agents with accurate factual basis.</p>
     */
    String facts;

    /**
     * Historical interaction records
     * <p>Stores historical dialogue fragments related to the current query, maintaining dialogue continuity and context consistency.</p>
     *
     * <h4>Record Content:</h4>
     * <ul>
     *   <li><strong>Related Dialogue</strong>: Semantically similar historical dialogue fragments</li>
     *   <li><strong>Important Decisions</strong>: Important decisions and reasoning processes made previously</li>
     *   <li><strong>Learning Content</strong>: New knowledge or corrections learned from interactions</li>
     *   <li><strong>Problem Solving</strong>: Successfully solved problems and solutions</li>
     * </ul>
     *
     * <p>Obtained from the historical dialogue library via RAG technology, provides experience reference.</p>
     */
    String history;

    /**
     * User profile information
     * <p>Stores user's personalized information, including preferences, habits, background, etc., used to provide personalized services.</p>
     *
     * <h4>Profile Composition:</h4>
     * <ul>
     *   <li><strong>Basic Info</strong>: User's basic background and identity information</li>
     *   <li><strong>Preference Settings</strong>: User's usage preferences and habits</li>
     *   <li><strong>Interest Fields</strong>: Fields and topics the user is interested in</li>
     *   <li><strong>Interaction Style</strong>: User's preferred interaction style and communication manner</li>
     * </ul>
     *
     * <p>Obtained through user behavior analysis and explicit settings, enables personalized experience.</p>
     */
    String profile;
}
