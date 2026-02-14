package com.jd.oxygent.oxybank.core.model.annotation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Batch deposit response model.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepositBatchResponse {

    private String batchId;
    private int total;
    private int successCount;
    private int duplicateCount;
    private int failedCount;
    private List<DepositResponse> results;
}
