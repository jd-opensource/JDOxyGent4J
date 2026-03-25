package com.jd.oxygent.core.oxygent.schemas.evaluation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConversationWithRating {
    private String traceId;
    private String input;
    private String callee;
    private String output;
    private String createTime;
    private String fromTraceId;
    private RatingStats ratingStats;
    private List<ConversationRating> ratingHistory;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("trace_id", traceId);
        map.put("input", input);
        map.put("callee", callee);
        map.put("output", output);
        map.put("create_time", createTime);
        map.put("from_trace_id", fromTraceId);

        if (ratingStats != null) {
            map.put("rating_stats", ratingStats.toMap());
        }

        if (ratingHistory != null && !ratingHistory.isEmpty()) {
            List<Map<String, Object>> ratingHistoryMaps = new ArrayList<>();
            for (ConversationRating rating : ratingHistory) {
                ratingHistoryMaps.add(rating.toMap());
            }
            map.put("rating_history", ratingHistoryMaps);
        }

        return map;
    }

    @SuppressWarnings("unchecked")
    public static ConversationWithRating fromMap(Map<String, Object> map) {
        ConversationWithRating conversation = new ConversationWithRating();

        conversation.setTraceId((String) map.getOrDefault("trace_id", null));
        conversation.setInput((String) map.getOrDefault("input", null));
        conversation.setCallee((String) map.getOrDefault("callee", null));
        conversation.setOutput((String) map.getOrDefault("output", null));
        conversation.setCreateTime((String) map.getOrDefault("create_time", null));
        conversation.setFromTraceId((String) map.getOrDefault("from_trace_id", null));

        Map<String, Object> ratingStatsMap = (Map<String, Object>) map.getOrDefault("rating_stats", null);
        if (ratingStatsMap != null) {
            conversation.setRatingStats(RatingStats.fromMap(ratingStatsMap));
        }

        List<Map<String, Object>> ratingHistoryMaps = (List<Map<String, Object>>) map.getOrDefault("rating_history", null);
        if (ratingHistoryMaps != null && !ratingHistoryMaps.isEmpty()) {
            List<ConversationRating> ratingHistory = new ArrayList<>();
            for (Map<String, Object> ratingMap : ratingHistoryMaps) {
                ratingHistory.add(ConversationRating.fromMap(ratingMap));
            }
            conversation.setRatingHistory(ratingHistory);
        }

        return conversation;
    }
}