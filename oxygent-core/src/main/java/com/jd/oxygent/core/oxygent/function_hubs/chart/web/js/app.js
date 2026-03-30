document.addEventListener('DOMContentLoaded', function() {
    const generateBtn = document.getElementById('generate-btn');
    const descriptionInput = document.getElementById('description');
    const resultContainer = document.getElementById('result');
    
    generateBtn.addEventListener('click', function() {
        const description = descriptionInput.value.trim();
        
        if (!description) {
            alert('请输入流程图描述');
            return;
        }
        
        // 显示加载状态
        resultContainer.innerHTML = '<p>正在生成流程图，请稍候...</p>';
        
        // 发送请求到后端
        fetch('/api/generate', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ description: description }),
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                resultContainer.innerHTML = `
                    <p>流程图已生成！</p>
                    <p>文件路径: ${data.file_path}</p>
                    <p>正在打开浏览器...</p>
                `;
            } else {
                resultContainer.innerHTML = `<p>生成失败: ${data.error}</p>`;
            }
        })
        .catch(error => {
            console.error('请求出错:', error);
            resultContainer.innerHTML = '<p>请求出错，请查看控制台获取详细信息</p>';
        });
    });
});