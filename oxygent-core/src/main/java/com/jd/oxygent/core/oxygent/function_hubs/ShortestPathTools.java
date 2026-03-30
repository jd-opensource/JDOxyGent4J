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
 * Shortest path tools class
 * Uses graph algorithms to solve shortest path problems between cities
 */
public class ShortestPathTools extends FunctionHub {

    private static final Logger logger = Logger.getLogger(ShortestPathTools.class.getName());
    // Global data storage (column_data)
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

            // Read header
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                workbook.close();
                return "File is Empty";
            }

            // Get column names
            List<String> columnNames = new ArrayList<>();
            for (Cell cell : headerRow) {
                columnNames.add(cell.getStringCellValue());
            }

            // Clear old data
            columnData.clear();

            // Read data for each column
            for (String columnName : columnNames) {
                List<Object> columnValues = new ArrayList<>();

                // Start reading from row 1 (skip header)
                for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) continue;

                    // Find column index
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
            logger.severe("Failed to read Excel file: " + e.getMessage());
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
                return createErrorResponse("Please call infoUpdate first to load city data");
            }

            List<Object> citiesObj = columnData.get("cities");
            List<Object> startCitiesObj = columnData.get("start_cities");
            List<Object> endCitiesObj = columnData.get("end_cities");
            List<Object> distancesObj = columnData.get("distances");

            if (citiesObj == null || startCitiesObj == null ||
                    endCitiesObj == null || distancesObj == null) {
                return createErrorResponse("Data incomplete, missing required column information");
            }

            // Convert to string list
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
                    logger.warning("Invalid distance value: " + obj);
                }
            }

            // Create city to index mapping
            Map<String, Integer> cityToIndex = new HashMap<>();
            for (int i = 0; i < cities.size(); i++) {
                cityToIndex.put(cities.get(i), i);
            }

            // Validate if start and end cities exist
            if (!cityToIndex.containsKey(startCity)) {
                return createErrorResponse("Start city does not exist: " + startCity);
            }
            if (!cityToIndex.containsKey(endCity)) {
                return createErrorResponse("End city does not exist: " + endCity);
            }

            // Create weighted graph
            Graph<String, DefaultWeightedEdge> graph =
                    new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

            // Add all city nodes
            cities.forEach(graph::addVertex);

            // Add edges (bidirectional edges, since roads between cities are bidirectional)
            for (int i = 0; i < startCities.size(); i++) {
                String from = startCities.get(i);
                String to = endCities.get(i);
                double distance = distances.get(i);

                // Add forward edge
                if (!graph.containsVertex(from) || !graph.containsVertex(to)) {
                // Skip invalid edge
                }

                graph.addEdge(from, to);
                DefaultWeightedEdge edge = graph.getEdge(from, to);
                graph.setEdgeWeight(edge, distance);

                // Add reverse edge
                graph.addEdge(to, from);
                DefaultWeightedEdge reverseEdge = graph.getEdge(to, from);
                graph.setEdgeWeight(reverseEdge, distance);
            }

            // Use Dijkstra algorithm to calculate shortest path
            long startTime = System.nanoTime();
            DijkstraShortestPath<String, DefaultWeightedEdge> dijkstra =
                    new DijkstraShortestPath<>(graph);
            var pathResult = dijkstra.getPath(startCity, endCity);
            long endTime = System.nanoTime();

            // Build result
            Map<String, Object> result = new LinkedHashMap<>();

            if (pathResult.getWeight() == Double.POSITIVE_INFINITY) {
                result.put("status", "not_optimal");
                result.put("message", "Cannot find path from " + startCity + " to " + endCity);
            } else {
                result.put("status", "optimal");
                result.put("distance", pathResult.getWeight());
                result.put("solve_time", (endTime - startTime) / 1_000_000_000.0); // Convert to seconds

                List<String> vertices = pathResult.getVertexList();

                // Build path segments
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

                // Visualize city path (simplified version, output to log)
                visualizeCityPath(cities, startCities, endCities, distances, vertices);
            }

            return JsonUtils.toJSONString(result);

        } catch (Exception e) {
            logger.severe("Shortest path calculation failed: " + e.getMessage());
            return createErrorResponse("Calculation failed: " + e.getMessage());
        }
    }

    /**
     * Visualize city graph and shortest path
     * Java version simplified to output to log, can also use graphics library for real visualization
     */
    private void visualizeCityPath(List<String> cities, List<String> startCities,
                                   List<String> endCities, List<Double> distances,
                                   List<String> pathVertices) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("\n=== Shortest Path Visualization ===\n");
            sb.append("Path: ").append(String.join(" → ", pathVertices)).append("\n");
            sb.append("Total Distance: ");

            double totalDistance = 0;
            for (int i = 0; i < pathVertices.size() - 1; i++) {
                String from = pathVertices.get(i);
                String to = pathVertices.get(i + 1);

                // Find edge weight
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

            // Note: If you need to generate image files, you can use the following libraries:
            // 1. GraphStream - graph visualization and layout
            // 2. JFreeChart - chart drawing
            // 3. Output GeoJSON for frontend rendering with D3.js/ECharts
        } catch (Exception e) {
            logger.warning("Error during visualization: " + e.getMessage());
        }
    }

    /**
     * Helper method: Get cell value
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
     * Helper method: Create error response
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
