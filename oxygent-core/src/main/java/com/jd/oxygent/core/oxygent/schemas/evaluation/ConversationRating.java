package com.jd.oxygent.core.oxygent.schemas.evaluation;

import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConversationRating {
    /**
     * Unique rating record ID
     */
    private String ratingId;
    /**
     * Conversation trace ID, links to specific conversation
     */
    private String traceId;
    /**
     * Rating type: like or dislike
     */
    private RatingType ratingType;
    /**
     * User ID (if user system exists)
     */
    private String userId;
    /**
     * module what rating is for
     */
    private String module;
    /**
     * User IP address
     */
    private String userIp;
    /**
     * comment tags
     */
    private List<String> tagList;
    /**
     * Rating comment or feedback
     */
    private String comment;
    /**
     * ERP system identifier
     */
    private String erp;
    private String createTime;
    private String updateTime;

    public Map<String, Object> toMap() {
        return JsonUtils.convertToMap(this, "snake");
    }

    public static ConversationRating fromMap(Map<String, Object> map) {
        return JsonUtils.parseObject(JsonUtils.mapToJsonString(map), ConversationRating.class);
    }
}