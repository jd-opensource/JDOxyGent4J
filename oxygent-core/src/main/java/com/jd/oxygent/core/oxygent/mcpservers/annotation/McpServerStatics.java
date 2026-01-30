package com.jd.oxygent.core.oxygent.mcpservers.annotation;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public final class McpServerStatics {
    public static String mode=null;
    public static String localhost=null;
    public static String port=null;
    public static String transport=null;
    public static boolean autoScan=false;
    public static List<String> scanBasePackages = new ArrayList<>();
    public static List<String> scanClasss = new ArrayList<>();
}
