package com.jd.oxygent.core.oxygent.samples.server.masprovider;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Getter
public class GlobalOxySpaceManager {

    private final Map<String, List<BaseOxy>> customGlobalOxySpace = new HashMap<>();

    public OxySpaceProvide putCustomOxySpaceName(String name) {
        List<BaseOxy> baseOxyList = new ArrayList<>();
        OxySpaceProvide oxySpaceProvide = new OxySpaceProvide(baseOxyList);
        customGlobalOxySpace.put(name, baseOxyList);
        return oxySpaceProvide;
    }

    @AllArgsConstructor
    public final class OxySpaceProvide {
        private List<BaseOxy> baseOxyList = null;

        public OxySpaceProvide addOxy(BaseOxy baseOxy) {
            baseOxyList.add(baseOxy);
            return this;
        }

        public GlobalOxySpaceManager toRoot() {
            return GlobalOxySpaceManager.this;
        }
    }

    private static class Holder {
        private static final GlobalOxySpaceManager INSTANCE = new GlobalOxySpaceManager();
    }

    public static GlobalOxySpaceManager getInstance() {
        return Holder.INSTANCE;
    }

    // Private constructor
    private GlobalOxySpaceManager() {
        // Prevent reflection from creating instances
        if (Holder.INSTANCE != null) {
            throw new IllegalStateException("Already initialized");
        }
    }

    @Override
    public String toString() {
        return customGlobalOxySpace.toString();
    }

}
