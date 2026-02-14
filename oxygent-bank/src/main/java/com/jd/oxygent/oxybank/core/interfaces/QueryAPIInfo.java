package com.jd.oxygent.oxybank.core.interfaces;

import lombok.Data;

import java.util.Map;

/**
 * Query API information model (from EndpointShow).
 */
@Data
public class QueryAPIInfo {

    private String name;
    private String path;
    private Map<String, Object> params;

    public QueryAPIInfo(String name, String path, Map<String, Object> params) {
        this.name = name;
        this.path = path;
        this.params = params;
    }
}
