package com.jd.oxygent.core.oxygent.samples.server.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * Organization wrapper containing agent node structure and ID dictionary
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class OrganizationWrapper {
    private AgentNodeWithPath organization;
    @JsonProperty("id_dict")
    private Map<String, Integer> idDict;

    public OrganizationWrapper(AgentNodeWithPath organization, Map<String, Integer> idDict) {
        this.organization = organization;
        this.idDict = idDict;
    }

}