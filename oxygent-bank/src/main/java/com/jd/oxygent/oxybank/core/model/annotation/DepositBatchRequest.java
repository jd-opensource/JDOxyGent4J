package com.jd.oxygent.oxybank.core.model.annotation;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Batch deposit request model.
 */
@Data
@NoArgsConstructor
public class DepositBatchRequest {

    private List<DepositRequest> dataList;
    private String batchId;
}
