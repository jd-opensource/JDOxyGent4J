package com.jd.oxygent.core.oxygent.samples.server.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * Item request data transfer object
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class ItemRequest {
    /**
     * Class constructor parameters, containing class name and configuration
     */
    @JsonProperty("class_attr")
    private Map<String, Object> classAttr;

    /**
     * Runtime parameters passed to agent execution method
     */
    private Map<String, Object> arguments;
}