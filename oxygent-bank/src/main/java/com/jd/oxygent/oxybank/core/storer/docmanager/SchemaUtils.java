package com.jd.oxygent.oxybank.core.storer.docmanager;

import com.jd.oxygent.oxybank.core.model.FieldInfo;
import com.jd.oxygent.oxybank.core.model.KBSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Schema操作工具函数
 */
@Slf4j
@Component
public class SchemaUtils {

    /**
     * 根据知识库schema转换数据列表的列类型
     * 
     * 处理流程:
     * 1. 保留空值原样(暂时不转换为空字符串)
     * 2. 根据schema field_type转换列类型
     * 3. 对于字符串列: 将空值转换为空字符串""
     * 4. 对于整数/浮点列: 如果存在空值则抛出错误
     * 
     * @param dataList 要转换的数据列表
     * @param kbSchema 包含字段定义的知识库schema
     * @return 转换后的数据列表
     * @throws IllegalArgumentException 如果整数/浮点列包含空值
     */
    public static List<Map<String, Object>> convertDataTypesBySchema(List<Map<String, Object>> dataList, KBSchema kbSchema) {
        if (kbSchema.getFields() == null || kbSchema.getFields().isEmpty()) {
            log.warn("Schema has no fields defined, skipping type conversion");
            return dataList;
        }

        List<Map<String, Object>> result = new ArrayList<>(dataList);

        for (FieldInfo fieldInfo : kbSchema.getFields()) {
            String fieldName = fieldInfo.getFieldName();
            String fieldType = fieldInfo.getFieldType();

            boolean fieldExists = false;
            for (Map<String, Object> data : dataList) {
                if (data.containsKey(fieldName)) {
                    fieldExists = true;
                    break;
                }
            }

            if (!fieldExists) {
                log.warn("Field '{}' defined in schema but not found in data, skipping", fieldName);
                continue;
            }

            try {
                if ("string".equals(fieldType)) {
                    convertStringField(result, fieldName);
                } else if ("integer".equals(fieldType)) {
                    convertIntegerField(result, fieldName);
                } else if ("float".equals(fieldType)) {
                    convertFloatField(result, fieldName);
                }
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                log.error("Failed to convert field '{}' to type '{}'", fieldName, fieldType, e);
                throw new IllegalArgumentException("Failed to convert field '" + fieldName + "' to type '" + fieldType + "': " + e.getMessage(), e);
            }
        }

        return result;
    }

    /**
     * 转换字符串类型字段
     * 
     * @param dataList 数据列表
     * @param fieldName 字段名
     */
    private static void convertStringField(List<Map<String, Object>> dataList, String fieldName) {
        for (Map<String, Object> data : dataList) {
            Object value = data.get(fieldName);
            if (value == null) {
                data.put(fieldName, "");
            } else {
                data.put(fieldName, String.valueOf(value));
            }
        }
        log.debug("Converted field '{}' to string type", fieldName);
    }

    /**
     * 转换整数类型字段
     * 
     * @param dataList 数据列表
     * @param fieldName 字段名
     * @throws IllegalArgumentException 如果字段包含空值或无效值
     */
    private static void convertIntegerField(List<Map<String, Object>> dataList, String fieldName) {
        List<Integer> invalidIndices = new ArrayList<>();
        List<String> invalidValues = new ArrayList<>();

        for (int i = 0; i < dataList.size(); i++) {
            Map<String, Object> data = dataList.get(i);
            Object value = data.get(fieldName);

            if (value == null) {
                invalidIndices.add(i);
                continue;
            }

            try {
                if (value instanceof Number) {
                    data.put(fieldName, ((Number) value).longValue());
                } else {
                    String strValue = String.valueOf(value);
                    if (strValue.isEmpty()) {
                        invalidIndices.add(i);
                    } else {
                        data.put(fieldName, Long.parseLong(strValue));
                    }
                }
            } catch (NumberFormatException e) {
                invalidIndices.add(i);
                invalidValues.add(String.valueOf(value));
            }
        }

        if (!invalidIndices.isEmpty()) {
            throw new IllegalArgumentException(
                "Field '" + fieldName + "' is defined as integer type in schema, " +
                "but contains " + invalidIndices.size() + " invalid value(s). " +
                "Invalid values: " + invalidValues + ". " +
                "Please ensure all values in field '" + fieldName + "' are valid integers."
            );
        }

        log.debug("Converted field '{}' to integer type", fieldName);
    }

    /**
     * 转换浮点类型字段
     * 
     * @param dataList 数据列表
     * @param fieldName 字段名
     * @throws IllegalArgumentException 如果字段包含空值或无效值
     */
    private static void convertFloatField(List<Map<String, Object>> dataList, String fieldName) {
        List<Integer> invalidIndices = new ArrayList<>();
        List<String> invalidValues = new ArrayList<>();

        for (int i = 0; i < dataList.size(); i++) {
            Map<String, Object> data = dataList.get(i);
            Object value = data.get(fieldName);

            if (value == null) {
                invalidIndices.add(i);
                continue;
            }

            try {
                if (value instanceof Number) {
                    data.put(fieldName, ((Number) value).doubleValue());
                } else {
                    String strValue = String.valueOf(value);
                    if (strValue.isEmpty()) {
                        invalidIndices.add(i);
                    } else {
                        data.put(fieldName, Double.parseDouble(strValue));
                    }
                }
            } catch (NumberFormatException e) {
                invalidIndices.add(i);
                invalidValues.add(String.valueOf(value));
            }
        }

        if (!invalidIndices.isEmpty()) {
            throw new IllegalArgumentException(
                "Field '" + fieldName + "' is defined as float type in schema, " +
                "but contains " + invalidIndices.size() + " invalid value(s). " +
                "Invalid values: " + invalidValues + ". " +
                "Please ensure all values in field '" + fieldName + "' are valid float numbers."
            );
        }

        log.debug("Converted field '{}' to float type", fieldName);
    }
}
