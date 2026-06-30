package com.jd.oxygent.core.oxygent.transport.a2a;

import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.AgentSkill;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class A2ACard {

    public static String[] cardIdentity(Mas mas) {
        if (mas == null) {
            return new String[]{"master_agent", "A2A facade of OxyGent MAS"};
        }
        String masterName = mas.getMasterAgentName();
        if (masterName == null || masterName.isEmpty()) {
            masterName = "master_agent";
        }
        BaseOxy masterOxy = mas.getOxyNameToOxy().get(masterName);
        if (masterOxy != null) {
            String name = masterOxy.getName() != null && !masterOxy.getName().isEmpty() ? masterOxy.getName() : masterName;
            String desc = masterOxy.getDesc() != null && !masterOxy.getDesc().isEmpty() ? masterOxy.getDesc() : name;
            return new String[]{name, desc};
        }
        return new String[]{masterName, masterName + " via A2A"};
    }

    public static String effectiveTarget(Mas mas, String targetAgentName) {
        if (mas != null && mas.getMasterAgentName() != null && !mas.getMasterAgentName().isEmpty()) {
            return mas.getMasterAgentName();
        }
        if (targetAgentName != null && !targetAgentName.isEmpty()) {
            return targetAgentName;
        }
        if (mas != null) {
            for (Map.Entry<String, BaseOxy> entry : mas.getOxyNameToOxy().entrySet()) {
                if ("agent".equals(entry.getValue().getCategory())) {
                    return entry.getKey();
                }
            }
        }
        return "master_agent";
    }

    public static List<AgentSkill> buildSkillsFromOrg(Mas mas) {
        String[] identity = cardIdentity(mas);
        String cardName = identity[0];
        String cardDesc = identity[1];

        if (mas == null) {
            return List.of(defaultSkill(cardName, cardDesc));
        }

        Mas.AgentNode agentOrg = mas.getAgentOrganization();
        if (agentOrg == null) {
            return List.of(defaultSkill(cardName, cardDesc));
        }

        List<AgentSkill> skills = new ArrayList<>();
        walkOrgNode(agentOrg, new ArrayList<>(), skills, mas);

        if (skills.isEmpty()) {
            return List.of(defaultSkill(cardName, cardDesc));
        }
        return skills;
    }

    private static void walkOrgNode(Mas.AgentNode node, List<String> path, List<AgentSkill> skills, Mas mas) {
        if (node == null) return;
        String nodeName = node.getName() != null ? node.getName() : "";
        String nodeType = node.getType() != null ? node.getType() : "";

        List<String> currPath = new ArrayList<>(path);
        currPath.add(nodeName);

        if (("agent".equals(nodeType) || "flow".equals(nodeType)) && !nodeName.isEmpty()) {
            String desc = "";
            BaseOxy oxy = mas.getOxyNameToOxy().get(nodeName);
            if (oxy != null) {
                desc = oxy.getDesc() != null && !oxy.getDesc().isEmpty() ? oxy.getDesc() : nodeName;
            }
            skills.add(new AgentSkill.Builder()
                    .id(String.join(".", currPath.stream().filter(p -> !p.isEmpty()).toList()))
                    .name(nodeName)
                    .description(desc)
                    .tags(List.of("oxygent", "a2a", nodeType))
                    .inputModes(List.of("text/plain"))
                    .outputModes(List.of("text/plain"))
                    .build());
        }

        if (node.getChildren() != null) {
            for (Mas.AgentNode child : node.getChildren()) {
                walkOrgNode(child, currPath, skills, mas);
            }
        }
    }

    private static AgentSkill defaultSkill(String cardName, String cardDesc) {
        return new AgentSkill.Builder()
                .id(cardName + ".chat")
                .name(cardName)
                .description(cardDesc)
                .tags(List.of("chat", "oxygent", "a2a"))
                .inputModes(List.of("text/plain"))
                .outputModes(List.of("text/plain"))
                .build();
    }

    public static AgentCard buildAgentCard(
            String requestBaseUrl,
            String a2aBasePath,
            String agentVersion,
            Mas mas
    ) {
        String endpoint = requestBaseUrl.replaceAll("/+$", "") + a2aBasePath;
        List<AgentSkill> skills = buildSkillsFromOrg(mas);
        String[] identity = cardIdentity(mas);

        return new AgentCard.Builder()
                .name(identity[0])
                .description(identity[1])
                .version(agentVersion)
                .url(endpoint)
                .capabilities(new AgentCapabilities.Builder()
                        .streaming(true)
                        .stateTransitionHistory(true)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(List.of("text/plain"))
                .defaultOutputModes(List.of("text/plain"))
                .skills(skills)
                .additionalInterfaces(List.of(new AgentInterface("JSONRPC", endpoint)))
                .build();
    }
}
