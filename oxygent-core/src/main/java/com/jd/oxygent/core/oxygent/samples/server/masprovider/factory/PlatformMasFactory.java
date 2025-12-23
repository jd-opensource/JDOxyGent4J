package com.jd.oxygent.core.oxygent.samples.server.masprovider.factory;

/**
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public abstract class PlatformMasFactory extends MasFactory {
    /**
     * Whether the current platform is supported
     *
     * @return true/false
     */
    public abstract boolean supportsPlatform();

    /**
     * Get platform priority
     *
     * @return priority
     */
    public abstract int getPriority();

    /**
     * Get platform name
     *
     * @return platform name
     */
    public abstract String getPlatformName();
}