

/**
 * Mermaid SDK for Flowchart Generation
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */

function convertAgentFlowToMermaidWithSubgraphs(nodes) {
        // Node type to style mapping
        const nodeTypeStyles = {
            'user': 'fill:#6f6,stroke:#333',
            'agent': 'fill:#bbf,stroke:#333',
            'llm': 'fill:#f96,stroke:#333',
            'tool': 'fill:#f9f,stroke:#333',
            'output': 'fill:#fff,stroke:#333,stroke-width:2px',
            'subgraph': 'fill:#eee,stroke:#666,stroke-dasharray:5 5'
        };

        // Store all elements
        const elements = {
            nodeDefinitions: [],
            connections: [],
            styleDefinitions: [],
            subgraphs: {}
        };

        // Store processed node IDs
        const processedNodes = new Set();

        // Find root node
        const rootNode = nodes.find(node => !node.father_node_id && node.caller === 'user') || nodes[0];
        if (!rootNode) throw new Error('Root node not found');

        // Build subgraph hierarchy
        function buildSubgraphHierarchy() {
            const hierarchy = {};

            nodes.forEach(node => {
                if (node.subgraph && !hierarchy[node.subgraph]) {
                    hierarchy[node.subgraph] = {
                        parent: getParentSubgraph(node.subgraph, nodes),
                        nodes: []
                    };
                }

                if (node.subgraph) {
                    hierarchy[node.subgraph].nodes.push(node.node_id);
                }
            });

            return hierarchy;
        }

        // Get parent subgraph
        function getParentSubgraph(subgraph, nodes) {
            const parts = subgraph.split('.');
            if (parts.length > 1) {
                return parts.slice(0, -1).join('.');
            }
            return null;
        }

        // Process node
        function processNode(node, currentSubgraph = null) {
            if (processedNodes.has(node.node_id)) return;
            processedNodes.add(node.node_id);

            // Determine node display properties
            const {displayName, nodeType} = getNodeDisplayInfo(node);

            // Special handling for output nodes
            if (node.node_id === nodes[nodes.length - 1].node_id) {
                const outputContent = formatOutputContent(node.output);
                elements.nodeDefinitions.push(`${node.node_id}["${outputContent}"]`);
                elements.styleDefinitions.push(`class ${node.node_id} output`);
                return;
            }

            // Add node definition
            elements.nodeDefinitions.push(`${node.node_id}["${displayName}"]`);
            elements.styleDefinitions.push(`class ${node.node_id} ${nodeType}`);

            // Process child nodes
            processChildNodes(node, currentSubgraph);

            // Process post nodes
            processPostNodes(node, currentSubgraph);
        }

        // Get node display information
        function getNodeDisplayInfo(node) {
            let displayName, nodeType;

            if (node.node_type === 'agent') {
                displayName = node.callee || node.node_id;
                nodeType = 'agent';
            } else if (node.node_type === 'llm') {
                displayName = node.callee || 'qwen32b';
                nodeType = 'llm';
            } else if (node.node_type === 'tool') {
                displayName = node.callee || 'tool';
                nodeType = 'tool';
            } else {
                displayName = node.node_id;
                nodeType = 'agent';
            }

            // Shorten overly long display names
            displayName = displayName.length > 15 ?
                `${displayName.substring(0, 12)}...` : displayName;

            return {displayName, nodeType};
        }

        // Format output content
        function formatOutputContent(output) {
            if (!output) return 'Output Result';

            // Extract key information or simplify output
            const maxLength = 50;
            if (output.length > maxLength) {
                const firstLine = output.split('\n')[0];
                return `${firstLine.substring(0, maxLength)}...`;
            }
            return output.replace(/\n/g, '<br/>');
        }

        // Process child nodes
        function processChildNodes(node, currentSubgraph) {
            if (node.child_node_ids && node.child_node_ids.length > 0) {
                node.child_node_ids.forEach(childId => {
                    const childNode = nodes.find(n => n.node_id === childId);
                    if (childNode) {
                        // Check if child node belongs to different subgraph
                        if (childNode.subgraph !== currentSubgraph) {
                            // If child node has subgraph but current doesn't, create subgraph connection first
                            if (childNode.subgraph && !currentSubgraph) {
                                elements.connections.push(`${node.node_id} --> ${childNode.subgraph}`);
                            }
                            // If current is in subgraph but child node isn't, connect to subgraph exit
                            else if (currentSubgraph && !childNode.subgraph) {
                                elements.connections.push(`${currentSubgraph}_exit --> ${childNode.node_id}`);
                            }
                            // Other cases: direct connection
                            else {
                                elements.connections.push(`${node.node_id} --> ${childNode.node_id}`);
                            }
                        } else {
                            elements.connections.push(`${node.node_id} --> ${childNode.node_id}`);
                        }

                        processNode(childNode, childNode.subgraph || currentSubgraph);
                    }
                });
            }
        }

        // Process post nodes
        function processPostNodes(node, currentSubgraph) {
            if (node.post_node_ids && node.post_node_ids.length > 0) {
                node.post_node_ids.forEach(postId => {
                    const postNode = nodes.find(n => n.node_id === postId);
                    if (postNode) {
                        // Connection within same subgraph
                        if (postNode.subgraph === currentSubgraph) {
                            elements.connections.push(`${node.node_id} --> ${postNode.node_id}`);
                        }
                        // Cross-subgraph connection
                        else if (currentSubgraph && !postNode.subgraph) {
                            elements.connections.push(`${currentSubgraph}_exit --> ${postNode.node_id}`);
                        }
                        // Other cases
                        else {
                            elements.connections.push(`${node.node_id} --> ${postNode.node_id}`);
                        }

                        processNode(postNode, postNode.subgraph || currentSubgraph);
                    }
                });
            }
        }

        // Build subgraph structure
        function buildSubgraphs() {
            const hierarchy = buildSubgraphHierarchy();

            // Build subgraphs by hierarchy
            Object.entries(hierarchy).forEach(([subgraphName, {nodes: subgraphNodes}]) => {
                const parent = hierarchy[subgraphName].parent;
                const subgraphId = subgraphName.replace(/\./g, '_');

                // Subgraph start
                let subgraphDef = `    subgraph ${subgraphId}["${subgraphName}"]\n`;

                // Add nodes within subgraph
                subgraphNodes.forEach(nodeId => {
                    const node = nodes.find(n => n.node_id === nodeId);
                    if (node) {
                        subgraphDef += `      ${nodeId}\n`;
                    }
                });

                // Add subgraph entry and exit
                subgraphDef += `      ${subgraphId}_entry[ ]:::invisible\n`;
                subgraphDef += `      ${subgraphId}_exit[ ]:::invisible\n`;
                subgraphDef += `    end\n`;

                // Store subgraph definition
                elements.subgraphs[subgraphName] = subgraphDef;

                // Add subgraph style
                elements.styleDefinitions.push(`class ${subgraphId} subgraph`);
            });

            // Build subgraph connection relationships
            Object.entries(hierarchy).forEach(([subgraphName, {parent}]) => {
                const subgraphId = subgraphName.replace(/\./g, '_');

                if (parent) {
                    const parentId = parent.replace(/\./g, '_');
                    elements.connections.push(`${parentId}_exit --> ${subgraphId}_entry`);
                }
            });
        }

        // Main processing flow
        buildSubgraphs();
        processNode(rootNode, rootNode.subgraph || null);

        // Build final Mermaid code
        let mermaidCode = `%%{init: {'theme': 'base', 'themeVariables': {'curve': 'stepAfter'}}}%%\n`;
        mermaidCode += `flowchart LR\n`;

        // Add invisible node styles
        mermaidCode += `    classDef invisible fill:none,stroke:none,color:transparent\n\n`;

        // Add subgraph definitions
        Object.values(elements.subgraphs).forEach(subgraphDef => {
            mermaidCode += subgraphDef + '\n';
        });

        // Add node definitions
        mermaidCode += `    %% Node definitions\n`;
        elements.nodeDefinitions.forEach(def => {
            mermaidCode += `    ${def}\n`;
        });

        // Add connection relationships
        mermaidCode += `    \n    %% Connection relationships\n`;
        elements.connections.forEach(conn => {
            mermaidCode += `    ${conn}\n`;
        });

        // Add style definitions
        // mermaidCode += `    \n    %% Style definitions\n`;
        // Object.entries(nodeTypeStyles).forEach(([type, style]) => {
        //     mermaidCode += `    classDef ${type} ${style}\n`;
        // });
        mermaidCode += `    \n`;
        elements.styleDefinitions.forEach(style => {
            mermaidCode += `    ${style}\n`;
        });

        return mermaidCode;
    }
/**
 * Convert agent call data to Mermaid flowchart
 * @param {Array} nodes - Agent call node array
 * @returns {string} - Mermaid flowchart code
 */
function generateFlowchart(nodes) {
    // Node type to style mapping
    const nodeTypeStyles = {
        'user': 'fill:#6f6,stroke:#333',
        'agent': 'fill:#bbf,stroke:#333',
        'llm': 'fill:#f96,stroke:#333',
        'tool': 'fill:#f9f,stroke:#333',
        'output': 'fill:#fff,stroke:#333'
    };

    // Store all node definitions
    const nodeDefinitions = [];
    // Store all connection relationships
    const connections = [];
    // Store style definitions
    const styleDefinitions = [];
    // Store processed node IDs to avoid duplication
    const processedNodes = new Set();

    // First find root node (node without parent)
    const rootNode = nodes.find(node => !node.father_node_id && node.caller === 'user');
    if (!rootNode) {
        throw new Error('Root node not found (user-initiated node)');
    }

    // Recursively process nodes
    function processNode(node) {
        if (processedNodes.has(node.node_id)) return;
        processedNodes.add(node.node_id);

        // Determine node type and display name
        let nodeName, nodeType;
        if (node.node_type === 'agent') {
            nodeName = node.callee || node.node_id;
            nodeType = 'agent';
        } else if (node.node_type === 'llm') {
            nodeName = node.callee || 'qwen32b';
            nodeType = 'llm';
        } else if (node.node_type === 'tool') {
            nodeName = node.callee || 'tool';
            nodeType = 'tool';
        } else {
            nodeName = node.node_id;
            nodeType = 'agent'; // Default to agent type
        }

        // Shorten overly long node ID display
        const displayName = nodeName.length > 15 ? `${nodeName.substring(0, 12)}...` : nodeName;

        // Special handling for output nodes
        if (node.node_id === nodes[nodes.length - 1].node_id) {
            const outputContent = node.output ? node.output.replace(/\n/g, '<br/>') : 'Output Result';
            nodeDefinitions.push(`${node.node_id}["${outputContent}"]`);
            styleDefinitions.push(`class ${node.node_id} output`);
            return;
        }

        // Add node definition
        nodeDefinitions.push(`${node.node_id}["${displayName}"]`);
        // Add style definition
        styleDefinitions.push(`class ${node.node_id} ${nodeType}`);

        // Process child nodes
        if (node.child_node_ids && node.child_node_ids.length > 0) {
            console.log('node', node)
            node.child_node_ids.forEach(childId => {
                const childNode = nodes.find(n => n.node_id === childId);
                if (childNode) {
                    connections.push(`${node.node_id} --> ${childNode.node_id}`);
                    processNode(childNode);
                }
            });
        }

        // Process post nodes (linear flow)
        if (node.post_node_ids && node.post_node_ids.length > 0) {
            node.post_node_ids.forEach(postId => {
                const postNode = nodes.find(n => n.node_id === postId);
                if (postNode) {
                    connections.push(`${node.node_id} --> ${postNode.node_id}`);
                    processNode(postNode);
                }
            });
        }
    }

    // Start processing from root node
    processNode(rootNode);

    // Build final Mermaid code
    let mermaidCode = `%%{init: {'theme': 'base', 'themeVariables': {'curve': 'stepAfter'}}}%%\n`;
    mermaidCode += `flowchart LR\n`;

    // Add node definitions
    mermaidCode += `    %% Node definitions\n`;
    nodeDefinitions.forEach(def => {
        mermaidCode += `    ${def}\n`;
    });

    // Add connection relationships
    mermaidCode += `    \n    %% Connection relationships\n`;
    console.log('connections', connections);
    connections.forEach(conn => {
        mermaidCode += `    ${conn}\n`;
    });

    // Add style definitions
    mermaidCode += `    \n    %% Style definitions\n`;
    Object.entries(nodeTypeStyles).forEach(([type, style]) => {
        mermaidCode += `    classDef ${type} ${style}\n`;
    });
    mermaidCode += `    \n`;
    styleDefinitions.forEach(style => {
        mermaidCode += `    ${style}\n`;
    });

    return mermaidCode;
}


function createFlowchartFromData(nodesData) {
    // Create a map of nodes by their ID for quick lookup
    const nodesMap = new Map(nodesData.map(node => [node.node_id, node]));

    // Initialize the flowchart structure
    const flowchart = {
        nodes: [],
        edges: [],
        subgraphs: {}
    };

    // Process each node to create the flowchart elements
    nodesData.forEach(node => {
        // Create the node for the flowchart
        const flowchartNode = {
            id: node.node_id,
            type: node.node_type,
            name: node.name || node.node_type,
            subgraph: node.subgraph,
            data: node // Store the original data for reference
        };

        // Add the node to the main nodes list
        flowchart.nodes.push(flowchartNode);

        // Ensure the subgraph exists in our subgraphs map
        if (!flowchart.subgraphs[node.subgraph]) {
            flowchart.subgraphs[node.subgraph] = {
                id: node.subgraph,
                name: node.subgraph,
                nodes: [],
                edges: []
            };
        }

        // Add the node to its subgraph
        flowchart.subgraphs[node.subgraph].nodes.push(node.node_id);

        // Process the edges (connections between nodes)
        if (node.post_node_ids && node.post_node_ids.length > 0) {
            node.post_node_ids.forEach(targetId => {
                const edge = {
                    id: `${node.node_id}-${targetId}`,
                    source: node.node_id,
                    target: targetId,
                    label: ''
                };

                // Add to main edges list
                flowchart.edges.push(edge);

                // Add to subgraph edges if both nodes are in the same subgraph
                const targetNode = nodesMap.get(targetId);
                if (targetNode && targetNode.subgraph === node.subgraph) {
                    flowchart.subgraphs[node.subgraph].edges.push(edge);
                }
            });
        }
    });

    return flowchart;
}



// Render function
function renderFlowchart(data, containerId) {
    const code = generateFlowchart(data);
    console.log(code)
    $('#flowchart-container').html('').removeAttr('data-processed');
    const container = document.getElementById(containerId);
    container.innerHTML = code;
    mermaid.run({
        querySelector: `#${containerId}`
    });
}









