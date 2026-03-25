package com.jd.oxygent.core.oxygent.schemas.evaluation;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RatingResponse {
    private boolean success;
    private String ratingId;
    private RatingStats currentStats;
    private String message = "";
}