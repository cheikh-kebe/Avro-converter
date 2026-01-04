package com.shanks.converter;

import com.shanks.model.AvroTypeInfo;
import org.apache.avro.Schema;

import java.util.*;

/**
 * Generates unified Avro schemas with all type definitions in a single file.
 *
 * This generator collects all named types (enums, records) and creates a single
 * schema file where types are defined once and referenced by name.
 *
 * Following SOLID principles:
 * - Single Responsibility: Focuses on generating unified schemas with type references
 * - Open/Closed: Can be extended for new type collection strategies
 */
public class UnifiedSchemaGenerator {

    private static final String DEFAULT_NAMESPACE = "com.shanks.generated";
    private final Map<String, AvroTypeInfo> namedTypes = new LinkedHashMap<>();
    private final Set<String> processedTypes = new HashSet<>();

    /**
     * Generate a unified Avro schema with all type definitions.
     *
     * @param rootType the root type information
     * @param rootName the name for the root schema
     * @return JSON array containing all schemas
     */
    public String generateUnifiedSchema(AvroTypeInfo rootType, String rootName) {
        namedTypes.clear();
        processedTypes.clear();

        // Collect all named types
        collectNamedTypes(rootType, rootName, DEFAULT_NAMESPACE);

        // Build JSON array of schemas
        StringBuilder json = new StringBuilder("[\n");

        List<Map.Entry<String, AvroTypeInfo>> typeList = new ArrayList<>(namedTypes.entrySet());
        for (int i = 0; i < typeList.size(); i++) {
            Map.Entry<String, AvroTypeInfo> entry = typeList.get(i);
            json.append(generateTypeJson(entry.getValue(), getSimpleName(entry.getKey()), DEFAULT_NAMESPACE, 2));
            if (i < typeList.size() - 1) {
                json.append(",\n");
            }
        }

        json.append("\n]");
        return json.toString();
    }

    /**
     * Collect all named types (enums, records) from the type tree.
     */
    private void collectNamedTypes(AvroTypeInfo typeInfo, String name, String namespace) {
        if (typeInfo == null) {
            return;
        }

        String fullName = namespace + "." + name;

        switch (typeInfo.getAvroType()) {
            case ENUM:
                if (!processedTypes.contains(fullName)) {
                    processedTypes.add(fullName);
                    namedTypes.put(fullName, typeInfo);
                }
                break;

            case RECORD:
                if (!processedTypes.contains(fullName)) {
                    processedTypes.add(fullName);

                    // First, collect all nested types
                    if (typeInfo.getFields() != null) {
                        for (Map.Entry<String, AvroTypeInfo> field : typeInfo.getFields().entrySet()) {
                            collectNamedTypes(field.getValue(), getTypeName(field.getValue()), namespace);
                        }
                    }

                    // Then add the record
                    namedTypes.put(fullName, typeInfo);
                }
                break;

            case ARRAY:
                if (typeInfo.getArrayItemType() != null) {
                    collectNamedTypes(typeInfo.getArrayItemType(), getTypeName(typeInfo.getArrayItemType()), namespace);
                }
                break;

            case UNION:
                if (typeInfo.getUnionTypes() != null) {
                    for (AvroTypeInfo unionType : typeInfo.getUnionTypes()) {
                        collectNamedTypes(unionType, getTypeName(unionType), namespace);
                    }
                }
                break;
        }
    }

    /**
     * Generate JSON for a type.
     */
    private String generateTypeJson(AvroTypeInfo typeInfo, String name, String namespace, int indent) {
        StringBuilder json = new StringBuilder();
        String indentStr = "  ".repeat(indent / 2);

        switch (typeInfo.getAvroType()) {
            case ENUM:
                json.append(indentStr).append("{\n");
                json.append(indentStr).append("  \"type\": \"enum\",\n");
                json.append(indentStr).append("  \"name\": \"").append(name).append("\",\n");
                json.append(indentStr).append("  \"namespace\": \"").append(namespace).append("\",\n");
                json.append(indentStr).append("  \"symbols\": [");

                List<String> symbols = typeInfo.getEnumSymbols();
                for (int i = 0; i < symbols.size(); i++) {
                    json.append("\"").append(symbols.get(i)).append("\"");
                    if (i < symbols.size() - 1) json.append(", ");
                }

                json.append("]\n");
                json.append(indentStr).append("}");
                break;

            case RECORD:
                json.append(indentStr).append("{\n");
                json.append(indentStr).append("  \"type\": \"record\",\n");
                json.append(indentStr).append("  \"name\": \"").append(name).append("\",\n");
                json.append(indentStr).append("  \"namespace\": \"").append(namespace).append("\",\n");
                json.append(indentStr).append("  \"fields\": [\n");

                if (typeInfo.getFields() != null) {
                    List<Map.Entry<String, AvroTypeInfo>> fields = new ArrayList<>(typeInfo.getFields().entrySet());
                    for (int i = 0; i < fields.size(); i++) {
                        Map.Entry<String, AvroTypeInfo> field = fields.get(i);
                        json.append(indentStr).append("    {\n");
                        json.append(indentStr).append("      \"name\": \"").append(field.getKey()).append("\",\n");
                        json.append(indentStr).append("      \"type\": ");
                        json.append(generateFieldTypeJson(field.getValue(), namespace));

                        // Add default for nullable fields
                        if (field.getValue().getAvroType() == Schema.Type.UNION &&
                            field.getValue().getUnionTypes() != null &&
                            field.getValue().getUnionTypes().get(0).getAvroType() == Schema.Type.NULL) {
                            json.append(",\n");
                            json.append(indentStr).append("      \"default\": null\n");
                        } else {
                            json.append("\n");
                        }

                        json.append(indentStr).append("    }");
                        if (i < fields.size() - 1) json.append(",");
                        json.append("\n");
                    }
                }

                json.append(indentStr).append("  ]\n");
                json.append(indentStr).append("}");
                break;
        }

        return json.toString();
    }

    /**
     * Generate JSON for a field type (with references).
     */
    private String generateFieldTypeJson(AvroTypeInfo typeInfo, String namespace) {
        switch (typeInfo.getAvroType()) {
            case NULL:
                return "\"null\"";
            case BOOLEAN:
                return "\"boolean\"";
            case INT:
                return "\"int\"";
            case LONG:
                if ("timestamp-millis".equals(typeInfo.getLogicalType())) {
                    return "{\"type\": \"long\", \"logicalType\": \"timestamp-millis\"}";
                }
                return "\"long\"";
            case FLOAT:
                return "\"float\"";
            case DOUBLE:
                return "\"double\"";
            case STRING:
                if ("uuid".equals(typeInfo.getLogicalType()) ||
                    (typeInfo.getPattern() != null && !typeInfo.getPattern().isEmpty())) {
                    StringBuilder stringType = new StringBuilder("{\"type\": \"string\"");
                    if ("uuid".equals(typeInfo.getLogicalType())) {
                        stringType.append(", \"logicalType\": \"uuid\"");
                    }
                    if (typeInfo.getPattern() != null && !typeInfo.getPattern().isEmpty()) {
                        // Escape backslashes and quotes in pattern
                        String escapedPattern = typeInfo.getPattern()
                                .replace("\\", "\\\\")
                                .replace("\"", "\\\"");
                        stringType.append(", \"pattern\": \"").append(escapedPattern).append("\"");
                    }
                    stringType.append("}");
                    return stringType.toString();
                }
                return "\"string\"";

            case ENUM:
                // Reference the enum by name
                String enumName = typeInfo.getRecordName() != null
                    ? typeInfo.getRecordName()
                    : "UnknownEnum";
                return "\"" + namespace + "." + enumName + "\"";

            case RECORD:
                // Reference the record by name
                String recordName = typeInfo.getRecordName() != null
                    ? typeInfo.getRecordName()
                    : "UnknownRecord";
                return "\"" + namespace + "." + recordName + "\"";

            case ARRAY:
                return "{\"type\": \"array\", \"items\": " +
                       generateFieldTypeJson(typeInfo.getArrayItemType(), namespace) + "}";

            case UNION:
                StringBuilder unionJson = new StringBuilder("[");
                List<AvroTypeInfo> unionTypes = typeInfo.getUnionTypes();
                for (int i = 0; i < unionTypes.size(); i++) {
                    unionJson.append(generateFieldTypeJson(unionTypes.get(i), namespace));
                    if (i < unionTypes.size() - 1) unionJson.append(", ");
                }
                unionJson.append("]");
                return unionJson.toString();

            default:
                return "\"string\"";
        }
    }

    /**
     * Get type name from AvroTypeInfo.
     */
    private String getTypeName(AvroTypeInfo typeInfo) {
        if (typeInfo.getRecordName() != null) {
            return typeInfo.getRecordName();
        }

        switch (typeInfo.getAvroType()) {
            case ENUM:
                return "Enum" + UUID.randomUUID().toString().substring(0, 8);
            case RECORD:
                return "Record" + UUID.randomUUID().toString().substring(0, 8);
            default:
                return typeInfo.getAvroType().getName();
        }
    }

    /**
     * Extract simple name from full name.
     */
    private String getSimpleName(String fullName) {
        int lastDot = fullName.lastIndexOf('.');
        return lastDot > 0 ? fullName.substring(lastDot + 1) : fullName;
    }
}
