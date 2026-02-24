package com.jd.oxygent.oxybank.core.storer.vectormanager;

import com.jd.oxygent.core.Config;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Vearch空间管理器
 * 
 * 使用官方pyvearch SDK进行空间检查、验证、删除和创建操作
 */
@Slf4j
@Component
public class VearchManager {

    private VearchClient client;

    public VearchManager() {
        String host = "";
        String token = ""; // fixme
        this.client = new VearchClient(host, token);
        
        try {
            checkConnection();
            log.info("Successfully connected to Vearch server {}", host);
        } catch (Exception e) {
            throw new RuntimeException("Unable to connect to Vearch server", e);
        }
    }

    /**
     * 构造函数
     * 
     * @param vearchClient Vearch客户端实例
     */
    public VearchManager(VearchClient vearchClient) {
        this.client = vearchClient;
        
        try {
            checkConnection();
            log.info("Successfully connected to Vearch server with provided client");
        } catch (Exception e) {
            throw new RuntimeException("Unable to connect to Vearch server", e);
        }
    }

    /**
     * 检查Vearch连接
     */
    private void checkConnection() {
        try {
            client.listDatabases();
        } catch (Exception e) {
            throw new RuntimeException("Unable to connect to Vearch server", e);
        }
    }

    /**
     * 检查数据库是否存在
     * 
     * @param dbName 数据库名称
     * @return 数据库是否存在
     */
    public boolean databaseExists(String dbName) {
        try {
            List<DatabaseInfo> databases = client.listDatabases();
            for (DatabaseInfo db : databases) {
                if (db.getName().equals(dbName)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Error checking if database exists", e);
            return false;
        }
    }

    /**
     * 检查空间是否存在
     * 
     * @param dbName 数据库名称
     * @param spaceName 空间名称
     * @return 空间是否存在
     */
    public boolean spaceExists(String dbName, String spaceName) {
        try {
            List<SpaceInfo> spaces = client.listSpaces(dbName);
            for (SpaceInfo space : spaces) {
                if (space.getName().equals(spaceName)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Error checking if space exists", e);
            return false;
        }
    }

    /**
     * 获取空间结构
     * 
     * @param dbName 数据库名称
     * @param spaceName 空间名称
     * @return 空间结构
     */
    public Map<String, Object> getSpaceStructure(String dbName, String spaceName) {
        try {
            SpaceInfo spaceInfo = client.database(dbName).space(spaceName).describe();
            return spaceInfo.toMap();
        } catch (Exception e) {
            try {
                SpaceSchema spaceSchema = client.database(dbName).space(spaceName).exist();
                return spaceSchema.toMap();
            } catch (Exception e2) {
                throw new IllegalArgumentException(
                    "Failed to get space structure: " + e.getMessage() + 
                    ", backup method also failed: " + e2.getMessage(), e2);
            }
        }
    }

    /**
     * 验证空间结构是否符合预期
     * 
     * @param dbName 数据库名称
     * @param spaceName 空间名称
     * @param expectedStructure 预期的空间结构
     * @return 结构是否匹配
     */
    public boolean validateSpaceStructure(String dbName, String spaceName, Map<String, Object> expectedStructure) {
        if (!spaceExists(dbName, spaceName)) {
            log.warn("Space {} does not exist", spaceName);
            return false;
        }

        try {
            Map<String, Object> currentStructure = getSpaceStructure(dbName, spaceName);
            log.info("Current space {} structure: {}", spaceName, currentStructure);

            List<Map<String, Object>> currentFields = new ArrayList<>();
            if (currentStructure.containsKey("fields")) {
                currentFields = (List<Map<String, Object>>) currentStructure.get("fields");
            } else if (currentStructure.containsKey("field")) {
                currentFields = (List<Map<String, Object>>) currentStructure.get("field");
            } else {
                log.error("Unable to extract field information from space structure: {}", currentStructure);
                return false;
            }

            List<Map<String, Object>> expectedFields = (List<Map<String, Object>>) expectedStructure.getOrDefault("fields", new ArrayList<>());

            Map<String, Map<String, Object>> currentFieldMap = new HashMap<>();
            for (Map<String, Object> field : currentFields) {
                String fieldName = (String) field.getOrDefault("name", 
                    field.getOrDefault("field_name", field.get("field")));
                if (fieldName != null) {
                    currentFieldMap.put(fieldName, field);
                }
            }

            Map<String, Map<String, Object>> expectedFieldMap = new HashMap<>();
            for (Map<String, Object> field : expectedFields) {
                expectedFieldMap.put((String) field.get("name"), field);
            }

            log.info("Current fields: {}", currentFieldMap.keySet());
            log.info("Expected fields: {}", expectedFieldMap.keySet());

            List<String> missingFields = new ArrayList<>();
            for (Map.Entry<String, Map<String, Object>> entry : expectedFieldMap.entrySet()) {
                String fieldName = entry.getKey();
                if (!currentFieldMap.containsKey(fieldName)) {
                    missingFields.add(fieldName);
                    log.error("Field {} does not exist in current space", fieldName);
                }
            }

            if (!missingFields.isEmpty()) {
                log.error("Missing fields: {}", missingFields);
                return false;
            }

            for (Map.Entry<String, Map<String, Object>> entry : expectedFieldMap.entrySet()) {
                String fieldName = entry.getKey();
                Map<String, Object> expectedField = entry.getValue();
                Map<String, Object> currentField = currentFieldMap.get(fieldName);

                String currentType = null;
                Object typeObj = currentField.get("type");
                if (typeObj == null) {
                    typeObj = currentField.getOrDefault("data_type", currentField.get("dtype"));
                }
                if (typeObj != null) {
                    currentType = String.valueOf(typeObj);
                }

                String expectedType = (String) expectedField.get("type");

                Map<String, String> typeMapping = Map.of(
                    "STRING", "string",
                    "INTEGER", "integer",
                    "FLOAT", "float",
                    "VECTOR", "vector"
                );

                if (typeMapping.containsKey(currentType)) {
                    currentType = typeMapping.get(currentType);
                }

                if (!currentType.equals(expectedType)) {
                    log.error("Field {} type mismatch: expected {}, actual {}", fieldName, expectedType, currentType);
                    return false;
                }

                if ("vector".equals(expectedType)) {
                    int expectedDim = (int) expectedField.getOrDefault("dimension", 
                        expectedStructure.getOrDefault("dimension", 1024));
                    Object dimObj = currentField.get("dimension");
                    int currentDim = dimObj != null ? (int) dimObj : 
                        (int) currentField.getOrDefault("dim", 0);

                    if (currentDim != expectedDim) {
                        log.error("Vector field {} dimension mismatch: expected {}, actual {}", 
                            fieldName, expectedDim, currentDim);
                        return false;
                    }
                }
            }

            log.info("Space {} structure validation passed", spaceName);
            return true;
        } catch (Exception e) {
            log.error("Error validating space structure", e);
            return false;
        }
    }

    /**
     * 创建数据库
     * 
     * @param dbName 数据库名称
     * @return 是否创建成功
     */
    public boolean createDatabase(String dbName) {
        try {
            if (!databaseExists(dbName)) {
                VearchResult result = client.createDatabase(dbName);
                if (result.isSuccess()) {
                    log.info("Database {} created successfully", dbName);
                    return true;
                } else {
                    log.error("Failed to create database {}: {}", dbName, result.getMessage());
                    return false;
                }
            } else {
                log.info("Database {} already exists", dbName);
                return true;
            }
        } catch (Exception e) {
            log.error("Failed to create database {}", dbName, e);
            return false;
        }
    }

    /**
     * 删除空间
     * 
     * @param dbName 数据库名称
     * @param spaceName 空间名称
     * @return 是否删除成功
     */
    public boolean deleteSpace(String dbName, String spaceName) {
        try {
            if (spaceExists(dbName, spaceName)) {
                VearchResult result = client.dropSpace(dbName, spaceName);
                if (result.isSuccess()) {
                    log.info("Space {} deleted", spaceName);
                    return true;
                } else {
                    log.error("Failed to delete space {}: {}", spaceName, result.getMessage());
                    return false;
                }
            } else {
                log.info("Space {} does not exist, no need to delete", spaceName);
                return true;
            }
        } catch (Exception e) {
            log.error("Failed to delete space {}", spaceName, e);
            return false;
        }
    }

    /**
     * 创建空间
     * 
     * @param dbName 数据库名称
     * @param spaceName 空间名称
     * @param structure 空间结构
     * @return 是否创建成功
     */
    public boolean createSpace(String dbName, String spaceName, Map<String, Object> structure) {
        try {
            if (!createDatabase(dbName)) {
                return false;
            }

            if (spaceExists(dbName, spaceName)) {
                log.info("Space {} already exists, will delete first", spaceName);
                if (!deleteSpace(dbName, spaceName)) {
                    return false;
                }
            }

            List<Field> fields = new ArrayList<>();
            for (Map<String, Object> fieldConfig : (List<Map<String, Object>>) structure.getOrDefault("fields", new ArrayList<>())) {
                String fieldName = (String) fieldConfig.get("name");
                String fieldType = (String) fieldConfig.get("type");
                String desc = (String) fieldConfig.getOrDefault("desc", "");
                Boolean hasIndex = (Boolean) fieldConfig.getOrDefault("index", true);

                Field field = null;
                if ("string".equals(fieldType)) {
                    field = new Field(fieldName, DataType.STRING, desc, 
                        hasIndex ? new ScalarIndex(fieldName + "_idx") : null);
                } else if ("integer".equals(fieldType)) {
                    field = new Field(fieldName, DataType.INTEGER, desc, 
                        hasIndex ? new ScalarIndex(fieldName + "_idx") : null);
                } else if ("float".equals(fieldType)) {
                    field = new Field(fieldName, DataType.FLOAT, desc, 
                        hasIndex ? new ScalarIndex(fieldName + "_idx") : null);
                } else if ("vector".equals(fieldType)) {
                    int dimension = (int) fieldConfig.getOrDefault("dimension", 
                        structure.getOrDefault("dimension", 128));
                    MetricType metricType = "L2".equals(structure.get("metric_type")) ? 
                        MetricType.L2 : MetricType.INNER_PRODUCT;
                    field = new Field(fieldName, DataType.VECTOR, desc, 
                        new FlatIndex(fieldName + "_idx", metricType), dimension);
                } else {
                    log.warn("Unsupported field type: {}", fieldType);
                    continue;
                }

                if (field != null) {
                    fields.add(field);
                }
            }

            SpaceSchema spaceSchema = new SpaceSchema(spaceName, fields);

            VearchResult result = client.createSpace(dbName, spaceSchema);
            if (result.isSuccess()) {
                log.info("Space {} created successfully", spaceName);
                return true;
            } else {
                log.error("Failed to create space {}: {}", spaceName, result.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to create space {}", spaceName, e);
            return false;
        }
    }

    /**
     * 使用schema创建空间
     * 
     * @param dbName 数据库名称
     * @param schema 空间schema
     * @return 是否创建成功
     */
    public boolean createSpaceWithSchema(String dbName, SpaceSchema schema) {
        try {
            VearchResult result = client.createSpace(dbName, schema);
            if (result.isSuccess()) {
                log.info("Space {} created successfully", schema.getName());
                return true;
            } else {
                String errorMsg = "Failed to create space " + schema.getName() + ": " + result.getMessage();
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = "Failed to create space " + schema.getName() + ": " + e.getMessage();
            log.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }

    /**
     * 确保空间存在且结构符合预期
     * 
     * @param dbName 数据库名称
     * @param spaceName 空间名称
     * @param expectedStructure 预期的空间结构
     * @param forceRecreate 是否强制重新创建空间
     * @return 操作是否成功
     */
    public boolean ensureSpace(String dbName, String spaceName, Map<String, Object> expectedStructure, boolean forceRecreate) {
        if (!databaseExists(dbName)) {
            log.info("Database {} does not exist, will create new database", dbName);
            if (!createDatabase(dbName)) {
                log.error("Failed to create database {}", dbName);
                return false;
            }
        }

        if (!spaceExists(dbName, spaceName)) {
            log.info("Space {} does not exist, will create new space", spaceName);
            return createSpace(dbName, spaceName, expectedStructure);
        }

        if (forceRecreate) {
            log.warn("Force recreate mode: delete space {}", spaceName);
            if (!deleteSpace(dbName, spaceName)) {
                log.error("Failed to delete space {}", spaceName);
                return false;
            }
            return createSpace(dbName, spaceName, expectedStructure);
        }

        if (validateSpaceStructure(dbName, spaceName, expectedStructure)) {
            log.info("Space {} exists and structure matches expectations", spaceName);
            return true;
        } else {
            log.warn("Space {} structure does not match expectations, will delete and recreate", spaceName);
            return createSpace(dbName, spaceName, expectedStructure);
        }
    }

    /**
     * 删除向量数据
     * 
     * @param dbName 数据库名称
     * @param spaceName 空间名称
     * @param kbId 知识库ID
     * @param fileIds 文件ID列表
     * @return 是否删除成功
     */
    public boolean deleteVector(String dbName, String spaceName, String kbId, List<String> fileIds) {
        try {
            if (fileIds == null || fileIds.isEmpty()) {
                log.info("No file IDs to delete");
                return true;
            }

            // FIXME: 需要实现实际的Vearch删除操作
            Map<String, Object> filterExpr = Map.of(
                "operator", "AND",
                "conditions", List.of(
                    Map.of("field", "kb_id", "value", List.of(kbId), "operator", "IN"),
                    Map.of("field", "ori_file_id", "value", fileIds, "operator", "IN")
                )
            );

            VearchFilter filter = buildVearchFilter(filterExpr);
            VearchResult response = client.delete(dbName, spaceName, filter);

            if (response.isSuccess()) {
                log.info("Successfully deleted {} records", response.getTotal());
                return true;
            } else {
                log.error("Delete failed: {}", response.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("Error batch deleting data", e);
            return false;
        }
    }

    /**
     * 添加DataFrame数据
     * 
     * @param databaseName 数据库名称
     * @param spaceName 空间名称
     * @param df 数据列表
     * @return 是否添加成功
     */
    public boolean addDf(String databaseName, String spaceName, List<Map<String, Object>> df) {
        List<Map<String, Object>> documents = new ArrayList<>();
        
        for (Map<String, Object> row : df) {
            Map<String, Object> doc = new HashMap<>(row);
            
            for (Map.Entry<String, Object> entry : doc.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof List) {
                    // 保留列表类型
                } else if (value == null) {
                    // 保留null值
                }
            }
            documents.add(doc);
        }

        if (documents.isEmpty()) {
            log.error("No valid documents to write");
            return false;
        }

        int batchSize = 100;
        int successCount = 0;
        int totalCount = documents.size();

        for (int i = 0; i < totalCount; i += batchSize) {
            int batchEnd = Math.min(i + batchSize, totalCount);
            List<Map<String, Object>> batchDocs = documents.subList(i, batchEnd);

            try {
                VearchResult response = client.upsert(databaseName, spaceName, batchDocs);

                if (response.isSuccess()) {
                    int batchSuccess = batchDocs.size();
                    successCount += batchSuccess;
                    log.info("Successfully inserted batch {}, total {} records", 
                        i / batchSize + 1, batchSuccess);
                } else {
                    log.error("Failed to insert batch {}: {}", i / batchSize + 1, response.getMessage());
                    return false;
                }
            } catch (Exception batchError) {
                log.error("Error processing batch {}", i / batchSize + 1, batchError);
                return false;
            }
        }

        log.info("Batch insert completed, successfully inserted {}/{} records", successCount, totalCount);
        return true;
    }

    /**
     * 批量添加向量数据到Vearch空间
     * 
     * @param databaseName 数据库名称
     * @param spaceName 空间名称
     * @param documents 要添加的文档列表
     * @return 是否添加成功
     */
    public boolean addVector(String databaseName, String spaceName, List<Map<String, Object>> documents) {
        try {
            if (documents == null || documents.isEmpty()) {
                log.info("No data to add");
                return true;
            }

            if (!spaceExists(databaseName, spaceName)) {
                log.error("Space {} does not exist in database {}", spaceName, databaseName);
                return false;
            }

            try {
                Map<String, Object> spaceStructure = getSpaceStructure(databaseName, spaceName);
                List<Map<String, Object>> currentFields = new ArrayList<>();
                
                if (spaceStructure.containsKey("fields")) {
                    currentFields = (List<Map<String, Object>>) spaceStructure.get("fields");
                } else if (spaceStructure.containsKey("field")) {
                    currentFields = (List<Map<String, Object>>) spaceStructure.get("field");
                }

                Set<String> existingFields = new HashSet<>();
                for (Map<String, Object> field : currentFields) {
                    String fieldName = (String) field.getOrDefault("name", 
                        field.getOrDefault("field_name", field.get("field")));
                    if (fieldName != null) {
                        existingFields.add(fieldName);
                    }
                }

                Set<String> allDocumentFields = new HashSet<>();
                for (Map<String, Object> doc : documents) {
                    allDocumentFields.addAll(doc.keySet());
                }

                Set<String> missingFields = new HashSet<>(allDocumentFields);
                missingFields.removeAll(existingFields);
                
                if (!missingFields.isEmpty()) {
                    log.error("Documents contain fields not in space: {}", missingFields);
                    log.error("Existing space fields: {}", existingFields);
                    log.error("Fields used by documents: {}", allDocumentFields);
                    return false;
                }

                log.info("Field validation passed, all fields exist in space");
            } catch (Exception e) {
                log.error("Error validating space fields", e);
                log.warn("Field validation failed, but attempting to continue data insertion");
            }

            List<String> requiredFields = List.of("kb_id", "ori_file_id", "chunk_id", "chunk_vector");
            for (int i = 0; i < documents.size(); i++) {
                Map<String, Object> doc = documents.get(i);
                for (String field : requiredFields) {
                    if (!doc.containsKey(field)) {
                        log.error("Document {} is missing required field: {}", i + 1, field);
                        return false;
                    }
                }

                Object vector = doc.get("chunk_vector");
                if (!(vector instanceof List) || ((List<?>) vector).size() != 1024) {
                    log.error("Document {} has incorrect vector dimension, expected 1024, actual {}", 
                        i + 1, vector instanceof List ? ((List<?>) vector).size() : "N/A");
                    return false;
                }
            }

            int batchSize = 100;
            int successCount = 0;
            int totalCount = documents.size();

            for (int i = 0; i < totalCount; i += batchSize) {
                int batchEnd = Math.min(i + batchSize, totalCount);
                List<Map<String, Object>> batchDocs = documents.subList(i, batchEnd);

                try {
                    VearchResult response = client.upsert(databaseName, spaceName, batchDocs);

                    if (response.isSuccess()) {
                        int batchSuccess = batchDocs.size();
                        successCount += batchSuccess;
                        log.info("Successfully inserted batch {}, total {} records", 
                            i / batchSize + 1, batchSuccess);
                    } else {
                        log.error("Failed to insert batch {}: {}", i / batchSize + 1, response.getMessage());
                        return false;
                    }
                } catch (Exception batchError) {
                    log.error("Error processing batch {}", i / batchSize + 1, batchError);
                    return false;
                }
            }

            log.info("Batch insert completed, successfully inserted {}/{} records", successCount, totalCount);
            return successCount == totalCount;
        } catch (Exception e) {
            log.error("Error batch inserting data into Vearch", e);
            return false;
        }
    }

    /**
     * 将LlamaIndex节点转换为Vearch格式并插入
     * 
     * @param databaseName 数据库名称
     * @param spaceName 空间名称
     * @param nodes LlamaIndex节点列表
     * @param embVersion 嵌入版本标识符
     * @return 是否添加成功
     */
    public boolean addNodes(String databaseName, String spaceName, List<Node> nodes, String embVersion) {
        try {
            if (nodes == null || nodes.isEmpty()) {
                log.info("No nodes to add");
                return true;
            }

            List<Map<String, Object>> documents = new ArrayList<>();
            for (Node node : nodes) {
                Map<String, Object> metadata = node.getMetadata() != null ? node.getMetadata() : new HashMap<>();

                Map<String, Object> doc = new HashMap<>();
                doc.put("kb_id", metadata.getOrDefault("kb_id", ""));
                doc.put("ori_file_id", metadata.getOrDefault("ori_file_id", ""));
                doc.put("chunk_id", metadata.getOrDefault("chunk_id", "chunk_" + Math.abs(node.getText().hashCode() % 1000000)));
                doc.put("chunk_text", node.getText());
                doc.put("chunk_extra_info", String.valueOf(metadata.getOrDefault("chunk_extra_info", new HashMap<>())));
                doc.put("return_text", metadata.getOrDefault("return_text", ""));
                doc.put("chunk_vector", node.getEmbedding() != null ? node.getEmbedding() : new ArrayList<>());
                doc.put("emb_version", embVersion != null ? embVersion : "2.0");
                doc.put("language", metadata.getOrDefault("language", ""));

                if (doc.get("kb_id") == null || doc.get("ori_file_id") == null || 
                    doc.get("chunk_vector") == null || ((List<?>) doc.get("chunk_vector")).isEmpty()) {
                    log.warn("Node missing required fields, skipping: chunk_id={}", doc.get("chunk_id"));
                    continue;
                }

                documents.add(doc);
            }

            if (documents.isEmpty()) {
                log.error("No valid documents to insert");
                return false;
            }

            return addVector(databaseName, spaceName, documents);
        } catch (Exception e) {
            log.error("Error converting nodes and inserting into Vearch", e);
            return false;
        }
    }

    /**
     * 诊断空间结构问题
     * 
     * @param dbName 数据库名称
     * @param spaceName 空间名称
     * @param expectedStructure 预期的空间结构
     * @return 诊断结果
     */
    public Map<String, Object> diagnoseSpace(String dbName, String spaceName, Map<String, Object> expectedStructure) {
        Map<String, Object> diagnosis = new HashMap<>();
        diagnosis.put("database_exists", false);
        diagnosis.put("space_exists", false);
        diagnosis.put("structure_match", false);
        diagnosis.put("current_fields", new ArrayList<>());
        diagnosis.put("expected_fields", new ArrayList<>());
        diagnosis.put("missing_fields", new ArrayList<>());
        diagnosis.put("type_mismatches", new ArrayList<>());
        diagnosis.put("errors", new ArrayList<>());

        try {
            diagnosis.put("database_exists", databaseExists(dbName));
            if (!(Boolean) diagnosis.get("database_exists")) {
                ((List<String>) diagnosis.get("errors")).add("Database " + dbName + " does not exist");
                return diagnosis;
            }

            diagnosis.put("space_exists", spaceExists(dbName, spaceName));
            if (!(Boolean) diagnosis.get("space_exists")) {
                ((List<String>) diagnosis.get("errors")).add("Space " + spaceName + " does not exist");
                return diagnosis;
            }

            try {
                Map<String, Object> currentStructure = getSpaceStructure(dbName, spaceName);
                log.info("Current space structure: {}", currentStructure);

                List<Map<String, Object>> currentFields = new ArrayList<>();
                if (currentStructure.containsKey("fields")) {
                    currentFields = (List<Map<String, Object>>) currentStructure.get("fields");
                } else if (currentStructure.containsKey("field")) {
                    currentFields = (List<Map<String, Object>>) currentStructure.get("field");
                }

                Map<String, Map<String, Object>> currentFieldMap = new HashMap<>();
                for (Map<String, Object> field : currentFields) {
                    String fieldName = (String) field.getOrDefault("name", 
                        field.getOrDefault("field_name", field.get("field")));
                    if (fieldName != null) {
                        currentFieldMap.put(fieldName, field);
                    }
                }

                diagnosis.put("current_fields", new ArrayList<>(currentFieldMap.keySet()));
                
                List<Map<String, Object>> expectedFields = (List<Map<String, Object>>) expectedStructure.getOrDefault("fields", new ArrayList<>());
                List<String> expectedFieldNames = new ArrayList<>();
                Map<String, Map<String, Object>> expectedFieldMap = new HashMap<>();
                for (Map<String, Object> field : expectedFields) {
                    expectedFieldNames.add((String) field.get("name"));
                    expectedFieldMap.put((String) field.get("name"), field);
                }
                diagnosis.put("expected_fields", expectedFieldNames);

                for (String fieldName : expectedFieldMap.keySet()) {
                    if (!currentFieldMap.containsKey(fieldName)) {
                        ((List<String>) diagnosis.get("missing_fields")).add(fieldName);
                    }
                }

                for (Map.Entry<String, Map<String, Object>> entry : expectedFieldMap.entrySet()) {
                    String fieldName = entry.getKey();
                    Map<String, Object> expectedField = entry.getValue();
                    
                    if (currentFieldMap.containsKey(fieldName)) {
                        Map<String, Object> currentField = currentFieldMap.get(fieldName);
                        String expectedType = (String) expectedField.get("type");

                        String currentType = null;
                        Object typeObj = currentField.get("type");
                        if (typeObj == null) {
                            typeObj = currentField.getOrDefault("data_type", currentField.get("dtype"));
                        }
                        if (typeObj != null) {
                            currentType = String.valueOf(typeObj);
                        }

                        Map<String, String> typeMapping = Map.of(
                            "STRING", "string",
                            "INTEGER", "integer",
                            "FLOAT", "float",
                            "VECTOR", "vector"
                        );

                        if (typeMapping.containsKey(currentType)) {
                            currentType = typeMapping.get(currentType);
                        }

                        if (!currentType.equals(expectedType)) {
                            ((List<Map<String, Object>>) diagnosis.get("type_mismatches")).add(Map.of(
                                "field", fieldName,
                                "expected", expectedType,
                                "current", currentType
                            ));
                        }
                    }
                }

                boolean structureMatch = ((List<?>) diagnosis.get("missing_fields")).isEmpty() && 
                    ((List<?>) diagnosis.get("type_mismatches")).isEmpty();
                diagnosis.put("structure_match", structureMatch);

            } catch (Exception e) {
                ((List<String>) diagnosis.get("errors")).add("Failed to get space structure: " + e.getMessage());
            }
        } catch (Exception e) {
            ((List<String>) diagnosis.get("errors")).add("Error during diagnosis: " + e.getMessage());
        }

        return diagnosis;
    }

    /**
     * 强制重新创建空间（用于解决结构不匹配问题）
     * 
     * @param dbName 数据库名称
     * @param spaceName 空间名称
     * @param expectedStructure 预期的空间结构
     * @return 重新创建是否成功
     */
    public boolean forceRecreateSpace(String dbName, String spaceName, Map<String, Object> expectedStructure) {
        log.warn("Force recreate space {}, this will delete all existing data", spaceName);

        try {
            Map<String, Object> diagnosis = diagnoseSpace(dbName, spaceName, expectedStructure);
            log.info("Pre-recreation diagnosis result: {}", diagnosis);

            if ((Boolean) diagnosis.get("space_exists")) {
                log.info("Deleting existing space {}", spaceName);
                if (!deleteSpace(dbName, spaceName)) {
                    log.error("Failed to delete space {}", spaceName);
                    return false;
                }
                log.info("Space {} successfully deleted", spaceName);
            }

            log.info("Creating new space {}", spaceName);
            if (createSpace(dbName, spaceName, expectedStructure)) {
                log.info("Space {} recreated successfully", spaceName);

                Map<String, Object> newDiagnosis = diagnoseSpace(dbName, spaceName, expectedStructure);
                if ((Boolean) newDiagnosis.get("structure_match")) {
                    log.info("Space {} passed verification after recreation", spaceName);
                    return true;
                } else {
                    log.error("Space {} still has issues after recreation: {}", spaceName, newDiagnosis);
                    return false;
                }
            } else {
                log.error("Failed to create new space {}", spaceName);
                return false;
            }
        } catch (Exception e) {
            log.error("Error force recreating space {}", spaceName, e);
            return false;
        }
    }

    /**
     * 自动重置空间（如果有结构问题）
     * 
     * @param dbName 数据库名称
     * @param spaceName 空间名称
     * @param expectedStructure 预期的空间结构
     * @return 操作是否成功
     */
    public boolean resetSpaceIfNeeded(String dbName, String spaceName, Map<String, Object> expectedStructure) {
        try {
            Map<String, Object> diagnosis = diagnoseSpace(dbName, spaceName, expectedStructure);

            if (!(Boolean) diagnosis.get("space_exists") || !(Boolean) diagnosis.get("structure_match")) {
                log.warn("Space {} needs reset, diagnosis result: {}", spaceName, diagnosis);
                return forceRecreateSpace(dbName, spaceName, expectedStructure);
            } else {
                log.info("Space {} is in normal state, no reset needed", spaceName);
                return true;
            }
        } catch (Exception e) {
            log.error("Error checking and resetting space {}", spaceName, e);
            return false;
        }
    }

    /**
     * 基于向量进行相似度搜索
     * 
     * @param queryVector 查询向量（1024维）
     * @param topK 返回结果数量
     * @param dbName 数据库名称
     * @param spaceName 空间名称
     * @return 搜索结果列表
     */
    public List<Map<String, Object>> searchVectors(List<Double> queryVector, int topK, String dbName, String spaceName) {
        try {
            // FIXME: 需要实现向量归一化逻辑
            // queryVector_norm = np.array(query_vector) / np.linalg.norm(query_vector)

            VectorInfo vectorInfo = new VectorInfo("chunk_vector", queryVector);

            VearchSearchResult searchResult = client.search(
                dbName,
                spaceName,
                List.of(vectorInfo),
                List.of("chunk_text", "kb_id", "ori_file_id", "chunk_id", "emb_version", "language"),
                topK
            );

            List<Map<String, Object>> results = new ArrayList<>();
            if (searchResult.isSuccess() && searchResult.getDocuments() != null && !searchResult.getDocuments().isEmpty()) {
                for (Map<String, Object> hit : searchResult.getDocuments().get(0)) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("score", hit.getOrDefault("_score", 0.0));
                    result.put("chunk_text", hit.getOrDefault("chunk_text", ""));
                    result.put("kb_id", hit.getOrDefault("kb_id", ""));
                    result.put("ori_file_id", hit.getOrDefault("ori_file_id", ""));
                    result.put("chunk_id", hit.getOrDefault("chunk_id", ""));
                    result.put("emb_version", hit.getOrDefault("emb_version", ""));
                    result.put("language", hit.getOrDefault("language", ""));
                    results.add(result);
                }
            }

            return results;
        } catch (Exception e) {
            log.error("Search error", e);
            return new ArrayList<>();
        }
    }

    /**
     * 构建Vearch过滤器
     * 
     * @param filterExpr 过滤表达式
     * @return Vearch过滤器对象
     */
    private VearchFilter buildVearchFilter(Map<String, Object> filterExpr) {
        // FIXME: 需要实现实际的Vearch过滤器构建逻辑
        return new VearchFilter();
    }

    // ============================================================================
    // 内部类定义（Vearch相关数据结构）
    // ============================================================================

    /**
     * 数据库信息
     */
    static class DatabaseInfo {
        private String name;

        public DatabaseInfo(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * 空间信息
     */
    static class SpaceInfo {
        private String name;

        public SpaceInfo(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Map<String, Object> toMap() {
            return Map.of("name", name);
        }
    }

    /**
     * 空间Schema
     */
    static class SpaceSchema {
        private String name;
        private List<Field> fields;

        public SpaceSchema(String name, List<Field> fields) {
            this.name = name;
            this.fields = fields;
        }

        public String getName() {
            return name;
        }

        public Map<String, Object> toMap() {
            List<Map<String, Object>> fieldMaps = new ArrayList<>();
            for (Field field : fields) {
                fieldMaps.add(field.toMap());
            }
            return Map.of(
                "name", name,
                "fields", fieldMaps
            );
        }
    }

    /**
     * 字段定义
     */
    static class Field {
        private String name;
        private DataType dataType;
        private String desc;
        private Object index;
        private int dimension;

        public Field(String name, DataType dataType, String desc, Object index) {
            this.name = name;
            this.dataType = dataType;
            this.desc = desc;
            this.index = index;
        }

        public Field(String name, DataType dataType, String desc, Object index, int dimension) {
            this(name, dataType, desc, index);
            this.dimension = dimension;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = Map.of(
                "name", name,
                "type", dataType.toString().toLowerCase(),
                "desc", desc
            );
            if (dimension > 0) {
                return Map.of(
                    "name", name,
                    "type", dataType.toString().toLowerCase(),
                    "desc", desc,
                    "dimension", dimension
                );
            }
            return map;
        }
    }

    /**
     * 数据类型枚举
     */
    enum DataType {
        STRING,
        INTEGER,
        FLOAT,
        VECTOR
    }

    /**
     * 度量类型枚举
     */
    enum MetricType {
        INNER_PRODUCT,
        L2
    }

    /**
     * 标量索引
     */
    static class ScalarIndex {
        private String name;

        public ScalarIndex(String name) {
            this.name = name;
        }
    }

    /**
     * 平面索引
     */
    static class FlatIndex {
        private String name;
        private MetricType metricType;

        public FlatIndex(String name, MetricType metricType) {
            this.name = name;
            this.metricType = metricType;
        }
    }

    /**
     * 向量信息
     */
    static class VectorInfo {
        private String fieldName;
        private List<Double> feature;

        public VectorInfo(String fieldName, List<Double> feature) {
            this.fieldName = fieldName;
            this.feature = feature;
        }
    }

    /**
     * Vearch过滤器
     */
    static class VearchFilter {
        private String operator;
        private List<Condition> conditions;

        public VearchFilter() {
            this.operator = "AND";
            this.conditions = new ArrayList<>();
        }

        public VearchFilter(String operator, List<Condition> conditions) {
            this.operator = operator;
            this.conditions = conditions;
        }
    }

    /**
     * 过滤条件
     */
    static class Condition {
        private String operator;
        private String field;
        private Object value;

        public Condition(String operator, String field, Object value) {
            this.operator = operator;
            this.field = field;
            this.value = value;
        }
    }

    /**
     * Vearch结果
     */
    static class VearchResult {
        private boolean success;
        private String message;
        private long total;

        public VearchResult() {
            this.success = true;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public long getTotal() {
            return total;
        }
    }

    /**
     * Vearch搜索结果
     */
    static class VearchSearchResult extends VearchResult {
        private List<List<Map<String, Object>>> documents;

        public VearchSearchResult() {
            super();
        }

        public List<List<Map<String, Object>>> getDocuments() {
            return documents;
        }
    }

    /**
     * LlamaIndex节点
     */
    static class Node {
        private String text;
        private Map<String, Object> metadata;
        private List<Double> embedding;

        public Node(String text, Map<String, Object> metadata, List<Double> embedding) {
            this.text = text;
            this.metadata = metadata;
            this.embedding = embedding;
        }

        public String getText() {
            return text;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public List<Double> getEmbedding() {
            return embedding;
        }
    }

    /**
     * Vearch客户端接口（FIXME: 需要实现实际的客户端）
     */
    static class VearchClient {
        private final String host;
        private final String token;

        public VearchClient(String host, String token) {
            this.host = host;
            this.token = token;
        }

        public List<DatabaseInfo> listDatabases() {
            // FIXME: 需要实现实际的数据库列表查询
            return new ArrayList<>();
        }

        public List<SpaceInfo> listSpaces(String dbName) {
            // FIXME: 需要实现实际的空间列表查询
            return new ArrayList<>();
        }

        public DatabaseClient database(String dbName) {
            return new DatabaseClient(dbName);
        }

        public VearchResult createDatabase(String dbName) {
            // FIXME: 需要实现实际的数据库创建
            VearchResult result = new VearchResult();
            result.success = true;
            return result;
        }

        public VearchResult createSpace(String dbName, SpaceSchema schema) {
            // FIXME: 需要实现实际的空间创建
            VearchResult result = new VearchResult();
            result.success = true;
            return result;
        }

        public VearchResult dropSpace(String dbName, String spaceName) {
            // FIXME: 需要实现实际的空间删除
            VearchResult result = new VearchResult();
            result.success = true;
            return result;
        }

        public VearchResult upsert(String dbName, String spaceName, List<Map<String, Object>> data) {
            // FIXME: 需要实现实际的数据插入
            VearchResult result = new VearchResult();
            result.success = true;
            return result;
        }

        public VearchResult delete(String dbName, String spaceName, VearchFilter filter) {
            // FIXME: 需要实现实际的数据删除
            VearchResult result = new VearchResult();
            result.success = true;
            result.total = 0;
            return result;
        }

        public VearchSearchResult search(String dbName, String spaceName, List<VectorInfo> vectorInfos,
                                      List<String> fields, int limit) {
            // FIXME: 需要实现实际的向量搜索
            VearchSearchResult result = new VearchSearchResult();
            result.documents = new ArrayList<>();
            return result;
        }
    }

    /**
     * 数据库客户端
     */
    static class DatabaseClient {
        private final String dbName;

        public DatabaseClient(String dbName) {
            this.dbName = dbName;
        }

        public SpaceClient space(String spaceName) {
            return new SpaceClient(dbName, spaceName);
        }
    }

    /**
     * 空间客户端
     */
    static class SpaceClient {
        private final String dbName;
        private final String spaceName;

        public SpaceClient(String dbName, String spaceName) {
            this.dbName = dbName;
            this.spaceName = spaceName;
        }

        public SpaceInfo describe() {
            // FIXME: 需要实现实际的空间描述查询
            return new SpaceInfo(spaceName);
        }

        public SpaceSchema exist() {
            // FIXME: 需要实现实际的空间存在性查询
            return new SpaceSchema(spaceName, new ArrayList<>());
        }
    }
}
