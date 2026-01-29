package com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server;

import com.jd.oxygent.core.oxygent.mcpservers.engine.McpServer;

import java.util.HashSet;
import java.util.Set;

/**
 * Kubernetes MCP Server package initializer.
 * <p>
 * 提供:
 * - 全局 McpServer 实例 (用于在各工具模块上统一注册)
 * - 非破坏模式与禁删/禁更新等安全开关的环境变量读取
 */
public class KubernetesMcpServer {

    // 全局 MCP 实例：各工具模块通过引用此实例并注册工具
    // 运行传输模式由启动入口 ServerMain 控制
    public static final McpServer MCP = new McpServer();

    // 安全与变更开关（环境变量控制）
    // - K8S_MCP_READ_ONLY=true: 仅允许只读/非破坏工具
    // - K8S_MCP_DISABLE_DESTRUCTIVE=true: 禁止 destructive 类操作（delete / update 等）
    private static final boolean READ_ONLY;
    private static final boolean DISABLE_DESTRUCTIVE;

    static {
        // 初始化安全开关
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
     * 返回是否启用只读模式（READ-ONLY）。
     * 该模式通常会移除 delete/update 等破坏性工具，仅保留只读与创建/更新安全工具的最小集合。
     */
    public static boolean isReadOnly() {
        return READ_ONLY;
    }

    /**
     * 返回是否禁用破坏性操作（DELETE/UPDATE 等）。
     * 在 READ-ONLY 未开启时，也可通过该开关细粒度限制工具集合。
     */
    public static boolean isDisableDestructive() {
        return DISABLE_DESTRUCTIVE;
    }
}
