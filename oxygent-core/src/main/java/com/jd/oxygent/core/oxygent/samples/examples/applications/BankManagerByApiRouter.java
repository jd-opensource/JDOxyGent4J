package com.jd.oxygent.core.oxygent.samples.examples.applications;

import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.annotation.ApiEndpoint;
import com.jd.oxygent.core.oxygent.samples.server.annotation.ApiParam;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bank Service Example Class
 */
@Slf4j
public class BankManagerByApiRouter {

    static {
        Config.getServer().setPort(8090);
    }

    private Map<String, String> userProfiles = new HashMap<>();

    public BankManagerByApiRouter() {
        // Initialize some sample data
        userProfiles.put("001", "Arlen, a student, likes music");
        userProfiles.put("002", "Tom, a programmer, likes sports");
    }

    @ApiEndpoint(
            path = "/user_profile_retrieve",
            method = ApiEndpoint.HttpMethod.POST,
            description = "A tool for querying user profile",
            tags = {"bank"}
    )
    public String userProfileRetrieve(
            @ApiParam(name = "query", description = "query") String query,
            @ApiParam(name = "user_pin", description = "SystemArg.user_pin") String user_pin,
            @ApiParam(name = "agent_pin", description = "SystemArg.agent_pin") String agent_pin
    ) {
        log.info("Querying user profile - user_pin: {}, agent_pin: {}, query: {}",
                user_pin, agent_pin, query);

        String portrait = userProfiles.getOrDefault(user_pin, "Nothing");
        return String.format("The current user profile is: %s", portrait);
    }

    @ApiEndpoint(
            path = "/user_profile_deposit",
            method = ApiEndpoint.HttpMethod.POST,
            description = "A tool for updating user profile",
            tags = {"bank"}
    )
    public String userProfileDeposit(
            @ApiParam(name = "content", description = "content") String content,
            @ApiParam(name = "user_pin", description = "SystemArg.user_pin") String user_pin,
            @ApiParam(name = "agent_pin", description = "SystemArg.agent_pin") String agent_pin
    ) {
        log.info("Updating user profile - user_pin: {}, agent_pin: {}, content: {}",
                user_pin, agent_pin, content);

        userProfiles.put(user_pin, content);
        return "updated user_profile";
    }

    @ApiEndpoint(
            path = "/list_banks",
            method = ApiEndpoint.HttpMethod.GET,
            description = "Get all bank endpoints",
            tags = {"system"}
    )
    public List<Map<String, Object>> listBanks() {
        return getBanksFromApiEndpoints();
    }

    private List<Map<String, Object>> getBanksFromApiEndpoints() {
        List<Map<String, Object>> banks = new ArrayList<>();

        try {
            Class<?> clazz = this.getClass();
            Method[] methods = clazz.getDeclaredMethods();

            for (Method method : methods) {
                if (method.isAnnotationPresent(ApiEndpoint.class)) {
                    ApiEndpoint endpoint = method.getAnnotation(ApiEndpoint.class);

                    // Check if there is a "bank" tag
                    boolean hasBankTag = false;
                    for (String tag : endpoint.tags()) {
                        if ("bank".equals(tag)) {
                            hasBankTag = true;
                            break;
                        }
                    }

                    if (hasBankTag) {
                        Map<String, Object> inputSchema = new HashMap<>();
                        Map<String, Object> properties = new HashMap<>();
                        List<String> required = new ArrayList<>();

                        // Get method parameter information
                        java.lang.reflect.Parameter[] parameters = method.getParameters();
                        for (java.lang.reflect.Parameter param : parameters) {
                            if (param.isAnnotationPresent(ApiParam.class)) {
                                ApiParam apiParam = param.getAnnotation(ApiParam.class);

                                // Determine parameter type
                                String paramType = getParamType(param.getType());

                                Map<String, Object> paramSchema = new HashMap<>();
                                paramSchema.put("type", paramType);
                                paramSchema.put("description", apiParam.description());

                                properties.put(apiParam.name(), paramSchema);
                                required.add(apiParam.name());
                            }
                        }

                        inputSchema.put("type", "object");
                        inputSchema.put("properties", properties);
                        inputSchema.put("required", required);

                        Map<String, Object> bankInfo = new HashMap<>();
                        bankInfo.put("name", method.getName());
                        bankInfo.put("endpoint", endpoint.path());
                        bankInfo.put("methods", new String[]{endpoint.method().toString()});
                        bankInfo.put("description", endpoint.description());
                        bankInfo.put("inputSchema", inputSchema);

                        banks.add(bankInfo);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error getting banks from API endpoints", e);
        }

        return banks;
    }

    private String getParamType(Class<?> paramClass) {
        if (paramClass == String.class) {
            return "string";
        } else if (paramClass == Integer.class || paramClass == int.class) {
            return "integer";
        } else if (paramClass == Float.class || paramClass == float.class ||
                paramClass == Double.class || paramClass == double.class) {
            return "number";
        } else if (paramClass == Boolean.class || paramClass == boolean.class) {
            return "boolean";
        } else {
            return "string";
        }
    }

    public static void main(String[] args) throws Exception {
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(args);
    }
}