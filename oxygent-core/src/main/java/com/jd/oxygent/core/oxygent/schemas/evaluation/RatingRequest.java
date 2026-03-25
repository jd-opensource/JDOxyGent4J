package com.jd.oxygent.core.oxygent.schemas.evaluation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RatingRequest {
    @JsonProperty("trace_id")
    private String traceId;
    @JsonProperty("rating_type")
    private RatingType ratingType;
    @JsonProperty("tag_list")
    private List<String> tagList;
    private String comment;
    private String erp;
    private String module;
}