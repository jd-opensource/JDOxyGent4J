package com.jd.oxygent.core.oxygent.samples.server.masprovider.factory.impl.platform.spring;

import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.infra.databases.BaseCache;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.schemas.contextengineer.ContextEngine;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.factory.PlatformMasFactory;
import com.jd.oxygent.core.oxygent.infra.databases.BaseEs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;
/**
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class SpringMasFactory extends PlatformMasFactory {

    private final static Integer SPRING_PRIORITY = 2;

    @Autowired
    private BaseEs esClient;
    @Autowired
    private BaseCache redisClient;
    @Autowired
    private ContextEngine contextEngine;

    @Override
    public Mas createMultipleMas(String masName, List<BaseOxy> oxySpace) {
        Mas mas = new Mas(masName, oxySpace);
            mas.setEsClient(esClient);
            mas.setRedisClient(redisClient);
            mas.setContextEngine(contextEngine);
            this.doAutowireMas(mas);
            mas.setOxySpace(oxySpace);
            mas.init();
        masMap.put(masName, mas);
        return mas;
    }

    protected void doAutowireMas(Mas mas){
        // Subclass extension
    }

    @Override
    public boolean supportsPlatform() {
        try {
            Class.forName("org.springframework.context.ApplicationContext");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public int getPriority() {
        return SPRING_PRIORITY;
    }

    @Override
    public String getPlatformName() {
        return "spring";
    }
}