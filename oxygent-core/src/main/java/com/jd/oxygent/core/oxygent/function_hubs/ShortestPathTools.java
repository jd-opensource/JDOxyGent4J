package com.jd.oxygent.core.oxygent.function_hubs;

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
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jgrapht.Graph;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

/**
 * 最短路径工具类
 * 使用图算法解决城市间的最短路径问题
 */
public class ShortestPathTools extends FunctionHub {

    private static final Logger logger = Logger.getLogger(ShortestPathTools.class.getName());
    //全局数据存储（column_data）
    private final Map<String, List<Object>> columnData = new HashMap<>();

    public ShortestPathTools() {
        super("shortest_path_tools");
        this.setDesc("A tool that can calculate the shortest path between different points");
    }

    @Tool(
            name = "infoUpdate",
            description = "Update city and distance information based on Excel.",
            paramMetas = {
                    @ParamMetaAuto(name = "filePath", type = "String", description = "Excel 文件路径"),
                    @ParamMetaAuto(name = "sheetName", type = "int", description = "工作表索引")
            }
    )
    public String infoUpdate(String filePath, int sheetName) {
        try {
            if (!Files.exists(Paths.get(filePath))) {
                return "File Not Found: " + filePath;
            }

            Workbook workbook = new XSSFWorkbook(new FileInputStream(filePath));
            Sheet sheet = workbook.getSheetAt(sheetName);

            // 读取表头
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                workbook.close();
                return "File is Empty";
            }

            // 获取列名
            List<String> columnNames = new ArrayList<>();
            for (Cell cell : headerRow) {
                columnNames.add(cell.getStringCellValue());
            }

            // 清空旧数据
            columnData.clear();

            // 读取每一列的数据
            for (String columnName : columnNames) {
                List<Object> columnValues = new ArrayList<>();

                // 从第 1 行开始读取（跳过表头）
                for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) continue;

                    // 找到列索引
                    int columnIndex = -1;
                    for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                        Cell headerCell = headerRow.getCell(i);
                        if (headerCell != null && headerCell.getStringCellValue().equals(columnName)) {
                            columnIndex = i;
                            break;
                        }
                    }

                    if (columnIndex >= 0) {
                        Cell cell = row.getCell(columnIndex);
                        if (cell != null) {
                            Object value = getCellValue(cell);
                            if (value != null && !value.toString().trim().isEmpty()) {
                                columnValues.add(value);
                            }
                        }
                    }
                }

                if (!columnValues.isEmpty()) {
                    columnData.put(columnName, columnValues);
                }
            }

            workbook.close();

            if (!columnData.isEmpty()) {
                return "File Read Success!";
            }

            return "File is Empty";

        } catch (Exception e) {
            logger.severe("读取 Excel 文件失败：" + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(
            name = "shortestPath",
            description = "A tool that can calculate the shortest path between different points",
            paramMetas = {
                    @ParamMetaAuto(name = "startCity", type = "String", description = "起点城市"),
                    @ParamMetaAuto(name = "endCity", type = "String", description = "终点城市")
            }
    )
    public String shortestPath(String startCity, String endCity) {
        try {
            if (columnData.isEmpty()) {
                return createErrorResponse("请先调用 infoUpdate 加载城市数据");
            }

            List<Object> citiesObj = columnData.get("cities");
            List<Object> startCitiesObj = columnData.get("start_cities");
            List<Object> endCitiesObj = columnData.get("end_cities");
            List<Object> distancesObj = columnData.get("distances");

            if (citiesObj == null || startCitiesObj == null ||
                    endCitiesObj == null || distancesObj == null) {
                return createErrorResponse("数据不完整，缺少必要的列信息");
            }

            // 转换为字符串列表
            List<String> cities = new ArrayList<>();
            for (Object obj : citiesObj) {
                cities.add(obj.toString());
            }

            List<String> startCities = new ArrayList<>();
            for (Object obj : startCitiesObj) {
                startCities.add(obj.toString());
            }

            List<String> endCities = new ArrayList<>();
            for (Object obj : endCitiesObj) {
                endCities.add(obj.toString());
            }

            List<Double> distances = new ArrayList<>();
            for (Object obj : distancesObj) {
                try {
                    distances.add(Double.parseDouble(obj.toString()));
                } catch (NumberFormatException e) {
                    logger.warning("距离值无效：" + obj);
                }
            }

            // 创建城市到索引的映射
            Map<String, Integer> cityToIndex = new HashMap<>();
            for (int i = 0; i < cities.size(); i++) {
                cityToIndex.put(cities.get(i), i);
            }

            // 验证起点和终点是否存在
            if (!cityToIndex.containsKey(startCity)) {
                return createErrorResponse("起点城市不存在：" + startCity);
            }
            if (!cityToIndex.containsKey(endCity)) {
                return createErrorResponse("终点城市不存在：" + endCity);
            }

            // 创建加权图
            Graph<String, DefaultWeightedEdge> graph =
                    new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

            // 添加所有城市节点
            cities.forEach(graph::addVertex);

            // 添加边（双向边，因为城市之间的道路是双向的）
            for (int i = 0; i < startCities.size(); i++) {
                String from = startCities.get(i);
                String to = endCities.get(i);
                double distance = distances.get(i);

                // 添加正向边
                if (!graph.containsVertex(from) || !graph.containsVertex(to)) {
                    continue; // 跳过无效边
                }

                graph.addEdge(from, to);
                DefaultWeightedEdge edge = graph.getEdge(from, to);
                graph.setEdgeWeight(edge, distance);

                // 添加反向边
                graph.addEdge(to, from);
                DefaultWeightedEdge reverseEdge = graph.getEdge(to, from);
                graph.setEdgeWeight(reverseEdge, distance);
            }

            // 使用 Dijkstra 算法计算最短路径
            long startTime = System.nanoTime();
            DijkstraShortestPath<String, DefaultWeightedEdge> dijkstra =
                    new DijkstraShortestPath<>(graph);
            var pathResult = dijkstra.getPath(startCity, endCity);
            long endTime = System.nanoTime();

            // 构建结果
            Map<String, Object> result = new LinkedHashMap<>();

            if (pathResult.getWeight() == Double.POSITIVE_INFINITY) {
                result.put("status", "not_optimal");
                result.put("message", "无法找到从 " + startCity + " 到 " + endCity + " 的路径");
            } else {
                result.put("status", "optimal");
                result.put("distance", pathResult.getWeight());
                result.put("solve_time", (endTime - startTime) / 1_000_000_000.0); // 转换为秒

                List<String> vertices = pathResult.getVertexList();

                // 构建路径段
                List<List<Integer>> path = new ArrayList<>();
                List<String> pathCities = new ArrayList<>();

                for (int i = 0; i < vertices.size() - 1; i++) {
                    String from = vertices.get(i);
                    String to = vertices.get(i + 1);
                    path.add(Arrays.asList(cityToIndex.get(from), cityToIndex.get(to)));
                    pathCities.add(from + " -> " + to);
                }

                result.put("path", path);
                result.put("path_cities", pathCities);

                // 可视化城市路径（简化版本，输出到日志）
                visualizeCityPath(cities, startCities, endCities, distances, vertices);
            }

            return JsonUtils.toJSONString(result);

        } catch (Exception e) {
            logger.severe("最短路径计算失败：" + e.getMessage());
            return createErrorResponse("计算失败：" + e.getMessage());
        }
    }

    /**
     * 可视化城市图和最短路径
     * Java版本简化为输出到日志，也可以使用图形库实现真正的可视化
     */
    private void visualizeCityPath(List<String> cities, List<String> startCities,
                                   List<String> endCities, List<Double> distances,
                                   List<String> pathVertices) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("\n=== 最短路径可视化 ===\n");
            sb.append("路径：").append(String.join(" → ", pathVertices)).append("\n");
            sb.append("总距离：");

            double totalDistance = 0;
            for (int i = 0; i < pathVertices.size() - 1; i++) {
                String from = pathVertices.get(i);
                String to = pathVertices.get(i + 1);

                // 查找边的权重
                for (int j = 0; j < startCities.size(); j++) {
                    if ((startCities.get(j).equals(from) && endCities.get(j).equals(to)) ||
                            (endCities.get(j).equals(from) && startCities.get(j).equals(to))) {
                        double dist = distances.get(j);
                        totalDistance += dist;
                        break;
                    }
                }
            }

            sb.append(totalDistance).append("\n");
            sb.append("=====================\n");

            logger.info(sb.toString());

            // 注意：如果需要生成图像文件，可以使用以下库：
            // 1. GraphStream - 图的可视化和布局
            // 2. JFreeChart - 图表绘制
            // 3. 输出 GeoJSON 给前端用 D3.js/ECharts 渲染
        } catch (Exception e) {
            logger.warning("可视化过程中出错：" + e.getMessage());
        }
    }

    /**
     * 辅助方法：获取单元格的值
     */
    private Object getCellValue(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                } else {
                    return cell.getNumericCellValue();
                }
            case BOOLEAN:
                return cell.getBooleanCellValue();
            default:
                return null;
        }
    }

    /**
     * 辅助方法：创建错误响应
     */
    private String createErrorResponse(String error) {
        try {
            Map<String, Object> errorResp = new LinkedHashMap<>();
            errorResp.put("error", error);
            return JsonUtils.toJSONString(errorResp);
        } catch (Exception e) {
            return "{\"error\": \"" + error + "\"}";
        }
    }
}
