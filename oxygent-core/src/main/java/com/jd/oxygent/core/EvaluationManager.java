package com.jd.oxygent.core;

import com.jd.oxygent.core.oxygent.infra.databases.BaseEs;
import com.jd.oxygent.core.oxygent.infra.impl.databases.es.LocalEs;
import com.jd.oxygent.core.oxygent.schemas.evaluation.ConversationRating;
import com.jd.oxygent.core.oxygent.schemas.evaluation.ConversationWithRating;
import com.jd.oxygent.core.oxygent.schemas.evaluation.RatingRequest;
import com.jd.oxygent.core.oxygent.schemas.evaluation.RatingResponse;
import com.jd.oxygent.core.oxygent.schemas.evaluation.RatingStats;
import com.jd.oxygent.core.oxygent.schemas.evaluation.RatingType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Conversation evaluation manager.
 *
 *     Handles evaluation-related data operations including:
 *     - Storage and retrieval of rating data
 *     - Calculation and update of rating statistics
 *     - Aggregated analysis of rating data
 *
 * @author OxyGent Team
 * @version 1.0.10.4
 * @since 1.0.10.4
 */
@Slf4j
@Component
@DependsOn("config")
public class EvaluationManager implements InitializingBean {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS");

    @Autowired
    private BaseEs esClient;

    private String appName;
    private String ratingIndex;
    private String ratingStatsIndex;

    public EvaluationManager() {
        afterPropertiesSet();
    }

    @Override
    public void afterPropertiesSet() {
        if (esClient == null) {
            esClient = new LocalEs();
        }
        appName = Config.getAppName();
        ratingIndex = appName + "_rating";
        ratingStatsIndex = appName + "_rating_stats";
    }

    private RatingStats createEmptyStats(String traceId) {
        return new RatingStats(
                traceId,
                0,
                0,
                0,
                0.0,
                LocalDateTime.now().format(DATETIME_FORMATTER)
        );
    }

    private int getHitsTotal(Map<String, Object> response) {
        if (response == null) {
            return 0;
        }
        Map<String, Object> hits = (Map<String, Object>) response.getOrDefault("hits", Map.of());
        Object total = hits.get("total");
        if (total != null) {
            if (total instanceof Map) {
                return (Integer) ((Map) total).getOrDefault("value", 0);
            } else {
                return (Integer) total;
            }
        }
        List<?> hitsList = (List<?>) hits.getOrDefault("hits", List.of());
        return hitsList.size();
    }

    private void refreshIndex(String indexName) {
        try {
            esClient.refreshIndex(indexName);
        } catch (Exception e) {
            log.warn("Failed to refresh index " + indexName + ": " + e.getMessage());
        }
    }

    public RatingResponse createRating(RatingRequest ratingRequest) {
        try {
            boolean traceExists = checkTraceExists(ratingRequest.getTraceId());
            if (!traceExists) {
                log.warn("Trace does not exist: " + ratingRequest.getTraceId() + ", but allowing rating to continue");
            }
            String currentTime = LocalDateTime.now().format(DATETIME_FORMATTER);
            String ratingId = UUID.randomUUID().toString();
            ConversationRating rating = ConversationRating.builder()
                    .ratingId(ratingId)
                    .traceId(ratingRequest.getTraceId())
                    .ratingType(ratingRequest.getRatingType())
                    .userId(ratingRequest.getUserId())
                    .module(ratingRequest.getModule())
                    .userIp(ratingRequest.getUserIp())
                    .tagList(ratingRequest.getTagList())
                    .comment(ratingRequest.getComment())
                    .score(ratingRequest.getScore())
                    .erp(ratingRequest.getErp())
                    .createTime(currentTime)
                    .build();
            esClient.index(ratingIndex, ratingId, rating.toMap());
            refreshIndex(ratingIndex);
            RatingStats stats = updateRatingStats(ratingRequest.getTraceId(), ratingRequest.getRatingType());
            return new RatingResponse(true, ratingId, stats, "Rating successful");
        } catch (Exception e) {
            log.error("Failed to create/update rating: " + e.getMessage(), e);
            return new RatingResponse(false, null, null, "Rating failed: " + e.getMessage());
        }
    }

    private boolean checkTraceExists(String traceId) {
        try {
            String traceIndex = appName + "_trace";

            Map<String, Object> query = Map.of(
                "query", Map.of(
                    "term", Map.of("trace_id", traceId)
                ),
                "size", 1
            );

            Map<String, Object> response = esClient.search(traceIndex, query);

            boolean exists = getHitsTotal(response) > 0;

            if (!exists) {
                log.warn("Trace record not found: " + traceId + ", but allowing rating to continue (possible data delay)");
            }

            return exists;
        } catch (Exception e) {
            log.warn("Failed to check trace existence: " + e.getMessage(), e);
            return true;
        }
    }

    public RatingStats updateRatingStats(String traceId, RatingType knownRatingType) {
        try {
            int likeCount = 0;
            int dislikeCount = 0;
            int totalRatings = 0;

            Map<String, Object> query = Map.of(
                "query", Map.of(
                    "term", Map.of("trace_id", traceId)
                ),
                "size", 1000
            );

            Map<String, Object> response = esClient.search(ratingIndex, query);

            if (response == null) {
                log.warn("No rating data found for trace_id " + traceId + " (index may not exist)");
                return createEmptyStats(traceId);
            }

            totalRatings = getHitsTotal(response);

            Map<String, Object> hits = (Map<String, Object>) response.getOrDefault("hits", Map.of());
            List<Map<String, Object>> hitsList = (List<Map<String, Object>>) hits.getOrDefault("hits", List.of());

            for (Map<String, Object> hit : hitsList) {
                Map<String, Object> source = (Map<String, Object>) hit.getOrDefault("_source", Map.of());
                String ratingType = (String) source.getOrDefault("rating_type", "");

                if (RatingType.LIKE.toString().equals(ratingType)) {
                    likeCount++;
                } else if (RatingType.DISLIKE.toString().equals(ratingType)) {
                    dislikeCount++;
                }
            }

            double satisfactionRate = totalRatings > 0 ? (likeCount / (double) totalRatings) * 100.0 : 0.0;

            RatingStats stats = new RatingStats(
                    traceId,
                    likeCount,
                    dislikeCount,
                    totalRatings,
                    satisfactionRate,
                    LocalDateTime.now().format(DATETIME_FORMATTER)
            );
            esClient.index(ratingStatsIndex, traceId, stats.toMap());
            refreshIndex(ratingStatsIndex);
            return stats;
        } catch (Exception e) {
            log.error("Failed to update rating stats for trace_id=" + traceId + ": " + e.getMessage(), e);
            return createEmptyStats(traceId);
        }
    }

    public Optional<RatingStats> getRatingStats(String traceId) {
        try {
            Map<String, Object> query = Map.of(
                "query", Map.of(
                    "term", Map.of("trace_id", traceId)
                ),
                "size", 1
            );

            Map<String, Object> response = esClient.search(ratingStatsIndex, query);

            if (response == null) {
                log.warn("Rating stats index not found for trace_id " + traceId);
                return Optional.empty();
            }

            if (getHitsTotal(response) > 0) {
                Map<String, Object> hits = (Map<String, Object>) response.getOrDefault("hits", Map.of());
                List<Map<String, Object>> hitsList = (List<Map<String, Object>>) hits.getOrDefault("hits", List.of());
                Map<String, Object> source = (Map<String, Object>) hitsList.get(0).getOrDefault("_source", Map.of());
                return Optional.of(RatingStats.fromMap(source));
            }

            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get rating stats for " + traceId + ": " + e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Map<String, RatingStats> getRatingsForTraces(List<String> traceIds) {
        try {
            if (traceIds == null || traceIds.isEmpty()) {
                return Map.of();
            }

            Map<String, Object> query = Map.of(
                "query", Map.of(
                    "terms", Map.of("trace_id", traceIds)
                ),
                "size", 10000
            );

            Map<String, Object> response = esClient.search(ratingStatsIndex, query);

            if (response == null) {
                log.warn("Rating stats index not found when fetching batch stats");
                return Map.of();
            }

            Map<String, RatingStats> result = new HashMap<>();
            Map<String, Object> hits = (Map<String, Object>) response.getOrDefault("hits", Map.of());
            List<Map<String, Object>> hitsList = (List<Map<String, Object>>) hits.getOrDefault("hits", List.of());

            for (Map<String, Object> hit : hitsList) {
                Map<String, Object> source = (Map<String, Object>) hit.getOrDefault("_source", Map.of());
                String traceId = (String) source.getOrDefault("trace_id", "");
                result.put(traceId, RatingStats.fromMap(source));
            }

            return result;
        } catch (Exception e) {
            log.error("Failed to get ratings for traces: " + e.getMessage(), e);
            return Map.of();
        }
    }

    public List<ConversationRating> getRatingHistory(String traceId, Optional<String> erp) {
        try {
            Map<String, Object> query;
            if (erp.isPresent()) {
                query = Map.of(
                    "query", Map.of(
                        "bool", Map.of(
                            "must", List.of(
                                Map.of("term", Map.of("trace_id", traceId)),
                                Map.of("term", Map.of("erp", erp.get()))
                            )
                        )
                    ),
                    "size", 1000
                );
            } else {
                query = Map.of(
                    "query", Map.of(
                        "bool", Map.of(
                            "must", List.of(
                                Map.of("term", Map.of("trace_id", traceId))
                            )
                        )
                    ),
                    "size", 1000
                );
            }

            Map<String, Object> response = esClient.search(ratingIndex, query);

            if (response == null) {
                log.warn("No rating data found for trace_id " + traceId + " (index may not exist)");
                return List.of();
            }

            List<ConversationRating> ratings = new ArrayList<>();
            Map<String, Object> hits = (Map<String, Object>) response.getOrDefault("hits", Map.of());
            List<Map<String, Object>> hitsList = (List<Map<String, Object>>) hits.getOrDefault("hits", List.of());

            for (Map<String, Object> hit : hitsList) {
                Map<String, Object> source = (Map<String, Object>) hit.getOrDefault("_source", Map.of());
                ratings.add(ConversationRating.fromMap(source));
            }

            ratings.sort((r1, r2) -> r2.getCreateTime().compareTo(r1.getCreateTime()));

            return ratings;
        } catch (Exception e) {
            log.error("Failed to get rating history for " + traceId + ": " + e.getMessage(), e);
            return List.of();
        }
    }

    public List<ConversationRating> getRatingHistory(String traceId) {
        return getRatingHistory(traceId, Optional.empty());
    }

    public Map<String, List<ConversationRating>> getRatingHistoriesForTraces(List<String> traceIds) {
        try {
            if (traceIds == null || traceIds.isEmpty()) {
                return Map.of();
            }

            Map<String, Object> searchBody = new HashMap<>();
            searchBody.put("query", Map.of("terms", Map.of("trace_id", traceIds)));
            searchBody.put("size", 10000);
            searchBody.put("sort", List.of(Map.of("create_time", Map.of("order", "desc"))));

            Map<String, Object> response = esClient.search(ratingIndex, searchBody);

            if (response == null) {
                log.warn("No rating data found when fetching batch histories");
                return Map.of();
            }

            Map<String, List<ConversationRating>> result = new HashMap<>();
            Map<String, Object> hits = (Map<String, Object>) response.getOrDefault("hits", Map.of());
            List<Map<String, Object>> hitsList = (List<Map<String, Object>>) hits.getOrDefault("hits", List.of());

            for (Map<String, Object> hit : hitsList) {
                Map<String, Object> source = (Map<String, Object>) hit.getOrDefault("_source", Map.of());
                String traceId = (String) source.getOrDefault("trace_id", "");
                ConversationRating rating = ConversationRating.fromMap(source);

                result.computeIfAbsent(traceId, k -> new ArrayList<>()).add(rating);
            }

            result.values().forEach(list -> list.sort((r1, r2) -> r2.getCreateTime().compareTo(r1.getCreateTime())));

            return result;
        } catch (Exception e) {
            log.error("Failed to get rating histories for traces: " + e.getMessage(), e);
            return Map.of();
        }
    }

    public boolean deleteRating(String ratingId) {
        try {
            Map<String, Object> query = Map.of(
                "query", Map.of(
                    "term", Map.of("rating_id", ratingId)
                ),
                "size", 1
            );

            Map<String, Object> response = esClient.search(ratingIndex, query);

            if (response == null || getHitsTotal(response) == 0) {
                query = Map.of(
                    "query", Map.of(
                        "term", Map.of("trace_id", ratingId)
                    ),
                    "size", 1
                );

                response = esClient.search(ratingIndex, query);
            }

            if (response == null || getHitsTotal(response) == 0) {
                return false;
            }

            Map<String, Object> hits = (Map<String, Object>) response.getOrDefault("hits", Map.of());
            List<Map<String, Object>> hitsList = (List<Map<String, Object>>) hits.getOrDefault("hits", List.of());
            Map<String, Object> source = (Map<String, Object>) hitsList.get(0).getOrDefault("_source", Map.of());
            String traceId = (String) source.getOrDefault("trace_id", "");
            String docId = (String) hitsList.get(0).getOrDefault("_id", "");

            esClient.delete(ratingIndex, docId);

            updateRatingStats(traceId, null);

            log.info("Deleted rating for trace " + traceId);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete rating " + ratingId + ": " + e.getMessage(), e);
            return false;
        }
    }

    public Map<String, Object> historyWithRatings(String ratingFilter, String searchTerm, int page, int pageSize) {

        // Build search query
        Map<String, Object> searchQuery = Map.of("match_all", Map.of());

        if (searchTerm != null && !searchTerm.strip().isEmpty()) {
            Map<String, Object> boolQuery = new HashMap<>();
            List<Map<String, Object>> shouldClauses = new ArrayList<>();

            Map<String, Object> termTraceId = Map.of("term", Map.of("trace_id", searchTerm));
            shouldClauses.add(termTraceId);

            Map<String, Object> matchInput = Map.of("match", Map.of("input", searchTerm));
            shouldClauses.add(matchInput);

            Map<String, Object> matchCallee = Map.of("match", Map.of("callee", searchTerm));
            shouldClauses.add(matchCallee);

            Map<String, Object> matchOutput = Map.of("match", Map.of("output", searchTerm));
            shouldClauses.add(matchOutput);

            boolQuery.put("should", shouldClauses);
            boolQuery.put("minimum_should_match", 1);
            searchQuery = Map.of("bool", boolQuery);
        }

        // Calculate how many traces we need to fetch
        int fetchSize = pageSize * 10;

        log.info(String.format("Fetching history: page=%d, page_size=%d, fetch_size=%d, rating_filter=%s, search_term=%s",
                page, pageSize, fetchSize, ratingFilter, searchTerm));

        Map<String, Object> searchBody = Map.of(
                "query", searchQuery,
                "size", fetchSize,
                "_source", List.of("trace_id", "group_id", "create_time"),
                "sort", List.of(Map.of("create_time", Map.of("order", "desc")))
        );

        Map<String, Object> tracesResponse = esClient.search(Config.getAppName() + "_trace", searchBody);

        // Fetch traces with minimal fields for grouping
        List<Map<String, Object>> traceHits = (List<Map<String, Object>>) ((Map<String, Object>) tracesResponse.get("hits")).get("hits");
        log.debug("Retrieved " + traceHits.size() + " traces from database");
        // Group traces by group_id
        Map<String, Map<String, Object>> groupsMetadataDict = new HashMap<>();

        for (Map<String, Object> hit : traceHits) {
            try {
                Map<String, Object> source = (Map<String, Object>) hit.get("_source");
                String traceId = (String) source.get("trace_id");
                String groupId = (String) source.getOrDefault("group_id", traceId);
                String createTime = (String) source.get("create_time");

                if (traceId == null || traceId.isEmpty()) {
                    continue;
                }

                groupsMetadataDict.computeIfAbsent(groupId, k -> {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("group_id", groupId);
                    metadata.put("trace_ids", new ArrayList<String>());
                    metadata.put("latest_create_time", createTime);
                    metadata.put("total_likes", 0);
                    metadata.put("total_dislikes", 0);
                    metadata.put("has_rating", false);
                    return metadata;
                });

                ((List<String>) groupsMetadataDict.get(groupId).get("trace_ids")).add(traceId);

                // Update latest_create_time if this trace is newer
                String currentLatest = (String) groupsMetadataDict.get(groupId).get("latest_create_time");
                if (createTime.compareTo(currentLatest) > 0) {
                    groupsMetadataDict.get(groupId).put("latest_create_time", createTime);
                }
            } catch (Exception e) {
                log.warn("Error processing trace hit", e);
                continue;
            }
        }

        List<Map<String, Object>> groupsMetadata = new ArrayList<>(groupsMetadataDict.values());
        log.debug("Built metadata for " + groupsMetadata.size() + " groups");

        List<String> allTraceIds = new ArrayList<>();
        for (Map<String, Object> metadata : groupsMetadata) {
            allTraceIds.addAll((List<String>) metadata.get("trace_ids"));
        }

        if (allTraceIds.isEmpty()) {
            log.warn("No trace_ids found, returning empty result");
            Map<String, Object> data = new HashMap<>();
            data.put("conversation_groups", new ArrayList<>());
            data.put("total", 0);
            data.put("page", page);
            data.put("page_size", pageSize);
            data.put("total_pages", 0);
            return data;
        }

        log.debug("Loading ratings for " + allTraceIds.size() + " traces");
        Map<String, RatingStats> ratingsMap = getRatingsForTraces(allTraceIds);

        // Calculate rating stats per group
        for (Map<String, Object> metadata : groupsMetadata) {
            int totalLikes = 0;
            int totalDislikes = 0;
            boolean hasRating = false;

            for (String traceId : (List<String>) metadata.get("trace_ids")) {
                RatingStats ratingStats = ratingsMap.get(traceId);
                if (ratingStats != null) {
                    totalLikes += ratingStats.getLikeCount();
                    totalDislikes += ratingStats.getDislikeCount();
                    if (ratingStats.getTotalRatings() > 0) {
                        hasRating = true;
                    }
                }
            }

            metadata.put("total_likes", totalLikes);
            metadata.put("total_dislikes", totalDislikes);
            metadata.put("has_rating", hasRating);
        }

        // Filter by rating criteria
        List<Map<String, Object>> filteredGroupsMetadata = new ArrayList<>();
        for (Map<String, Object> metadata : groupsMetadata) {
            int totalLikes = (int) metadata.get("total_likes");
            int totalDislikes = (int) metadata.get("total_dislikes");
            boolean hasRating = (boolean) metadata.get("has_rating");

            if (ratingFilter.equals("all") ||
                    (ratingFilter.equals("liked") && totalLikes > 0) ||
                    (ratingFilter.equals("disliked") && totalDislikes > 0) ||
                    (ratingFilter.equals("unrated") && !hasRating)) {
                filteredGroupsMetadata.add(metadata);
            }
        }

        // Sort by latest time
        filteredGroupsMetadata.sort((a, b) -> {
            String timeA = (String) a.get("latest_create_time");
            String timeB = (String) b.get("latest_create_time");
            return timeB.compareTo(timeA);
        });

        // Pagination
        int totalGroups = filteredGroupsMetadata.size();
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalGroups);

        List<Map<String, Object>> pageGroupsMetadata;
        if (fromIndex >= totalGroups) {
            pageGroupsMetadata = new ArrayList<>();
        } else {
            pageGroupsMetadata = filteredGroupsMetadata.subList(fromIndex, toIndex);
        }

        // Fetch full details only for current page
        List<String> pageTraceIds = new ArrayList<>();
        for (Map<String, Object> metadata : pageGroupsMetadata) {
            pageTraceIds.addAll((List<String>) metadata.get("trace_ids"));
        }

        // Fetch full trace details
        Map<String, Object> pageTracesSearchParams = new HashMap<>();
        Map<String, Object> termsQuery = new HashMap<>();
        termsQuery.put("trace_id", pageTraceIds);
        Map<String, Object> innerQuery = new HashMap<>();
        innerQuery.put("terms", termsQuery);
        pageTracesSearchParams.put("query", innerQuery);
        pageTracesSearchParams.put("size", pageTraceIds.size());
        pageTracesSearchParams.put("_source", Arrays.asList(
                "trace_id", "input", "callee", "output", "create_time", "from_trace_id", "group_id"
        ));

        Map<String, Object> pageTracesResponse = esClient.search(Config.getAppName() + "_trace", pageTracesSearchParams);

        // Build trace details map
        Map<String, Map<String, Object>> traceDetailsMap = new HashMap<>();
        Map<String, Object> pageHits = (Map<String, Object>) pageTracesResponse.getOrDefault("hits", Map.of());
        List<Map<String, Object>> pageTraceHits = (List<Map<String, Object>>) pageHits.getOrDefault("hits", List.of());

        for (Map<String, Object> hit : pageTraceHits) {
            @SuppressWarnings("unchecked")
            Map<String, Object> source = (Map<String, Object>) hit.getOrDefault("_source", Map.of());
            String traceId = (String) source.getOrDefault("trace_id", "");
            traceDetailsMap.put(traceId, source);
        }

        Map<String, List<ConversationRating>> ratingHistoriesMap = getRatingHistoriesForTraces(pageTraceIds);

        // Build response
        List<Map<String, Object>> conversationGroups = new ArrayList<>();

        for (Map<String, Object> metadata : pageGroupsMetadata) {
            String groupId = (String) metadata.get("group_id");
            List<Map<String, Object>> conversations = new ArrayList<>();

            for (String traceId : (List<String>) metadata.get("trace_ids")) {
                Map<String, Object> traceDetail = traceDetailsMap.get(traceId);
                if (traceDetail == null) {
                    continue;
                }

                Map<String, Object> conversationData = new HashMap<>();
                conversationData.put("trace_id", traceId);
                conversationData.put("input", traceDetail.getOrDefault("input", ""));
                conversationData.put("callee", traceDetail.getOrDefault("callee", ""));
                conversationData.put("output", traceDetail.getOrDefault("output", ""));
                conversationData.put("create_time", traceDetail.getOrDefault("create_time", ""));
                conversationData.put("from_trace_id", traceDetail.getOrDefault("from_trace_id", ""));
                conversationData.put("group_id", groupId);

                RatingStats ratingStats = ratingsMap.get(traceId);
                List<ConversationRating> ratingHistory = ratingHistoriesMap.getOrDefault(traceId, new ArrayList<>());

                ConversationWithRating conversationWithRating = new ConversationWithRating(
                        traceId,
                        (String) conversationData.get("input"),
                        (String) conversationData.get("callee"),
                        (String) conversationData.get("output"),
                        (String) conversationData.get("create_time"),
                        (String) conversationData.get("from_trace_id"),
                        ratingStats,
                        ratingHistory
                );

                conversations.add(conversationWithRating.toMap());
            }

            // Sort conversations by create_time
            conversations.sort((a, b) -> {
                String timeA = (String) a.get("create_time");
                String timeB = (String) b.get("create_time");
                return timeA.compareTo(timeB);
            });

            Map<String, Object> groupData = new HashMap<>();
            groupData.put("group_id", groupId);
            groupData.put("conversations", conversations);
            groupData.put("latest_create_time", metadata.get("latest_create_time"));
            groupData.put("conversation_count", conversations.size());
            groupData.put("total_likes", metadata.get("total_likes"));
            groupData.put("total_dislikes", metadata.get("total_dislikes"));
            groupData.put("has_rating", metadata.get("has_rating"));

            conversationGroups.add(groupData);
        }

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("conversation_groups", conversationGroups);
        responseData.put("total", totalGroups);
        responseData.put("page", page);
        responseData.put("page_size", pageSize);
        responseData.put("total_pages", (totalGroups + pageSize - 1) / pageSize);
        return responseData;
    }

    public Map<String, Object> getOverallRatingStats(int days) {
        try {
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = endDate.minusDays(days);
            String startDateStr = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00:00.000000"));
            String endDateStr = endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd 23:59:59.999999"));

            Map<String, Object> query = Map.of(
                "query", Map.of(
                    "range", Map.of(
                        "create_time", Map.of(
                            "gte", startDateStr,
                            "lte", endDateStr
                        )
                    )
                ),
                "size", 10000
            );

            Map<String, Object> response = esClient.search(ratingIndex, query);

            if (response == null) {
                log.warn("Rating index not found when generating trend report");
                Map<String, Object> result = new HashMap<>();
                result.put("total_ratings", 0);
                result.put("like_count", 0);
                result.put("dislike_count", 0);
                result.put("like_rate", 0.0);
                result.put("daily_stats", new HashMap<>());
                return result;
            }

            int totalRatings = 0;
            int likeCount = 0;
            int dislikeCount = 0;
            Map<String, Map<String, Integer>> dailyStats = new HashMap<>();

            Map<String, Object> hits = (Map<String, Object>) response.getOrDefault("hits", Map.of());
            List<Map<String, Object>> hitsList = (List<Map<String, Object>>) hits.getOrDefault("hits", List.of());

            for (Map<String, Object> hit : hitsList) {
                Map<String, Object> source = (Map<String, Object>) hit.getOrDefault("_source", Map.of());
                totalRatings++;

                String ratingType = (String) source.getOrDefault("rating_type", "");
                if (RatingType.LIKE.toString().equals(ratingType)) {
                    likeCount++;
                } else {
                    dislikeCount++;
                }

                String createTime = (String) source.getOrDefault("create_time", "");
                String dateStr = createTime.split(" ")[0];
                Map<String, Integer> daily = dailyStats.computeIfAbsent(dateStr, k -> new HashMap<>());
                daily.put(ratingType, daily.getOrDefault(ratingType, 0) + 1);
            }

            double overallSatisfaction = totalRatings > 0 ? (likeCount / (double) totalRatings * 100) : 0.0;

            Map<String, Object> result = new HashMap<>();
            result.put("total_ratings", totalRatings);
            result.put("like_count", likeCount);
            result.put("dislike_count", dislikeCount);
            result.put("satisfaction_rate", Math.round(overallSatisfaction * 100.0) / 100.0);
            result.put("daily_stats", dailyStats);

            Map<String, Object> timeRange = new HashMap<>();
            timeRange.put("start_date", startDateStr);
            timeRange.put("end_date", endDateStr);
            timeRange.put("days", days);
            result.put("time_range", timeRange);

            return result;
        } catch (Exception e) {
            log.error("Failed to get overall rating stats: " + e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("total_ratings", 0);
            result.put("like_count", 0);
            result.put("dislike_count", 0);
            result.put("satisfaction_rate", 0.0);
            result.put("daily_stats", new HashMap<>());
            result.put("error", e.getMessage());
            return result;
        }
    }

    public Map<String, Object> clearAllRatingData() {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("deleted_ratings", 0);
            result.put("deleted_stats", 0);
            List<String> errors = new ArrayList<>();

            Map<String, Object> query = Map.of(
                "query", Map.of(
                    "match_all", Map.of()
                ),
                "size", 1000
            );

            Map<String, Object> ratingResponse = esClient.search(ratingIndex, query);

            int ratingCount = getHitsTotal(ratingResponse);

            if (ratingCount > 0) {
                try {
                    esClient.deleteIndex(ratingIndex);
                } catch (Exception e) {
                    log.error("Failed to delete rating records", e);
                }
            }

            Map<String, Object> statsResponse = esClient.search(ratingStatsIndex, query);

            int statsCount = getHitsTotal(statsResponse);

            if (statsCount > 0) {
                try {
                    esClient.deleteIndex(ratingStatsIndex);
                } catch (Exception e) {
                    log.error("Failed to delete rating statistics", e);
                }
            }

            if (!errors.isEmpty()) {
                result.put("success", false);
            }
            result.put("errors", errors);

            return result;
        } catch (Exception e) {
            log.error("Failed to clear rating data", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("deleted_ratings", 0);
            result.put("deleted_stats", 0);
            List<String> errors = new ArrayList<>();
            errors.add("Clear failed: " + e.getMessage());
            result.put("errors", errors);
            return result;
        }
    }

    public Map<String, Object> ensureRatingIndicesWithCorrectMapping() {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("rating_index_created", false);
            result.put("rating_stats_index_created", false);
            List<String> errors = new ArrayList<>();

            Map<String, Object> ratingMapping = new HashMap<>();
            Map<String, Object> settings = new HashMap<>();
            settings.put("number_of_shards", 1);
            settings.put("number_of_replicas", 0);
            ratingMapping.put("settings", settings);

            Map<String, Object> mappings = new HashMap<>();
            Map<String, Object> properties = new HashMap<>();

            Map<String, Object> ratingIdProp = new HashMap<>();
            ratingIdProp.put("type", "keyword");
            properties.put("rating_id", ratingIdProp);

            Map<String, Object> traceIdProp = new HashMap<>();
            traceIdProp.put("type", "keyword");
            properties.put("trace_id", traceIdProp);

            Map<String, Object> ratingTypeProp = new HashMap<>();
            ratingTypeProp.put("type", "keyword");
            properties.put("rating_type", ratingTypeProp);

            Map<String, Object> userIdProp = new HashMap<>();
            userIdProp.put("type", "keyword");
            properties.put("user_id", userIdProp);

            Map<String, Object> userIpProp = new HashMap<>();
            userIpProp.put("type", "ip");
            properties.put("user_ip", userIpProp);

            Map<String, Object> commentProp = new HashMap<>();
            commentProp.put("type", "text");
            properties.put("comment", commentProp);

            Map<String, Object> erpProp = new HashMap<>();
            erpProp.put("type", "keyword");
            properties.put("erp", erpProp);

            Map<String, Object> createTimeProp = new HashMap<>();
            createTimeProp.put("type", "keyword");
            properties.put("create_time", createTimeProp);

            Map<String, Object> updateTimeProp = new HashMap<>();
            updateTimeProp.put("type", "keyword");
            properties.put("update_time", updateTimeProp);

            mappings.put("properties", properties);
            ratingMapping.put("mappings", mappings);

            Map<String, Object> ratingStatsMapping = new HashMap<>();
            ratingStatsMapping.put("settings", settings);

            Map<String, Object> statsMappings = new HashMap<>();
            Map<String, Object> statsProperties = new HashMap<>();

            Map<String, Object> statsTraceIdProp = new HashMap<>();
            statsTraceIdProp.put("type", "keyword");
            statsProperties.put("trace_id", statsTraceIdProp);

            Map<String, Object> likeCountProp = new HashMap<>();
            likeCountProp.put("type", "integer");
            statsProperties.put("like_count", likeCountProp);

            Map<String, Object> dislikeCountProp = new HashMap<>();
            dislikeCountProp.put("type", "integer");
            statsProperties.put("dislike_count", dislikeCountProp);

            Map<String, Object> totalRatingsProp = new HashMap<>();
            totalRatingsProp.put("type", "integer");
            statsProperties.put("total_ratings", totalRatingsProp);

            Map<String, Object> satisfactionRateProp = new HashMap<>();
            satisfactionRateProp.put("type", "float");
            statsProperties.put("satisfaction_rate", satisfactionRateProp);

            Map<String, Object> lastUpdatedProp = new HashMap<>();
            lastUpdatedProp.put("type", "keyword");
            statsProperties.put("last_updated", lastUpdatedProp);

            statsMappings.put("properties", statsProperties);
            ratingStatsMapping.put("mappings", statsMappings);

            try {
                Map<String, Object> ratingResult = esClient.createIndex(ratingIndex, ratingMapping);

                if (!Boolean.TRUE.equals(ratingResult.getOrDefault("already_exists", false))) {
                    result.put("rating_index_created", true);
                    log.info("Created rating record index: " + ratingIndex);
                } else {
                    log.info("Rating record index already exists: " + ratingIndex);
                }
            } catch (Exception e) {
                String errorMsg = "Failed to create rating record index: " + e.getMessage();
                errors.add(errorMsg);
                log.error(errorMsg);
            }

            try {
                Map<String, Object> statsResult = esClient.createIndex(ratingIndex, ratingMapping);

                if (!Boolean.TRUE.equals(statsResult.getOrDefault("already_exists", false))) {
                    result.put("rating_stats_index_created", true);
                    log.info("Created rating statistics index: " + ratingStatsIndex);
                } else {
                    log.info("Rating statistics index already exists: " + ratingStatsIndex);
                }
            } catch (Exception e) {
                String errorMsg = "Failed to create rating statistics index: " + e.getMessage();
                errors.add(errorMsg);
                log.error(errorMsg);
            }

            if (!errors.isEmpty()) {
                result.put("success", false);
            }
            result.put("errors", errors);

            return result;
        } catch (Exception e) {
            log.error("Failed to ensure index mapping: " + e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("rating_index_created", false);
            result.put("rating_stats_index_created", false);
            List<String> errors = new ArrayList<>();
            errors.add("Operation failed: " + e.getMessage());
            result.put("errors", errors);
            return result;
        }
    }

}