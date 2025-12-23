package com.jd.oxygent.core.oxygent.infra.dataobject.vearch.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jd.oxygent.core.oxygent.infra.dataobject.vearch.model.Cache;
import com.jd.oxygent.core.oxygent.infra.dataobject.vearch.model.Space;

import java.util.List;

public interface SpaceClient {
    String createSpace(Space var1) throws JsonProcessingException;

    String viewSpace(String var1);

    String deleteSpace(String var1);

    String modifyCacheSize(String var1, List<Cache> var2) throws JsonProcessingException;

    String viewCacheSize(String var1);
}
