/**
 * Utility Functions
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */

/**
 * @description Remove duplicates
 * @param {*} data
 * @returns
 */
function unionName(data) {
    return [...new Set(data)]
}

/**
 * @description Convert array to map
 * @param {*} nodes
 * @returns
 */
function arrayToMap(nodes) {
    const nodeMap = []
    for (const node of nodes) {
        nodeMap[node.node_id] = {...node, children: []};
    }
    return nodeMap;
}

/**
 * @description Build tree structure
 * @param {*} nodes
 * @returns
 */
function buildTree(nodes) {
    console.log('nodes', nodes);
    const nodeMap = {};
    const tree = [];

    // First, store all nodes in the mapping table
    for (const node of nodes) {
        nodeMap[node.node_id] = {...node, children: []};
    }

    // Then build tree structure
    for (const node of nodes) {
        if (node.father_node_id === '') {
            tree.push(nodeMap[node.node_id]);
        } else {
            const parent = nodeMap[node.father_node_id];
            if (parent) {
                parent.children.push(nodeMap[node.node_id]);
            }
        }
    }

    return tree;
}

/**
 * @description Get variable type
 * @param {*} value
 * @returns
 */
function getType(value) {
    // Handle special case of null
    if (value === null) {
        return 'null';
    }

    // Handle basic types
    const type = typeof value;
    if (type !== 'object') {
        return type;
    }

    // Handle object types
    const toString = Object.prototype.toString.call(value);
    const typeString = toString.slice(8, -1).toLowerCase();

    // Special handling for some types
    if (typeString === 'object') {
        if (value.constructor) {
            return value.constructor.name.toLowerCase();
        }
    }

    return typeString;
}

/**
 * @description Generate script ID
 * @returns
 */
function generateScriptId() {
    const date = new Date();
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    const h = String(date.getHours()).padStart(2, '0');
    const min = String(date.getMinutes()).padStart(2, '0');
    const s = String(date.getSeconds()).padStart(2, '0');
    return `repro_script_${y}${m}${d}${h}${min}${s}`;
}


function getTextWidth(text) {
    var tempDiv = $('<div>').css({
        position: 'absolute',
        float: 'left',
        whiteSpace: 'nowrap',
        visibility: 'hidden',
        fontSize: $('#message_input').css('font-size') // Or other style properties
    }).text(text);
    $('body').append(tempDiv);
    var width = tempDiv.width();
    tempDiv.remove(); // Remove temporary element, clean up DOM
    return width;
}
