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
package com.jd.oxygent.core.oxygent.utils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Data structure processing utility class
 *
 * <h3>Feature Description</h3>
 * <ul>
 *   <li>Provides node data processing and transformation capabilities</li>
 *   <li>Supports building complex tree data structures</li>
 *   <li>Implements automatic calculation of node predecessor/successor and parent-child relationships</li>
 *   <li>Supports processing and sorting of parallel node groups</li>
 * </ul>
 *
 * <h3>Core Features</h3>
 * <ul>
 *   <li>Node value normalization: Unified node data format and status code conversion</li>
 *   <li>Relationship building: Automatic calculation of node predecessor/successor and parent-child relationships</li>
 *   <li>Tree construction: Build hierarchical tree structures from flattened node lists</li>
 *   <li>Parallel processing: Support identification and correct sorting of parallel node groups</li>
 * </ul>
 *
 * <h3>Application Scenarios</h3>
 * <ul>
 *   <li>Node relationship processing in workflow engines</li>
 *   <li>Dependency relationship calculation in task scheduling systems</li>
 *   <li>Tree display of organizational structures or classification systems</li>
 *   <li>Data modeling for flowcharts or decision trees</li>
 * </ul>
 *
 * <h3>Data Model Description</h3>
 * <ul>
 *   <li>Nodes must contain node_id as unique identifier</li>
 *   <li>Support father_node_id to represent parent-child relationships</li>
 *   <li>Support pre_node_ids to represent predecessor dependency relationships</li>
 *   <li>Support parallel_id to identify parallel execution groups</li>
 *   <li>Support order field to control node execution order</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class DataUtils {

    /*
     * Node status code mapping table
     */
    private static Map<String, Integer> STATUS_CODE = new HashMap<>() {{
        this.put("CREATED", 1);
        this.put("RUNNING", 2);
        this.put("COMPLETED", 3);
        this.put("SUCCESS", 4);
        this.put("FAILED", 5);
        this.put("PAUSED", 6);
        this.put("SKIPPED", 7);
        this.put("CANCELED", 8);
    }};

    /**
     * Normalize node data values
     * <p>
     * Performs format normalization and type conversion on node data to ensure data structure consistency.
     * Mainly handles string splitting, status code conversion, and message structure normalization.
     *
     * <h4>Processing Logic</h4>
     * <ol>
     *   <li>Convert pipe-separated string fields to array format</li>
     *   <li>Handle normalization of null values and empty values</li>
     *   <li>Convert status strings to corresponding status codes</li>
     *   <li>Normalize LLM node message structure to maintain frontend consistency</li>
     * </ol>
     *
     * <h4>Field Conversion Rules</h4>
     * <ul>
     *   <li>call_stack: String → String array (split by |)</li>
     *   <li>node_id_stack: String → String array (split by |)</li>
     *   <li>pre_node_ids: String → String array (split by |)</li>
     *   <li>extra: "null" → "{}"</li>
     *   <li>state: Status string → Status code integer</li>
     * </ul>
     *
     * @param nodeData Node data mapping table to be processed, will be modified directly, cannot be null
     */
    public static void changeNodeValue(Map<String, Object> nodeData) {
        nodeData.put("call_stack", nodeData.get("call_stack").toString().split("\\|"));
        nodeData.put("node_id_stack", nodeData.get("node_id_stack").toString().split("\\|"));
        if (nodeData.get("pre_node_ids") instanceof String) {
            nodeData.put("pre_node_ids", nodeData.get("pre_node_ids").toString().split("\\|"));
        }
        if ("null".equals(nodeData.get("extra"))) {
            nodeData.put("extra", "{}");
        }
        if (nodeData.containsKey("state")) {
            nodeData.put("state", STATUS_CODE.get(nodeData.get("state")));
        }
        try {
            //Because localAgent stores Message as Message java instance, serialization causes frontend inconsistency, convert to frontend consistent structure
            if (nodeData.get("node_type").toString().equals("llm")) {
                Map<String, Object> argumentsMap = (Map) ((Map) nodeData.get("input")).get("arguments");
                Map messagesMap = (Map) argumentsMap.get("messages");
                argumentsMap.put("messages", messagesMap.get("messages"));
            }
        } catch (Exception e) {
        }
    }

    /**
     * Build relationship mapping between nodes
     * <p>
     * Automatically calculate and add successor node and child node relationship information for each node in the node list.
     * Based on existing predecessor relationships (pre_node_ids) and parent-child relationships (father_node_id),
     * reverse calculate successor relationships and child node relationships.
     *
     * <h4>Added Fields</h4>
     * <ul>
     *   <li>post_node_ids: List of all successor node IDs that have the current node as predecessor</li>
     *   <li>child_node_ids: List of all direct child node IDs that have the current node as parent</li>
     * </ul>
     *
     * <h4>Processing Logic</h4>
     * <ol>
     *   <li>Build fast mapping table from node_id to node object</li>
     *   <li>Initialize empty post_node_ids and child_node_ids lists for all nodes</li>
     *   <li>Traverse each node's pre_node_ids, add post_node_ids to corresponding predecessor nodes</li>
     *   <li>Traverse each node's father_node_id, add child_node_ids to corresponding parent nodes</li>
     *   <li>Handle predecessor/successor relationships between sibling nodes (based on order sorting)</li>
     * </ol>
     *
     * <h4>Special Handling</h4>
     * <ul>
     *   <li>Child nodes under the same parent node will automatically establish predecessor/successor relationships based on their position in child_node_ids</li>
     *   <li>Avoid index out of bounds exceptions, safely handle boundary cases</li>
     * </ul>
     *
     * @param nodes Node list, each node must contain node_id field,
     *              optionally contains pre_node_ids and father_node_id fields, cannot be null
     */
    public static void addPostAndChildNodeIds(List<Map<String, Object>> nodes) {
        // Build mapping from node_id to node
        Map<String, Map<String, Object>> nodeMap = nodes.stream()
                .collect(Collectors.toMap(
                        n -> n.getOrDefault("node_id", "").toString(),
                        Function.identity(),
                        (existing, replacement) -> existing, // Handle duplicate keys
                        HashMap::new
                ));
        // Resolve the issue of node order disruption caused by asynchronous execution
        List<Map<String, Object>> collect = nodes.stream().sorted((nodePre, nodeNext) -> {
            return nodePre.get("create_time").toString().compareTo(nodeNext.get("create_time").toString());
        }).toList();
        nodes.clear();
        nodes.addAll(collect);

        // Initialize postNodeIds and childNodeIds
        for (Map<String, Object> node : nodes) {
            node.put("post_node_ids", new ArrayList<String>());
            node.put("child_node_ids", new ArrayList<String>());
        }

        // Fill postNodeIds (based on pre_node_ids)
        for (Map<String, Object> node : nodes) {
            @SuppressWarnings("unchecked")
            List<String> preNodeIds = (List<String>) node.getOrDefault("pre_node_ids", List.of());
            for (String preId : preNodeIds) {
                if (preId != null && nodeMap.containsKey(preId)) {
                    @SuppressWarnings("unchecked")
                    List<String> postNodeIds = (List<String>) nodeMap.get(preId).get("post_node_ids");
                    postNodeIds.add(node.getOrDefault("node_id", "").toString());
                }
            }

            // Fill childNodeIds (based on father_node_id)
            String fatherNodeId = node.getOrDefault("father_node_id", "").toString();
            if (fatherNodeId != null && nodeMap.containsKey(fatherNodeId)) {
                @SuppressWarnings("unchecked")
                List<String> childNodeIds = (List<String>) nodeMap.get(fatherNodeId).get("child_node_ids");
                childNodeIds.add(node.getOrDefault("node_id", "").toString());
            }
        }

//        for (Map<String, Object> node : nodes) {
//            String fatherNodeId = node.getOrDefault("father_node_id", "").toString();
//            if (null != fatherNodeId && !"".equals(fatherNodeId)) {
//                List<String> childNodeIds = (List<String>) nodeMap.get(fatherNodeId).get("child_node_ids");
//                int nodeId = childNodeIds.indexOf(node.get("node_id"));
//                if (nodeId != -1) {
//                    if (nodeId > 0) {
//                        ((List) node.get("pre_node_ids")).add(childNodeIds.get(nodeId - 1));
//                    }
//                    try { //Handle index out of bounds exception normally
//                        String nextNodeId = childNodeIds.get(nodeId + 1);
//                        if (null != nextNodeId) {
//                            ((List) node.get("post_node_ids")).add(nextNodeId);
//                        }
//                    } catch (Exception e) {
//                    }
//                }
//            }
//        }
    }

    /**
     * Build hierarchical tree data structure
     * <p>
     * Convert flattened node list to hierarchical tree structure, supporting parallel node group processing.
     * Automatically identify root nodes, build parent-child relationships, and correctly handle node execution order.
     *
     * <h4>Building Algorithm</h4>
     * <ol>
     *   <li>Create node mapping table and deep copy original data</li>
     *   <li>Identify root nodes (nodes with null from_node_id)</li>
     *   <li>Build child node mapping relationships</li>
     *   <li>Recursively build tree structure, handle parallel node groups</li>
     * </ol>
     *
     * <h4>Data Structure Requirements</h4>
     * <ul>
     *   <li>Each node must contain node_id, node_name, node_type</li>
     *   <li>Root node's from_node_id must be null</li>
     *   <li>Child nodes point to parent nodes through from_node_id</li>
     *   <li>Parallel nodes identified by same parallel_id</li>
     *   <li>Node order controlled by order field</li>
     * </ul>
     *
     * <h4>Output Structure</h4>
     * <ul>
     *   <li>Root node contains node_id, node_name, node_type and nodes fields</li>
     *   <li>nodes field contains child node array, single nodes or parallel node groups</li>
     *   <li>Parallel node groups represented as array of node arrays</li>
     * </ul>
     *
     * @param inputData Node list to be processed, each node must contain necessary fields, cannot be null or empty
     * @return Map&lt;String, Object&gt; Built tree structure root node
     * @throws IllegalArgumentException Thrown when root node cannot be found
     */
    public static Map<String, Object> buildTree(List<Map<String, Object>> inputData) {
        // Create node mapping (deep copy)
        Map<String, Map<String, Object>> nodeDict = new HashMap<>();
        for (Map<String, Object> node : inputData) {
            Map<String, Object> nodeCopy = new HashMap<>(node);
            nodeCopy.put("nodes", new ArrayList<>());
            nodeDict.put(node.getOrDefault("node_id", "").toString(), nodeCopy);
        }

        // Find root node (node with null from_node_id)
        List<Map<String, Object>> roots = nodeDict.values().stream()
                .filter(node -> node.get("from_node_id") == null)
                .collect(Collectors.toList());

        if (roots.isEmpty()) {
            throw new IllegalArgumentException("No root node found (node with null 'from_node_id')");
        }

        // Build child node mapping
        Map<String, List<Map<String, Object>>> childrenMap = buildChildrenMap(nodeDict);

        // Build and return tree
        return buildNodeEntry(roots.get(0), childrenMap);
    }

    /**
     * Build parent-child node mapping relationship
     * <p>
     * Traverse node dictionary, build mapping table from parent node ID to child node list based on from_node_id field.
     * Provides efficient child node lookup mechanism for tree structure building.
     *
     * @param nodeDict Node dictionary, key is node_id, value is node object
     * @return Map<String, List < Map < String, Object>>> Mapping table from parent node ID to child node list
     */
    private static Map<String, List<Map<String, Object>>> buildChildrenMap(Map<String, Map<String, Object>> nodeDict) {
        Map<String, List<Map<String, Object>>> childrenMap = new HashMap<>();
        for (Map<String, Object> node : nodeDict.values()) {
            String fromNodeId = node.getOrDefault("from_node_id", "").toString();
            if (fromNodeId != null) {
                childrenMap.computeIfAbsent(fromNodeId, k -> new ArrayList<>()).add(node);
            }
        }
        return childrenMap;
    }

    /**
     * Build tree entry for single node
     * <p>
     * Create single node entry in tree structure, containing basic information and subtree structure.
     * Recursively process child nodes to build complete tree hierarchy.
     *
     * @param node        Current node object to be processed
     * @param childrenMap Mapping table from parent node ID to child node list
     * @return Map<String, Object> Tree node containing node_id, node_name, node_type and nodes fields
     */
    private static Map<String, Object> buildNodeEntry(Map<String, Object> node, Map<String, List<Map<String, Object>>> childrenMap) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("node_id", node.get("node_id"));
        entry.put("node_name", node.get("node_name"));
        entry.put("node_type", node.get("node_type"));
        entry.put("nodes", buildSubtree(node, childrenMap));
        return entry;
    }

    /**
     * Build subtree structure of node
     * <p>
     * Process all child nodes of a node, supporting identification and sorting of parallel node groups.
     * Divide child nodes into regular nodes and parallel node groups, build subtree after sorting by order field.
     *
     * <h4>Processing Steps</h4>
     * <ol>
     *   <li>Get all child nodes of current node</li>
     *   <li>Group by parallel_id: regular nodes and parallel node groups</li>
     *   <li>Process internal sorting of parallel node groups</li>
     *   <li>Merge all child nodes and sort by order</li>
     *   <li>Build final nodes list (single nodes or parallel groups)</li>
     * </ol>
     *
     * @param parent      Parent node object
     * @param childrenMap Mapping table from parent node ID to child node list
     * @return List<Object> Subtree node list, elements can be single nodes or parallel node group arrays
     */
    private static List<Object> buildSubtree(Map<String, Object> parent, Map<String, List<Map<String, Object>>> childrenMap) {
        List<Map<String, Object>> children = childrenMap.getOrDefault(parent.getOrDefault("node_id", "").toString(), new ArrayList<>());

        // Group: non-parallel nodes and parallel node groups
        List<Map<String, Object>> nonParallel = new ArrayList<>();
        Map<String, List<Map<String, Object>>> parallelGroups = new HashMap<>();

        for (Map<String, Object> child : children) {
            String parallelId = child.getOrDefault("parallel_id", "").toString();
            if (parallelId != null) {
                parallelGroups.computeIfAbsent(parallelId, k -> new ArrayList<>()).add(child);
            } else {
                nonParallel.add(child);
            }
        }

        // Process parallel groups
        List<ParallelGroup> parallelList = processParallelGroups(parallelGroups);

        // Merge and sort all child nodes
        List<MergedChild> allChildren = mergeAndSortChildren(nonParallel, parallelList);

        // Build final nodes list
        List<Object> nodes = new ArrayList<>();
        for (MergedChild mergedChild : allChildren) {
            if (mergedChild.hasParallelGroup()) {
                // Parallel group: add a node list
                List<Map<String, Object>> groupNodes = new ArrayList<>();
                for (Map<String, Object> groupNode : mergedChild.getParallelGroup()) {
                    groupNodes.add(buildNodeEntry(groupNode, childrenMap));
                }
                nodes.add(groupNodes);
            } else {
                // Single node
                nodes.add(buildNodeEntry(mergedChild.getSingleNode(), childrenMap));
            }
        }

        return nodes;
    }

    /**
     * Process sorting and organization of parallel node groups
     * <p>
     * Sort nodes within each parallel group by order field, and record minimum order value of group for global sorting.
     * Ensure correct execution order of nodes within parallel groups while maintaining relative positions between parallel groups.
     *
     * @param parallelGroups Parallel group mapping, key is parallel_id, value is node list of that group
     * @return List<ParallelGroup> Processed parallel group list, containing minimum order and sorted nodes
     */
    private static List<ParallelGroup> processParallelGroups(Map<String, List<Map<String, Object>>> parallelGroups) {
        List<ParallelGroup> parallelList = new ArrayList<>();
        for (List<Map<String, Object>> group : parallelGroups.values()) {
            // Sort by order
            group.sort(Comparator.comparingInt(n -> (int) n.get("order")));
            int minOrder = (int) group.get(0).get("order");
            parallelList.add(new ParallelGroup(minOrder, group));
        }
        return parallelList;
    }

    /**
     * Merge and sort all child nodes
     * <p>
     * Merge regular child nodes and parallel node groups into unified list, and sort by order field.
     * Ensure final execution order meets business logic requirements.
     *
     * @param nonParallel  Non-parallel node list
     * @param parallelList Processed parallel group list
     * @return List<MergedChild> Merged and sorted child node list, uniformly wrapped as MergedChild objects
     */
    private static List<MergedChild> mergeAndSortChildren(List<Map<String, Object>> nonParallel, List<ParallelGroup> parallelList) {
        List<MergedChild> allChildren = new ArrayList<>();

        // Add non-parallel nodes
        for (Map<String, Object> node : nonParallel) {
            allChildren.add(new MergedChild((int) node.get("order"), node));
        }

        // Add parallel groups
        for (ParallelGroup group : parallelList) {
            allChildren.add(new MergedChild(group.minOrder, group.nodes));
        }

        // Sort by order
        allChildren.sort(Comparator.comparingInt(MergedChild::getOrder));
        return allChildren;
    }

    // ========== Internal Helper Classes ==========

    /**
     * Parallel node group data structure
     * <p>
     * Used to represent node set with same parallel_id,
     * record minimum order value within group for global sorting.
     *
     * @author OxyGent Team
     * @version 1.0.0
     * @since 1.0.0
     */
    private static class ParallelGroup {
        /**
         * Minimum order value of nodes within group, used for global sorting
         */
        final int minOrder;
        /**
         * All nodes within parallel group, sorted by order
         */
        final List<Map<String, Object>> nodes;

        /**
         * Construct parallel node group
         *
         * @param minOrder Minimum order value within group
         * @param nodes    Node list
         */
        ParallelGroup(int minOrder, List<Map<String, Object>> nodes) {
            this.minOrder = minOrder;
            this.nodes = nodes;
        }
    }

    /**
     * Merged child node wrapper class
     * <p>
     * Uniformly wrap single nodes and parallel node groups for unified sorting and processing.
     * Uses composite pattern, distinguishes different node types through type identification.
     *
     * @author OxyGent Team
     * @version 1.0.0
     * @since 1.0.0
     */
    private static class MergedChild {
        /**
         * Sort order value of node or group
         */
        private final int order;
        /**
         * Single node object, mutually exclusive with parallelGroup
         */
        private final Map<String, Object> singleNode;
        /**
         * Parallel node group, mutually exclusive with singleNode
         */
        private final List<Map<String, Object>> parallelGroup;

        /**
         * Construct wrapper object for single node
         *
         * @param order      Order value of node
         * @param singleNode Single node object
         */
        MergedChild(int order, Map<String, Object> singleNode) {
            this.order = order;
            this.singleNode = singleNode;
            this.parallelGroup = null;
        }

        /**
         * Construct wrapper object for parallel node group
         *
         * @param order         Order value of group (usually minimum order within group)
         * @param parallelGroup Parallel node group list
         */
        MergedChild(int order, List<Map<String, Object>> parallelGroup) {
            this.order = order;
            this.singleNode = null;
            this.parallelGroup = parallelGroup;
        }

        /**
         * Get sort order value
         *
         * @return Order value of node or group
         */
        public int getOrder() {
            return order;
        }

        /**
         * Check if it is a parallel node group
         *
         * @return true indicates parallel group, false indicates single node
         */
        public boolean hasParallelGroup() {
            return parallelGroup != null;
        }

        /**
         * Get single node object
         *
         * @return Single node object, only valid for non-parallel groups
         */
        public Map<String, Object> getSingleNode() {
            return singleNode;
        }

        /**
         * Get parallel node group
         *
         * @return Parallel node group list, only valid for parallel groups
         */
        public List<Map<String, Object>> getParallelGroup() {
            return parallelGroup;
        }
    }

}
