package com.jd.oxygent.oxybank.core.interfaces;

import lombok.Data;

import java.util.Map;

/**
 * Query API info for core endpoints (from EndpointShowController).
 */
@Data
public class CoreEndpointAPIInfo {

    private String name;
    private String path;
    private Map<String, String> params;
}
