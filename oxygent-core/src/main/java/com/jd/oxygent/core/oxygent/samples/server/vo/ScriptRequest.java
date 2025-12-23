package com.jd.oxygent.core.oxygent.samples.server.vo;

import lombok.Data;

import java.util.List;

/**
 * Script request data transfer object
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class ScriptRequest {
    /**
     * Human-friendly script label displayed in UI
     */
    private String name;

    /**
     * Arbitrary list structure to be sent to /call subsequently
     */
    private List<Object> contents;
}
