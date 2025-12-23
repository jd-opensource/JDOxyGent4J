package com.jd.oxygent.core.oxygent.infra.dataobject.vearch.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jd.oxygent.core.oxygent.infra.dataobject.vearch.model.Index;

public class Field {
    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private String type;

    @JsonProperty("dimension")
    private int dimension;

    @JsonProperty("index")
    private Index index;

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

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    public int getDimension() {
        return this.dimension;
    }

    public void setIndex(Index index) {
        this.index = index;
    }

    public Index getIndex() {
        return this.index;
    }
}