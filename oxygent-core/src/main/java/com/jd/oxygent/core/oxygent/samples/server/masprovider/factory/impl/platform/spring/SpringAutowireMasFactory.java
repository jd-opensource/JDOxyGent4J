package com.jd.oxygent.core.oxygent.samples.server.masprovider.factory.impl.platform.spring;

import com.jd.oxygent.core.Mas;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;
/**
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class SpringAutowireMasFactory extends SpringMasFactory{

    private final static Integer SPRING_AUTOWRITE_PRIORITY = 3;

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Override
    protected void doAutowireMas(Mas mas) {
        if(null!=beanFactory){
            beanFactory.autowireBean(mas);
        }
    }

    @Override
    public int getPriority() {
        return SPRING_AUTOWRITE_PRIORITY;
    }

    @Override
    public String getPlatformName() {
        return "springAutowire";
    }

}