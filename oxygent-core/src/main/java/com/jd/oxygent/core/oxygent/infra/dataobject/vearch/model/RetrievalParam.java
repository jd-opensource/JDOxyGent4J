package com.jd.oxygent.core.oxygent.infra.dataobject.vearch.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.jd.oxygent.core.oxygent.infra.dataobject.vearch.model.Hnsw;

public class RetrievalParam {
    @JsonProperty("metric_type")
    private String metricType;
    private int ncentroids;
    private int nsubvector;
    private int training_threshold = 39;
    @JsonProperty("hnsw")
    private Hnsw hnsw = new Hnsw();

    public String getMetricType() {
        return this.metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public int getNcentroids() {
        return this.ncentroids;
    }

    public void setNcentroids(int ncentroids) {
        this.ncentroids = ncentroids;
    }

    public int getNsubvector() {
        return this.nsubvector;
    }

    public void setNsubvector(int nsubvector) {
        this.nsubvector = nsubvector;
    }

    public void setTraining_threshold(int training_threshold) {
        this.training_threshold = training_threshold;
    }

    public int getTraining_threshold() {
        return this.training_threshold;
    }

    public void setHnsw(Hnsw hnsw) {
        this.hnsw = hnsw;
    }

    public Hnsw getHnsw() {
        return this.hnsw;
    }
}