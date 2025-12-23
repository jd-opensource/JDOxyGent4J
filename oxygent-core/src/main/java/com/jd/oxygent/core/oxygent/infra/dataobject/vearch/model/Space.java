package com.jd.oxygent.core.oxygent.infra.dataobject.vearch.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.jd.oxygent.core.oxygent.infra.dataobject.vearch.model.Field;

import java.util.List;

public class Space {
    @JsonProperty("name")
    private String name;

    @JsonProperty("partition_num")
    private int partitionNum;

    @JsonProperty("replica_num")
    private int replicaNum;

    @JsonProperty("fields")
    private List<Field> fields;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setPartitionNum(int partitionNum) {
        this.partitionNum = partitionNum;
    }

    public int getPartitionNum() {
        return this.partitionNum;
    }

    public void setReplicaNum(int replicaNum) {
        this.partitionNum = replicaNum;
    }

    public int getReplicaNum() {
        return this.replicaNum;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    public List<Field> getFields() {
        return this.fields;
    }
}
