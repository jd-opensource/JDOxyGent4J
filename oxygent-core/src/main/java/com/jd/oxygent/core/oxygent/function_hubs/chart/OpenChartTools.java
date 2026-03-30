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
 * Flowchart opening tools class
 * Opens generated flowchart HTML files in the browser
 */
public class OpenChartTools extends FunctionHub {

    private static final Logger logger = Logger.getLogger(OpenChartTools.class.getName());

    public OpenChartTools() {
        super("open_chart_tools");
        this.setDesc("Open the generated flowchart HTML file in the browser");
    }

    @Tool(
            name = "openHtmlChart",
            description = "Open the generated flowchart HTML file in the browser",
            paramMetas = {
                    @ParamMetaAuto(name = "filePath", type = "String", description = "HTML file path")
            }
    )
    public String openHtmlChart(String filePath) {
        try {
            // Ensure absolute path is used
            String absolutePath = filePath;
            if (!Paths.get(filePath).isAbsolute()) {
                String currentDir = System.getProperty("user.dir");

                // If currently in examples/other directory and file path starts with output/, need to adjust path
                if ((currentDir.endsWith("examples/other") || currentDir.endsWith("examples\\other"))
                        && filePath.startsWith("output/")) {
                    // Go back to project root directory to resolve path
                    String projectRoot = Paths.get(currentDir).getParent().getParent().toString();
                    absolutePath = Paths.get(projectRoot, filePath).toString();
                } else {
                    absolutePath = Paths.get(filePath).toAbsolutePath().toString();
                }
            }

            // Check if file exists
            File file = new File(absolutePath);
            if (!file.exists()) {
                return "Error: File does not exist: " + absolutePath;
            }

            logger.info("Opening file: " + absolutePath);

            // Open in browser
            Desktop desktop = Desktop.getDesktop();
            desktop.browse(file.toURI());

            return "Flowchart opened in browser: " + absolutePath;

        } catch (Exception e) {
            logger.severe("Error opening browser: " + e.getMessage());
            return "Error opening browser: " + e.getMessage() + "\nPlease manually open the generated file: " + filePath;
        }
    }
}
