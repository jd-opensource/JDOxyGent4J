package com.jd.oxygent.core.oxygent.function_hubs.chart;

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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * 静态文件工具类
 * 用于创建和管理静态文件（HTML、CSS、JavaScript）
 */
public class ChartStaticFilesUtils {

    private static final Logger logger = Logger.getLogger(ChartStaticFilesUtils.class.getName());

    /**
     * 创建必要的静态文件
     *
     * @param basePath 基础路径
     * @return 是否成功
     */
    public static boolean createStaticFiles(String basePath) {
        try {
            // 创建目录结构
            Path baseDir = Paths.get(basePath);
            Path webDir = baseDir.resolve("web");
            Path cssDir = webDir.resolve("css");
            Path jsDir = webDir.resolve("js");

            Files.createDirectories(cssDir);
            Files.createDirectories(jsDir);

            // 创建 index.html
            String indexHtml = "<!DOCTYPE html>\n" +
                    "<html lang=\"zh-CN\">\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                    "    <title>Mermaid 流程图生成器</title>\n" +
                    "    <link rel=\"stylesheet\" href=\"css/style.css\">\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <div class=\"container\">\n" +
                    "        <h1>Mermaid 流程图生成器</h1>\n" +
                    "        \n" +
                    "        <div class=\"input-container\">\n" +
                    "            <h2>输入流程图描述</h2>\n" +
                    "            <textarea id=\"description\" placeholder=\"请输入流程图描述，例如：生成一个软件开发流程图，包括需求分析、设计、编码、测试和部署阶段。\"></textarea>\n" +
                    "            <button id=\"generate-btn\">生成流程图</button>\n" +
                    "        </div>\n" +
                    "        \n" +
                    "        <div class=\"result-container\" id=\"result\">\n" +
                    "            <!-- 结果将在这里显示 -->\n" +
                    "        </div>\n" +
                    "    </div>\n" +
                    "    \n" +
                    "    <script src=\"js/app.js\"></script>\n" +
                    "</body>\n" +
                    "</html>";

            Files.writeString(webDir.resolve("index.html"), indexHtml);
            logger.info("已创建：" + webDir.resolve("index.html"));

            // 创建 style.css
            String styleCss = "body {\n" +
                    "    font-family: Arial, sans-serif;\n" +
                    "    margin: 0;\n" +
                    "    padding: 20px;\n" +
                    "    background-color: #f5f5f5;\n" +
                    "}\n" +
                    "\n" +
                    ".container {\n" +
                    "    max-width: 800px;\n" +
                    "    margin: 0 auto;\n" +
                    "    background-color: white;\n" +
                    "    padding: 20px;\n" +
                    "    border-radius: 8px;\n" +
                    "    box-shadow: 0 2px 10px rgba(0,0,0,0.1);\n" +
                    "}\n" +
                    "\n" +
                    "h1, h2 {\n" +
                    "    color: #333;\n" +
                    "}\n" +
                    "\n" +
                    ".input-container {\n" +
                    "    margin-bottom: 20px;\n" +
                    "}\n" +
                    "\n" +
                    "textarea {\n" +
                    "    width: 100%;\n" +
                    "    height: 150px;\n" +
                    "    padding: 10px;\n" +
                    "    border: 1px solid #ddd;\n" +
                    "    border-radius: 4px;\n" +
                    "    font-size: 14px;\n" +
                    "    margin-bottom: 10px;\n" +
                    "}\n" +
                    "\n" +
                    "button {\n" +
                    "    background-color: #4CAF50;\n" +
                    "    color: white;\n" +
                    "    padding: 10px 15px;\n" +
                    "    border: none;\n" +
                    "    border-radius: 4px;\n" +
                    "    cursor: pointer;\n" +
                    "    font-size: 14px;\n" +
                    "}\n" +
                    "\n" +
                    "button:hover {\n" +
                    "    background-color: #45a049;\n" +
                    "}\n" +
                    "\n" +
                    ".result-container {\n" +
                    "    padding: 15px;\n" +
                    "    border: 1px solid #ddd;\n" +
                    "    border-radius: 4px;\n" +
                    "    background-color: #f9f9f9;\n" +
                    "    min-height: 100px;\n" +
                    "}";

            Files.writeString(cssDir.resolve("style.css"), styleCss);
            logger.info("已创建：" + cssDir.resolve("style.css"));

            // 创建 app.js
            String appJs = "document.addEventListener('DOMContentLoaded', function() {\n" +
                    "    const generateBtn = document.getElementById('generate-btn');\n" +
                    "    const descriptionInput = document.getElementById('description');\n" +
                    "    const resultContainer = document.getElementById('result');\n" +
                    "    \n" +
                    "    generateBtn.addEventListener('click', function() {\n" +
                    "        const description = descriptionInput.value.trim();\n" +
                    "        \n" +
                    "        if (!description) {\n" +
                    "            alert('请输入流程图描述');\n" +
                    "            return;\n" +
                    "        }\n" +
                    "        \n" +
                    "        // 显示加载状态\n" +
                    "        resultContainer.innerHTML = '<p>正在生成流程图，请稍候...</p>';\n" +
                    "        \n" +
                    "        // 发送请求到后端\n" +
                    "        fetch('/api/generate', {\n" +
                    "            method: 'POST',\n" +
                    "            headers: {\n" +
                    "                'Content-Type': 'application/json',\n" +
                    "            },\n" +
                    "            body: JSON.stringify({ description: description }),\n" +
                    "        })\n" +
                    "        .then(response => response.json())\n" +
                    "        .then(data => {\n" +
                    "            if (data.success) {\n" +
                    "                resultContainer.innerHTML = `\n" +
                    "                    <p>流程图已生成！</p>\n" +
                    "                    <p>文件路径：${data.file_path}</p>\n" +
                    "                    <p>正在打开浏览器...</p>\n" +
                    "                `;\n" +
                    "            } else {\n" +
                    "                resultContainer.innerHTML = `<p>生成失败：${data.error}</p>`;\n" +
                    "            }\n" +
                    "        })\n" +
                    "        .catch(error => {\n" +
                    "            console.error('请求出错:', error);\n" +
                    "            resultContainer.innerHTML = '<p>请求出错，请查看控制台获取详细信息</p>';\n" +
                    "        });\n" +
                    "    });\n" +
                    "});";

            Files.writeString(jsDir.resolve("app.js"), appJs);
            logger.info("已创建：" + jsDir.resolve("app.js"));

            return true;

        } catch (IOException e) {
            logger.severe("创建静态文件时出错：" + e.getMessage());
            return false;
        }
    }
}
