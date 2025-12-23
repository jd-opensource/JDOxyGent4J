/**
 * Mermaid SDK for Gantt Chart Generation
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */

function formatTimeWithMilliseconds(timeString) {
    // Split date and time parts
    const [datePart, timePart] = timeString.split(' ');

    // Split time part
    const [hms, fractions] = timePart.split('.');

    // Extract first 3 digits of milliseconds (pad with 0 if less than 3 digits)
    const milliseconds = fractions ? fractions.substring(0, 3).padEnd(3, '0') : '000';

    // Combine into new format
    return `${datePart} ${hms}.${milliseconds}`;
}

function unionName(data) {
    return [...new Set(data)]
}


function generateGrant(taskList) {
    let ganttCode = `gantt \n`;
    ganttCode += `dateFormat  YYYY-MM-DD HH:mm:ss.SSS \n`;
    // Add sections
    // ganttCode += `    section Project Phases\n`;
    // Add tasks
    taskList.forEach(task => {
        if (task.sectionName) {
            ganttCode += `section ${task.sectionName}\n`;
        }
        if (task.data) {
            task.data.forEach(_task => {
                ganttCode += `    ${_task.name} :${_task.id}, ${_task.start}, ${_task.end}\n`;
                ganttCode += `    click ${_task.id} call renderGanttClick(${_task.id})\n`;
                ganttCode += '\n';
            })
        }

    });
    return ganttCode;
}


function renderGantt(nodesDatas, containerId) {

    const callerArray = nodesDatas.map(({caller}) => caller);
    const unionNamecallerArray = unionName(callerArray);

    const transformData = nodesDatas.map(_data => {
        return {
            ..._data,
            id: _data.node_id,
            next: _data.child_node_ids.filter(i => i),
            name: _data.callee,
            start: formatTimeWithMilliseconds(_data.create_time),
            end: formatTimeWithMilliseconds(_data.update_time),
        }
    })

    const transformDataSection = unionNamecallerArray.map((_caller) => {
        return {
            sectionName: _caller,
            data: transformData.filter(i => i.caller === _caller),
        }
    })

    const code = generateGrant(transformDataSection);
    $('#flowchart-container-gantt').html('').removeAttr('data-processed');
    const container = document.getElementById(containerId);
    container.innerHTML = code;
    mermaid.run({
        querySelector: `#${containerId}`
    });
}








