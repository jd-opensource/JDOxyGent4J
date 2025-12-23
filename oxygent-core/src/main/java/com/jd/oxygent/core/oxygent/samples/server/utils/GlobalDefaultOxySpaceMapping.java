package com.jd.oxygent.core.oxygent.samples.server.utils;

import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.OxySpaceBeanCollector;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;

import java.lang.reflect.Method;

/**
 * @author OxyGent Team
 * @version 1.0.0
 * @description For test environment only
 * @since 1.0.0
 */
public final class GlobalDefaultOxySpaceMapping {

    public static void searchCurrentThreadStackAnnotationOxySpaceName(String className) throws Exception {
        Class<?> hasOxySpaceBeanClass = Class.forName(className);
        if (hasOxySpaceBeanClass != null) {
            for (Method method : hasOxySpaceBeanClass.getMethods()) {
                OxySpaceBean annotation = method.getAnnotation(OxySpaceBean.class);
                if (annotation != null) {
                    if (annotation.defaultStart()) {
                        OxySpaceBeanCollector.DEFAULT_STARTUP_OXYSPACE_BEAN_NAME = "".equals(annotation.name()) ? annotation.value() : annotation.name();
                        break;
                    }
                }
            }
        }
    }

}