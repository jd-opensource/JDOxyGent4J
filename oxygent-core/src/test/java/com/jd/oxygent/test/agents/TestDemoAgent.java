/*
 * Copyright 2025 JD.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this project except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.oxygent.test.agents;

import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.samples.examples.agent.DemoSkillAgent;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.OxySpaceBeanCollector;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.utils.LogUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.python.indexer.ast.NContinue;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * auto test all demo agents
 */
@Slf4j
public class TestDemoAgent {

    Set<String> exclude = Set.of(
            "DemoSseAgent",
            "AppMasterAgent",
            "DemoBrowser",
            "OllamaDemo",
            "DemoBankChatAgentDumpMemory",
            "DemoBankReactAgentAutonomy",
            "DemoBankReactAgentAutonomyByMCP",
            "DemoBankReactAgentRigid"
    );

    @Before
    public void setUp() {
    }

    @Test
    public void testDemoAgent() {
        OxySpaceBeanCollector collector = OxySpaceBeanCollector.getInstance();
        collector.init();

        collector.getOxySpaceMethods().values().forEach(
                method -> {
                    if (!exclude.contains(method.getDeclaringClass().getSimpleName())) {
                        log.info(LogUtils.ANSI_RED + "######################### invoking demo agent: {}" + LogUtils.ANSI_RESET_ALL, method.getDeclaringClass().getName());

                        Config.getMessage().setShowInTerminal(false);
                        Config.getMessage().setDetailedObservation(false);
                        Config.getMessage().setDetailedToolCall(false);
                        Config.getMessage().setStored(false);

                        try {
                            Class<?> clazz = method.getDeclaringClass();
                            Constructor<?> constructor = clazz.getDeclaredConstructor();
                            constructor.setAccessible(true);
                            Object instance = constructor.newInstance();
                            List<BaseOxy> oxySpace = (List<BaseOxy>) method.invoke(instance, null);
                            var mas = new Mas(DemoSkillAgent.class.getSimpleName(), oxySpace);
                            mas.setDefaultOxySpace(oxySpace);
                            mas.init();

                            String query = "hello";
                            OxySpaceBean annotation = method.getAnnotation(OxySpaceBean.class);
                            if (annotation != null) {
                                query = annotation.query();
                            }
                            var payload = new HashMap<String, Object>();
                            payload.put("query", query);

                            OxyResponse response = mas.chatWithAgent(payload);
                            log.info(response.getOutput().toString());
                            log.info(LogUtils.ANSI_RED + "######################### demo agent invoked: {}" + LogUtils.ANSI_RESET_ALL, method.getDeclaringClass().getName());
                        } catch (Exception e) {
                            log.error(LogUtils.ANSI_RED + "######################### invoke demo agent failed: {}" + LogUtils.ANSI_RESET_ALL, method.getDeclaringClass().getName(), e);
                        }
                    } else {
                        log.info(LogUtils.ANSI_RED + "######################### demo agent excluded: {}" + LogUtils.ANSI_RESET_ALL, method.getDeclaringClass().getName());
                    }
                }
        );
    }
}
