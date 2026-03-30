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
package com.jd.oxygent.core.oxygent.function_hubs;

import com.jd.oxygent.core.oxygent.oxy.function_tools.FunctionHub;
import com.jd.oxygent.core.oxygent.tools.ParamMetaAuto;
import com.jd.oxygent.core.oxygent.tools.Tool;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SqlTools extends FunctionHub {

    private static final Logger logger = Logger.getLogger(SqlTools.class.getName());
    private Connection dbConnection = null;

    public SqlTools() {
        super("sql_tools");
        this.setDesc("A tool that can interact with SQL databases.");
        this.initConfig();
    }

    private void initConfig() {
        // Get database URL from environment variable
        String dbUrl = System.getenv("SQL_TOOLS_DB_URL");
        String user = System.getenv("SQL_TOOLS_DB_USER");
        String password = System.getenv("SQL_TOOLS_DB_PASSWORD");
        if (dbUrl == null || dbUrl.isEmpty()) {
            logger.warning("Could not find the db_url from environment, SQL tools will be disabled");
            return;
        }

        try {
            // Establish database connection
            dbConnection = DriverManager.getConnection(dbUrl, user, password);
            logger.info("Database connection established successfully");
        } catch (SQLException e) {
            logger.severe("Error establishing database connection: " + e.getMessage());
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    @Tool(
            name = "list_tables",
            description = "Use this function to get a list of table names in the database"
    )
    public String listTables() {
        if (dbConnection == null) {
            String errorMsg = "Database connection is not initialized. Please check SQL_TOOLS_DB_URL environment variable.";
            logger.warning(errorMsg);
            return JsonUtils.toJSONString(Map.of("error", errorMsg));
        }
        
        try {
            List<String> tableNames = new ArrayList<>();
            DatabaseMetaData metaData = dbConnection.getMetaData();
            ResultSet resultSet = metaData.getTables(null, null, "%", new String[]{"TABLE"});

            while (resultSet.next()) {
                tableNames.add(resultSet.getString("TABLE_NAME"));
            }
            resultSet.close();

            logger.info("Retrieved tables: " + tableNames);
            return JsonUtils.toJSONString(tableNames);
        } catch (SQLException e) {
            String errorMsg = "Error getting tables: " + e.getMessage();
            logger.severe(errorMsg);
            return JsonUtils.toJSONString(Map.of("error", errorMsg));
        }
    }

    @Tool(
            name = "run_sql",
            description = "run a sql query and return the result",
            paramMetas = {
                    @ParamMetaAuto(
                            name = "sql",
                            type = "String",
                            description = "The SQL query to execute"
                    ),
                    @ParamMetaAuto(
                            name = "limit",
                            type = "Integer",
                            description = "Optional limit on the number of results",
                            defaultValue = "null"
                    )
            }
    )
    public String runSql(String sql, Integer limit) {
        if (dbConnection == null) {
            String errorMsg = "Database connection is not initialized. Please check SQL_TOOLS_DB_URL environment variable.";
            logger.warning(errorMsg);
            return JsonUtils.toJSONString(Map.of("error", errorMsg));
        }
        
        logger.info("Running SQL: " + sql);

        try (Statement statement = dbConnection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql);
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            List<Map<String, Object>> rows = new ArrayList<>();
            int rowCount = 0;

            while (resultSet.next()) {
                if (limit != null && rowCount >= limit) {
                    break;
                }

                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = resultSet.getObject(i);
                    row.put(columnName, value);
                }
                rows.add(row);
                rowCount++;
            }

            resultSet.close();
            return JsonUtils.toJSONString(rows);
        } catch (SQLException e) {
            String errorMsg = "Error running query: " + e.getMessage();
            logger.severe(errorMsg);
            return JsonUtils.toJSONString(Map.of("error", errorMsg));
        }
    }

    @Tool(
            name = "describe_tables",
            description = "describe the given table",
            paramMetas = {
                    @ParamMetaAuto(
                            name = "table_name",
                            type = "String",
                            description = "The name of the table to describe"
                    )
            }
    )
    public String describeTables(String tableName) {
        if (dbConnection == null) {
            String errorMsg = "Database connection is not initialized. Please check SQL_TOOLS_DB_URL environment variable.";
            logger.warning(errorMsg);
            return JsonUtils.toJSONString(Map.of("error", errorMsg));
        }
        
        try {
            logger.info("Describing table: " + tableName);
            List<Map<String, Object>> tableSchema = new ArrayList<>();
            DatabaseMetaData metaData = dbConnection.getMetaData();
            ResultSet resultSet = metaData.getColumns(null, null, tableName, "%");

            while (resultSet.next()) {
                Map<String, Object> column = new HashMap<>();
                column.put("name", resultSet.getString("COLUMN_NAME"));
                column.put("type", resultSet.getString("TYPE_NAME"));
                column.put("nullable", resultSet.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                tableSchema.add(column);
            }
            resultSet.close();

            return JsonUtils.toJSONString(tableSchema);
        } catch (SQLException e) {
            String errorMsg = "Error getting table schema: " + e.getMessage();
            logger.severe(errorMsg);
            return JsonUtils.toJSONString(Map.of("error", errorMsg));
        }
    }

    // Close database connection when the object is finalized
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (dbConnection != null && !dbConnection.isClosed()) {
            try {
                dbConnection.close();
                logger.info("Database connection closed");
            } catch (SQLException e) {
                logger.severe("Error closing database connection: " + e.getMessage());
            }
        }
    }
}
