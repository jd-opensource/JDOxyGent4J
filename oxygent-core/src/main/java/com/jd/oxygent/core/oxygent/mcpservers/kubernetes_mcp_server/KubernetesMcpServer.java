package com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server;

import java.util.HashSet;
import java.util.Set;

/**
 * Kubernetes MCP Server package initializer.
 * <p>
 * Provides:
 * - Global McpServer instance (used for unified registration on various tool modules)
 * - Environment variable reading for non-destructive mode and security switches like disable-delete/disable-update
 */
public class KubernetesMcpServer {

    // Security and change switches (controlled by environment variables)
    // - K8S_MCP_READ_ONLY=true: Only allows read-only/non-destructive tools
    // - K8S_MCP_DISABLE_DESTRUCTIVE=true: Prohibits destructive operations (delete / update, etc.)
    private static final boolean READ_ONLY;
    private static final boolean DISABLE_DESTRUCTIVE;

    static {
        // Initialize security switches
        Set<String> trueValues = new HashSet<>();
        trueValues.add("1");
        trueValues.add("true");
        trueValues.add("yes");
        trueValues.add("on");

        String readOnlyValue = System.getenv("K8S_MCP_READ_ONLY");
        READ_ONLY = readOnlyValue != null && trueValues.contains(readOnlyValue.strip().toLowerCase());

        String disableDestructiveValue = System.getenv("K8S_MCP_DISABLE_DESTRUCTIVE");
        DISABLE_DESTRUCTIVE = disableDestructiveValue != null && trueValues.contains(disableDestructiveValue.strip().toLowerCase());
    }

    /**
     * Returns whether read-only mode is enabled.
     * This mode typically removes destructive tools such as delete/update, retaining only a minimal set of read-only and safe create/update tools.
     */
    public static boolean isReadOnly() {
        return READ_ONLY;
    }

    /**
     * Returns whether destructive operations (DELETE/UPDATE, etc.) are disabled.
     * When READ-ONLY is not enabled, this switch can also be used to fine-grain control the tool set.
     */
    public static boolean isDisableDestructive() {
        return DISABLE_DESTRUCTIVE;
    }
}
