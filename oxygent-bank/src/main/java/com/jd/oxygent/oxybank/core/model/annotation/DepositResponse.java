package com.jd.oxygent.oxybank.core.model.annotation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Deposit response model.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepositResponse {

    private String dataId;
    private String dataHash;
    private String status;
    private boolean isDuplicate;
    private String message;
}
