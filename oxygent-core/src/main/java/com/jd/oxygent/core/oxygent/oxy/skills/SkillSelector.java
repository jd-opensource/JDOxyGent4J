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
package com.jd.oxygent.core.oxygent.oxy.skills;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.oxygent.core.oxygent.schemas.memory.Memory;
import com.jd.oxygent.core.oxygent.schemas.memory.Message;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyState;
import com.jd.oxygent.core.oxygent.utils.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Skill selector for semantic (pre-execute) activation.
 *
 * <p>This class implements a selector-based activation strategy:
 * <ul>
 *     <li>The main agent does not need to call the Skill tool.</li>
 *     <li>We run an extra lightweight LLM call to choose at most one skill.</li>
 * </ul>
 *
 * <p>The selector only operates on Skill metadata (name + description). Full SKILL.md
 * content is loaded only after a skill is selected.</p>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class SkillSelector {

    private static final Logger logger = LoggerFactory.getLogger(SkillSelector.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern WORD_RE = Pattern.compile("[a-zA-Z0-9_\\-]{2,}|[\u4e00-\u9fff]{2,}");

    private static final Pattern SKILL_CREATION_INTENT_RE = Pattern.compile(
            "(创建|新建|生成|制作|编写|写|搭建|开发).{0,20}(技能|skill|SKILL|SKILL\\.md)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SKILL_CREATION_INTENT_EN_RE = Pattern.compile(
            "\\b(create|make|build|generate|init|initialize|scaffold|draft|write)\\b.*\\bskills?\\b",
            Pattern.CASE_INSENSITIVE
    );
    //Common shorthand: "做个 skill" / "写个 skill" / "new skill"
    private static final Pattern NEW_SKILL_RE = Pattern.compile(
            "\\bnew\\b.*\\bskills?\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final String SKILL_CREATOR_NAME = "skill-creator";

    private static final int DEFAULT_MAX_CANDIDATES = 30;
    private static final double DEFAULT_MIN_CONFIDENCE = 0.6;

    /**
     * Skill selection result.
     */
    public static class SkillSelection {
        // Selected skill name
        private final String selectedSkill;
        // Confidence score
        private final double confidence;
        // Reason
        private final String reason;

        public SkillSelection(String selectedSkill, double confidence, String reason) {
            this.selectedSkill = selectedSkill;
            this.confidence = confidence;
            this.reason = reason;
        }

        public String getSelectedSkill() {
            return selectedSkill;
        }

        public double getConfidence() {
            return confidence;
        }

        public String getReason() {
            return reason;
        }

        public static SkillSelection noSkills() {
            return new SkillSelection(null, 0.0, "no_skills");
        }

        public static SkillSelection parseFailed() {
            return new SkillSelection(null, 0.0, "selector_parse_failed");
        }

        public static SkillSelection selectorLlmFailed(OxyState state) {
            return new SkillSelection(null, 0.0, "selector_llm_failed:" + state);
        }

        public static SkillSelection outOfSet() {
            return new SkillSelection(null, 0.0, "selector_selected_out_of_set");
        }

        public static SkillSelection belowThreshold(String reason) {
            return new SkillSelection(null, 0.0, "below_threshold:" + reason);
        }
    }

    /**
     * Check if query looks like a skill creation request.
     *
     * @param query User query string
     * @return true if query appears to be about creating a skill
     */
    private static boolean looksLikeSkillCreationRequest(String query) {
        String q = (query == null) ? "" : query.trim();
        if (q.isEmpty()) {
            return false;
        }

        if (q.startsWith("/skill-creator")) {
            return true;
        }

        if (SKILL_CREATION_INTENT_RE.matcher(q).find()) {
            return true;
        }

        if (SKILL_CREATION_INTENT_EN_RE.matcher(q).find()) {
            return true;
        }

        if (NEW_SKILL_RE.matcher(q).find()) {
            return true;
        }

        return false;
    }

    /**
     * Tokenize text into a set of words.
     *
     * @param text Input text
     * @return Set of lowercase tokens
     */
    private static Set<String> tokenize(String text) {
        Set<String> tokens = new HashSet<>();
        if (text == null || text.isEmpty()) {
            return tokens;
        }

        var matcher = WORD_RE.matcher(text);
        while (matcher.find()) {
            tokens.add(matcher.group().toLowerCase());
        }
        return tokens;
    }

    /**
     * Rank skills by keyword overlap with query.
     *
     * <p>Cheap heuristic ranking to reduce selector context size.</p>
     *
     * @param query User query
     * @param skills List of skill metadata
     * @return Ranked list of skills
     */
    private static List<SkillMetadata> rankSkillsByKeywordOverlap(String query, List<SkillMetadata> skills) {
        if (skills == null || skills.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> queryTokens = tokenize(query);

        List<RankedSkill> ranked = new ArrayList<>();
        for (SkillMetadata skill : skills) {
            Set<String> nameTokens = tokenize(skill.getName());
            Set<String> descTokens = tokenize(skill.getDescription());

            int score = 0;
            for (String token : queryTokens) {
                if (nameTokens.contains(token) || descTokens.contains(token)) {
                    score++;
                }
            }

            int nameScore = 0;
            for (String token : queryTokens) {
                if (nameTokens.contains(token)) {
                    nameScore++;
                }
            }

            ranked.add(new RankedSkill(score, nameScore, skill));
        }

        ranked.sort((a, b) -> {
            if (a.score != b.score) {
                return Integer.compare(b.score, a.score);
            }
            if (a.nameScore != b.nameScore) {
                return Integer.compare(b.nameScore, a.nameScore);
            }
            return a.skill.getName().compareTo(b.skill.getName());
        });

        return ranked.stream()
                .map(r -> r.skill)
                .collect(Collectors.toList());
    }

    private static class RankedSkill {
        final int score;
        final int nameScore;
        final SkillMetadata skill;

        RankedSkill(int score, int nameScore, SkillMetadata skill) {
            this.score = score;
            this.nameScore = nameScore;
            this.skill = skill;
        }
    }

    /**
     * Build selector prompt for LLM.
     *
     * @param query User query
     * @param skills List of candidate skills
     * @return Memory object with system and user messages
     */
    private static Memory buildSelectorPrompt(String query, List<SkillMetadata> skills) {
        Memory mem = new Memory();

        String systemPrompt = "You are a skill selector.\n" +
                "\n" +
                "Choose at most ONE skill from the list.\n" +
                "\n" +
                "Return JSON only (no markdown, no prose) using this schema:\n" +
                "{\n" +
                "  \"selected_skill\": \"<name>\" | null,\n" +
                "  \"confidence\": 0.0,\n" +
                "  \"reason\": \"...\"\n" +
                "}\n" +
                "\n" +
                "Rules:\n" +
                "- Select a skill only if it meaningfully improves execution.\n" +
                "- If none apply, return selected_skill=null with confidence 0.\n" +
                "- Use confidence in [0,1].\n" +
                "\n" +
                "Notes:\n" +
                "- The user may ask in languages different from the skill descriptions (e.g., Chinese). Use your understanding.\n";

        mem.addMessage(Message.systemMessage(systemPrompt));

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("User request:\n");
        userPrompt.append(query.trim()).append("\n");
        userPrompt.append("\n");
        userPrompt.append("Available skills (name: description):\n");

        for (SkillMetadata skill : skills) {
            userPrompt.append("- ").append(skill.getName()).append(": ").append(skill.getDescription()).append("\n");
        }

        mem.addMessage(Message.userMessage(userPrompt.toString()));
        return mem;
    }

    /**
     * Parse selector LLM output.
     *
     * @param text LLM output text
     * @return SkillSelection object
     */
    private static SkillSelection parseSelectorOutput(String text) {
        try {
            String jsonStr = CommonUtils.extractFirstJson(text);
            JsonNode root = objectMapper.readTree(jsonStr);

            String selectedSkill = null;
            JsonNode selectedNode = root.get("selected_skill");
            if (selectedNode != null && !selectedNode.isNull()) {
                selectedSkill = selectedNode.asText();
                if (selectedSkill != null) {
                    selectedSkill = selectedSkill.trim();
                    if (selectedSkill.isEmpty()) {
                        selectedSkill = null;
                    }
                }
            }

            double confidence = 0.0;
            JsonNode confidenceNode = root.get("confidence");
            if (confidenceNode != null) {
                confidence = confidenceNode.asDouble();
                if (confidence < 0.0) {
                    confidence = 0.0;
                } else if (confidence > 1.0) {
                    confidence = 1.0;
                }
            }

            String reason = "unspecified";
            JsonNode reasonNode = root.get("reason");
            if (reasonNode != null && reasonNode.isTextual()) {
                reason = reasonNode.asText().trim();
                if (reason.isEmpty()) {
                    reason = "unspecified";
                }
            }

            return new SkillSelection(selectedSkill, confidence, reason);

        } catch (Exception e) {
            logger.warn("Failed to parse selector output: {}", e.getMessage());
            return SkillSelection.parseFailed();
        }
    }

    /**
     * Select a skill for the given query.
     *
     * <p>This runs an extra LLM call and expects strict JSON back.</p>
     *
     * @param oxyRequest OxyRequest object for LLM calls
     * @param llmModel LLM model name to use
     * @param skills List of available skills
     * @param query User query
     * @return SkillSelection result
     */
    public static CompletableFuture<SkillSelection> selectSkill(
            OxyRequest oxyRequest,
            String llmModel,
            List<SkillMetadata> skills,
            String query) {
        return selectSkill(oxyRequest, llmModel, skills, query, DEFAULT_MAX_CANDIDATES, DEFAULT_MIN_CONFIDENCE);
    }

    /**
     * Select a skill for the given query with custom parameters.
     *
     * <p>This runs an extra LLM call and expects strict JSON back.</p>
     *
     * @param oxyRequest OxyRequest object for LLM calls
     * @param llmModel LLM model name to use
     * @param skills List of available skills
     * @param query User query
     * @param maxCandidates Maximum number of candidates to consider
     * @param minConfidence Minimum confidence threshold
     * @return CompletableFuture with SkillSelection result
     */
    public static CompletableFuture<SkillSelection> selectSkill(
            OxyRequest oxyRequest,
            String llmModel,
            List<SkillMetadata> skills,
            String query,
            int maxCandidates,
            double minConfidence) {

        return CompletableFuture.supplyAsync(() -> {
            if (skills == null || skills.isEmpty()) {
                return SkillSelection.noSkills();
            }

            String q = (query == null) ? "" : query.trim();
            //用户的问题为创建/生成/初始化等关键含义的词汇 ,而skill工具列表中存在skill-creator技能则直接返回。
            if (looksLikeSkillCreationRequest(q)) {
                Set<String> skillNames = skills.stream()
                        .map(SkillMetadata::getName)
                        .collect(Collectors.toSet());
                if (skillNames.contains(SKILL_CREATOR_NAME)) {
                    return new SkillSelection(SKILL_CREATOR_NAME, 0.99, "heuristic:skill_creation");
                }
            }

            List<SkillMetadata> ranked = rankSkillsByKeywordOverlap(q, skills);
            int candidatesCount = Math.max(1, Math.min(maxCandidates, ranked.size()));
            List<SkillMetadata> candidates = ranked.subList(0, candidatesCount);
            //提示词+skill工具列表 + 用户需求说明提示词
            Memory mem = buildSelectorPrompt(q, candidates);
            //大模型请求参数
            Map<String, Object> llmArgs = new HashMap<>();
            llmArgs.put("messages", mem);
            llmArgs.put("temperature", 0);
            //大模型请求,skill工具选择结果返回
            OxyResponse resp = oxyRequest.call(Map.of(
                    "callee", llmModel,
                    "arguments", llmArgs,
                    "is_send_message", false,
                    "is_save_history", false
            ));
            //模型选择失败
            if (resp.getState() != OxyState.COMPLETED) {
                return SkillSelection.selectorLlmFailed(resp.getState());
            }
            //解析大模型输出skill选择内容封装为SkillSelection
            SkillSelection selection = parseSelectorOutput(resp.getOutputAsString());
            if (selection.getSelectedSkill() == null) {
                return selection;
            }
            //所有被大模型候选的skill工具名
            Set<String> candidateNames = candidates.stream()
                    .map(SkillMetadata::getName)
                    .collect(Collectors.toSet());
            //大模型选择工具名必须为候选skill工具名
            if (!candidateNames.contains(selection.getSelectedSkill())) {
                return SkillSelection.outOfSet();
            }
            //选择的skill工具信任度分数小于配置的阈值
            if (selection.getConfidence() < minConfidence) {
                return SkillSelection.belowThreshold(selection.getReason());
            }
            //返回最终skill元数据
            return selection;
        });
    }
}
