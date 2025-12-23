package com.jd.oxygent.core.oxygent.samples.server.masprovider.oxyspace;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;

import java.util.List;
import java.util.Map;

/**
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public interface OxySpaceProvider {
    public Map<String, List<BaseOxy>> getCustomOxySpace();

    public String getOxySpaceName();
}