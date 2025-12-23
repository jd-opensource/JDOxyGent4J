package com.jd.oxygent.core.oxygent.samples.server.masprovider.factory.impl;

import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.infra.databases.BaseCache;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.schemas.contextengineer.ContextEngine;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.factory.PlatformMasFactory;
import com.jd.oxygent.core.oxygent.infra.databases.BaseEs;
import com.jd.oxygent.core.oxygent.infra.impl.databases.es.LocalEs;
import com.jd.oxygent.core.oxygent.infra.impl.databases.redis.LocalCache;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManualMasFactory extends PlatformMasFactory {

    private final static Integer MANUAL_PRIORITY = 1;

    private BaseEs esClient = new LocalEs();

    private BaseCache redisClient = new LocalCache();

    private ContextEngine contextEngine = new ContextEngine();

    @Override
    public Mas createMultipleMas(String masName, List<BaseOxy> oxySpace) {
        if (null == esClient || null == redisClient || null == contextEngine) {
            throw new RuntimeException("esClient or redisClient or contextEngine is null");
        }
        Mas mas = new Mas(masName, oxySpace);
        mas.setOxySpace(oxySpace);
        mas.setEsClient(esClient);
        mas.setRedisClient(redisClient);
        mas.setContextEngine(contextEngine);
        mas.init();
        masMap.put(masName, mas);
        return mas;
    }

    @Override
    public boolean supportsPlatform() {
        return true;
    }

    @Override
    public int getPriority() {
        return MANUAL_PRIORITY;
    }

    @Override
    public String getPlatformName() {
        return "manual";
    }
}
