package com.jd.oxygent.core.oxygent.infra.dataobject.vearch.client;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.oxygent.core.oxygent.infra.dataobject.vearch.model.Cache;
import com.jd.oxygent.core.oxygent.infra.dataobject.vearch.model.Space;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;

import java.util.List;

public class SpaceOperation extends BaseOperation implements SpaceClient {
    private final String databaseName;

    public SpaceOperation(String databaseName) {
        this.databaseName = databaseName;
    }

    public SpaceOperation(String baseUrl, String databaseName) {
        super(baseUrl);
        this.databaseName = databaseName;
    }

    public SpaceOperation(String baseUrl, String userName, String token, String databaseName) {
        super(baseUrl, userName, token);
        this.databaseName = databaseName;
    }

    public String createSpace(Space space) throws JsonProcessingException {
        String endpoint = "/dbs/" + this.databaseName + "/spaces";
        return this.sendPostRequest(endpoint, JsonUtils.writeValueAsString(space));
    }

    public String viewSpace(String spaceName) {
        return this.sendGetRequest("/dbs/" + this.databaseName + "/spaces/" + spaceName);
    }

    public String deleteSpace(String spaceName) {
        return this.sendDeleteRequest("/dbs/" + this.databaseName + "/spaces/" + spaceName);
    }

    public String modifyCacheSize(String spaceName, List<Cache> caches) throws JsonProcessingException {
        return this.sendPostRequest("/space/" + this.databaseName + "/" + spaceName, JsonUtils.writeValueAsString(caches));
    }

    public String viewCacheSize(String spaceName) {
        return this.sendGetRequest("/config/" + this.databaseName + "/" + spaceName);
    }
}

