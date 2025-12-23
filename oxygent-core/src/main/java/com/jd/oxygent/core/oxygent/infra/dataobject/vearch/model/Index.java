package com.jd.oxygent.core.oxygent.infra.dataobject.vearch.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jd.oxygent.core.oxygent.infra.dataobject.vearch.model.RetrievalParam;

public class Index {
    @JsonProperty("name")
    private String name;

    @JsonProperty("type")

    private String type;
    @JsonProperty("params")
    private RetrievalParam retrievalParam;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }

    public void setRetrievalParam(RetrievalParam retrievalParam) {
        this.retrievalParam = retrievalParam;
    }

    public RetrievalParam getRetrievalParam() {
        return this.retrievalParam;
    }
}
