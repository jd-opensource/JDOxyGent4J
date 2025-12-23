package com.jd.oxygent.core.oxygent.samples.server.masprovider.factory;

import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.GlobalOxySpaceManager;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.oxyspace.OxySpaceProvider;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.OxySpaceProviderRegistry;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.OxySpaceBeanCollector;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.tools.PresetTools;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public abstract class MasFactory {

    protected final Map<String, Mas> masMap = new ConcurrentHashMap<>();

    protected OxySpaceProvider oxySpaceProvider = OxySpaceProviderRegistry.getOxySpaceProvider();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private OxySpaceBeanCollector oxySpaceBeanCollector = OxySpaceBeanCollector.getInstance();

    protected List<BaseOxy> defaultOxySpace = Arrays.asList(
            HttpLlm.builder()
                    .name("default_llm")
                    .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                    .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                    .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                    .llmParams(Map.of("temperature", 0.01)) // Use Map.of to create immutable Map
                    .timeout(30)
                    .build(),

            // 2. Time tools (assuming preset_tools.time_tools is a predefined Tool instance)
            PresetTools.TIME_TOOLS, // Need to define PresetTools class

            // 3. Time agent
            ReActAgent.builder()
                    .name("time_agent")
                    .desc("A tool that can query the time")
                    .tools(Arrays.asList("time_tools")) // Tool name list
                    .build(),
            // 4. File tools
            PresetTools.FILE_TOOLS,

            // 5. File agent
            ReActAgent.builder()
                    .name("file_agent")
                    .desc("A tool that can operate the file system")
                    .tools(Arrays.asList("file_tools"))
                    .build(),

            // 6. Math tools
            PresetTools.MATH_TOOLS,
            // 7. Math agent
            ReActAgent.builder()
                    .name("math_agent")
                    .desc("A tool that can perform mathematical calculations.")
                    .tools(Arrays.asList("math_tools"))
                    .build(),

            // 8. Master Agent
            ReActAgent.builder()
                    .isMaster(true) // Set as master agent
                    .name("master_agent")
                    .llmModel("default_llm")
                    .subAgents(Arrays.asList("time_agent", "file_agent", "math_agent")) // Sub-agent list
                    .build()
    );

    /**
     * Get Mas instance of default space
     *
     * @return Mas instance
     */
    public final Mas getMas() {
        return getMas(OxySpaceBeanCollector.DEFAULT_STARTUP_OXYSPACE_BEAN_NAME);
    }

    /**
     * Get singleton Mas instance
     *
     * @param masName Mas name
     * @return Mas instance
     */
    public final Mas getMas(String masName) {
        return masMap.get(masName);
    }

    /**
     * Create singleton Mas instance
     *
     * @return Mas instance
     */
    public final Mas createMas() {
        return createMas(OxySpaceBeanCollector.DEFAULT_STARTUP_OXYSPACE_BEAN_NAME);
    }

    /**
     * Create singleton Mas instance
     *
     * @param masName Mas name
     * @return Mas instance
     */
    public final Mas createMas(String masName) {
        return createMas(masName, fetchOxySpace(masName));
    }

    /**
     * Create singletonMas instance
     *
     * @param masName  Mas name
     * @param oxySpace OxySpacelist
     * @return Mas instance
     */
    public final Mas createMas(String masName, List<BaseOxy> oxySpace) {
        masMap.putIfAbsent(masName, createMultipleMas(masName, oxySpace));
        Mas mas = masMap.get(masName);
        putMasMeta(mas, masName);
        return mas;
    }

    /**
     * Create multipleMas instance
     *
     * @param masName  Mas name
     * @param oxySpace OxySpacelist
     * @return Mas instance
     */
    public abstract Mas createMultipleMas(String masName, List<BaseOxy> oxySpace);

    /**
     * Create multiple Mas instance through default @OxySpaceBean
     *
     * @return Mas instance
     */
    public Mas createMultipleMasByKey() {
        return createMultipleMasByKey(OxySpaceBeanCollector.DEFAULT_STARTUP_OXYSPACE_BEAN_NAME);
    }

    /**
     * Create multipleMas instance through custom oxySpace
     *
     * @param masName Mas name
     * @return Mas instance
     */
    public Mas createMultipleMasByKey(String masName) {
        Mas multipleMas = createMultipleMas(masName, fetchOxySpace(masName));
        putMasMeta(multipleMas, masName);
        return multipleMas;
    }

    private void putMasMeta(Mas mas, String masName) {
        OxySpaceBean oxySpaceBean = OxySpaceBeanCollector.getInstance().getOxySpaceBeansAnnotationMapping().get(masName);
        if (oxySpaceBean != null) {
            mas.setFirstQuery(oxySpaceBean.query());
        }
    }

    /**
     * Get Mas instance's OxySpace
     *
     * @param masName Mas name
     * @return OxySpacelist
     */
    private List<BaseOxy> fetchOxySpace(String masName) {
        List<BaseOxy> oxySpace = null;
        // plugs @OxySpaceBean
        oxySpace = oxySpaceBeanCollector.getOxySpaceBeans(masName);
        // plugs oxySpace
        if (oxySpace == null && oxySpaceProvider != null) {
            oxySpace = oxySpaceProvider.getCustomOxySpace().get(masName);
        }
        // plugs global oxySpace
        if (oxySpace == null) {
            oxySpace = GlobalOxySpaceManager.getInstance().getCustomGlobalOxySpace().get(masName);
        }
        // default oxySpace
        if (oxySpace == null || oxySpace.size() == 0) {
            oxySpace = defaultOxySpace;
            log.warn(masName + " oxySpace not found, using default");
        }
        return oxySpace;
    }
}