package com.jd.oxygent.core.oxygent.samples.server.vo;

import com.jd.oxygent.core.Mas;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agent node converter utility class
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class AgentNodeConverter {
    /**
     * Convert original AgentNode to new structure containing path and generate id_dict
     */
    public static OrganizationWrapper convertToOrganization(Mas.AgentNode rootNode) {
        // Map for storing node name and ID mapping
        Map<String, Integer> idDict = new LinkedHashMap<>();

        // Convert node structure and build id_dict simultaneously
        AgentNodeWithPath organizationNode = convertWithPath(rootNode, new ArrayList<>(), idDict);

        return new OrganizationWrapper(organizationNode, idDict);
    }

    /**
     * Recursive conversion method
     */
    private static AgentNodeWithPath convertWithPath(Mas.AgentNode originalNode,
                                                     List<String> currentPath,
                                                     Map<String, Integer> idDict) {
        // Create new path list (avoid modifying original list)
        List<String> newPath = new ArrayList<>(currentPath);
        newPath.add(originalNode.getName());

        // Create new node
        AgentNodeWithPath newNode = new AgentNodeWithPath();
        newNode.setName(originalNode.getName());
        newNode.setType(originalNode.getType());
        newNode.setPath(newPath);

        // If this is the first time encountering this node name, add to id_dict
        if (!idDict.containsKey(originalNode.getName()) && ("flow".equals(originalNode.getType()) || "agent".equals(originalNode.getType()))) {
            idDict.put(originalNode.getName(), idDict.size());
        }

        // Recursively process child nodes
        List<AgentNodeWithPath> newChildren = new ArrayList<>();
        for (Mas.AgentNode child : originalNode.getChildren()) {
            AgentNodeWithPath newChild = convertWithPath(child, newPath, idDict);
            newChildren.add(newChild);
        }
        newNode.setChildren(newChildren);

        return newNode;
    }

    /**
     * Alternative method: if need to build id_dict separately after conversion
     */
    public static Map<String, Integer> buildIdDict(AgentNodeWithPath rootNode) {
        Map<String, Integer> idDict = new LinkedHashMap<>();
        buildIdDictRecursive(rootNode, idDict);
        return idDict;
    }

    private static void buildIdDictRecursive(AgentNodeWithPath node, Map<String, Integer> idDict) {
        if (!idDict.containsKey(node.getName())) {
            idDict.put(node.getName(), idDict.size());
        }

        for (AgentNodeWithPath child : node.getChildren()) {
            buildIdDictRecursive(child, idDict);
        }
    }
}