package com.jd.oxygent.oxybank.core.model.annotation;

import lombok.Data;

import java.util.List;

/**
 * Pending P0 response.
 */
@Data
public class PendingP0Response {

    private int total;
    private List<PendingP0Item> items;
}
