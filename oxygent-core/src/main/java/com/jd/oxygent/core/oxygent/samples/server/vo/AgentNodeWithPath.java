package com.jd.oxygent.core.oxygent.samples.server.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent node with path information
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class AgentNodeWithPath {
    private String name;
    private String type;
    private List<String> path;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<AgentNodeWithPath> children;

    // Constructor
    public AgentNodeWithPath() {
        this.children = new ArrayList<>();
    }

    // Getter and setter methods
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getPath() {
        return path;
    }

    public void setPath(List<String> path) {
        this.path = path;
    }

    public List<AgentNodeWithPath> getChildren() {
        return children;
    }

    public void setChildren(List<AgentNodeWithPath> children) {
        this.children = children;
    }
}