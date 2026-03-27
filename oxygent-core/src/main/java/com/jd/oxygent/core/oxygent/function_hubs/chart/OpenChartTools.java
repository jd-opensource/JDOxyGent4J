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
import com.jd.oxygent.core.oxygent.oxy.function_tools.FunctionHub;
import com.jd.oxygent.core.oxygent.tools.ParamMetaAuto;
import com.jd.oxygent.core.oxygent.tools.Tool;
import java.awt.*;
import java.io.File;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * 流程图打开工具类
 * 在浏览器中打开生成的流程图 HTML 文件
 */
public class OpenChartTools extends FunctionHub {

    private static final Logger logger = Logger.getLogger(OpenChartTools.class.getName());

    public OpenChartTools() {
        super("open_chart_tools");
        this.setDesc("在浏览器中打开生成的流程图 HTML 文件");
    }

    @Tool(
            name = "openHtmlChart",
            description = "在浏览器中打开生成的流程图 HTML 文件",
            paramMetas = {
                    @ParamMetaAuto(name = "filePath", type = "String", description = "HTML 文件路径")
            }
    )
    public String openHtmlChart(String filePath) {
        try {
            // 确保使用绝对路径
            String absolutePath = filePath;
            if (!Paths.get(filePath).isAbsolute()) {
                String currentDir = System.getProperty("user.dir");

                // 如果当前在 examples/other 目录，并且文件路径以 output/开头，需要调整路径
                if ((currentDir.endsWith("examples/other") || currentDir.endsWith("examples\\other"))
                        && filePath.startsWith("output/")) {
                    // 回到项目根目录来解析路径
                    String projectRoot = Paths.get(currentDir).getParent().getParent().toString();
                    absolutePath = Paths.get(projectRoot, filePath).toString();
                } else {
                    absolutePath = Paths.get(filePath).toAbsolutePath().toString();
                }
            }

            // 检查文件是否存在
            File file = new File(absolutePath);
            if (!file.exists()) {
                return "错误：文件不存在：" + absolutePath;
            }

            logger.info("正在打开文件：" + absolutePath);

            // 在浏览器中打开
            Desktop desktop = Desktop.getDesktop();
            desktop.browse(file.toURI());

            return "已在浏览器中打开流程图：" + absolutePath;

        } catch (Exception e) {
            logger.severe("打开浏览器时出错：" + e.getMessage());
            return "打开浏览器时出错：" + e.getMessage() + "\n请手动打开生成的文件：" + filePath;
        }
    }
}
