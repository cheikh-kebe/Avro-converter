package com.shanks.converter;

import com.shanks.model.AvroTypeInfo;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;

import java.util.*;

/**
 * Generates Avro schemas from inferred type information.
 *
 * This class follows the Single Responsibility Principle by focusing solely on
 * schema generation from AvroTypeInfo.
 */
public class SchemaGenerator {

    private static final String DEFAULT_NAMESPACE = "com.shanks.generated";
    private final Map<String, Schema> enumSchemaCache = new HashMap<>();
    private boolean includeDoc = false;
    private int enumCounter = 0;

    /**
     * Set whether to include doc fields in generated schemas.
     *
     * @param includeDoc true to include doc fields from OpenAPI descriptions
     */
    public void setIncludeDoc(boolean includeDoc) {
        this.includeDoc = includeDoc;
    }

    /**
     * Generate an Avro schema from the root type information.
     *
     * @param rootType   the root type information
     * @param recordName the name for the root record
     * @return the generated Avro schema
     */
    public Schema generateSchema(AvroTypeInfo rootType, String recordName) {
        enumSchemaCache.clear();
        enumCounter = 0;

        if (rootType.getAvroType() == Schema.Type.RECORD) {
            return generateRecordSchema(rootType, recordName, DEFAULT_NAMESPACE);
        }

        return generateTypeSchema(rootType, recordName, DEFAULT_NAMESPACE);
    }

    /**
     * Generate a schema for any type.
     */
    private Schema generateTypeSchema(AvroTypeInfo typeInfo, String name, String namespace) {
        switch (typeInfo.getAvroType()) {
            case NULL:
                return Schema.create(Schema.Type.NULL);

            case BOOLEAN:
                return Schema.create(Schema.Type.BOOLEAN);

            case INT:
                return Schema.create(Schema.Type.INT);

            case LONG:
                return Schema.create(Schema.Type.LONG);

            case FLOAT:
                return Schema.create(Schema.Type.FLOAT);

            case DOUBLE:
                return Schema.create(Schema.Type.DOUBLE);

            case STRING:
                return generateStringSchema(typeInfo);

            case ARRAY:
                return generateArraySchema(typeInfo, name, namespace);

            case ENUM:
                return generateEnumSchema(typeInfo, name, namespace);

            case RECORD:
                return generateRecordSchema(typeInfo, name, namespace);

            case UNION:
                return generateUnionSchema(typeInfo, name, namespace);

            default:
                return Schema.create(Schema.Type.STRING);
        }
    }

    /**
     * Generate a string schema with optional logical type and pattern.
     * If the type has a name (recordName), create a named type.
     */
    private Schema generateStringSchema(AvroTypeInfo typeInfo) {
        if (typeInfo.getRecordName() != null && typeInfo.getLogicalType() != null) {
            // Named schema for logical types (e.g. UUID)
            StringBuilder schemaJson = new StringBuilder();
            schemaJson.append("{\"name\":\"").append(typeInfo.getRecordName()).append("\"");
            schemaJson.append(",\"type\":\"string\"");
            schemaJson.append(",\"logicalType\":\"").append(typeInfo.getLogicalType()).append("\"");
            if (typeInfo.getPattern() != null && !typeInfo.getPattern().isEmpty()) {
                // Escape backslashes and quotes in pattern
                String escapedPattern = typeInfo.getPattern()
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"");
                schemaJson.append(",\"pattern\":\"").append(escapedPattern).append("\"");
            }
            schemaJson.append("}");
            return new Schema.Parser().parse(schemaJson.toString());
        }

        // For string with pattern but no logical type
        if (typeInfo.getPattern() != null && !typeInfo.getPattern().isEmpty()) {
            StringBuilder schemaJson = new StringBuilder();
            schemaJson.append("{\"type\":\"string\"");
            String escapedPattern = typeInfo.getPattern()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"");
            schemaJson.append(",\"pattern\":\"").append(escapedPattern).append("\"");
            if (typeInfo.getLogicalType() != null) {
                schemaJson.append(",\"logicalType\":\"").append(typeInfo.getLogicalType()).append("\"");
            }
            schemaJson.append("}");
            return new Schema.Parser().parse(schemaJson.toString());
        }

        Schema stringSchema = Schema.create(Schema.Type.STRING);

        if ("uuid".equals(typeInfo.getLogicalType())) {
            LogicalTypes.uuid().addToSchema(stringSchema);
        }

        return stringSchema;
    }

    /**
     * Generate an array schema.
     */
    private Schema generateArraySchema(AvroTypeInfo typeInfo, String name, String namespace) {
        AvroTypeInfo itemType = typeInfo.getArrayItemType();
        if (itemType == null) {
            itemType = AvroTypeInfo.builder()
                    .avroType(Schema.Type.STRING)
                    .build();
        }

        Schema itemSchema = generateTypeSchema(itemType, name, namespace);
        return Schema.createArray(itemSchema);
    }

    /**
     * Generate an enum schema.
     */
    private Schema generateEnumSchema(AvroTypeInfo typeInfo, String name, String namespace) {
        List<String> symbols = typeInfo.getEnumSymbols();
        if (symbols == null || symbols.isEmpty()) {
            symbols = Collections.singletonList("UNKNOWN");
        }

        String enumName = typeInfo.getRecordName() != null ? typeInfo.getRecordName() : sanitizeName(name);

        String cacheKey = (namespace != null ? namespace : "") + "." + enumName;
        if (enumSchemaCache.containsKey(cacheKey)) {
            return enumSchemaCache.get(cacheKey);
        }

        String enumDoc = includeDoc ? typeInfo.getDoc() : null;
        Schema enumSchema = Schema.createEnum(enumName, enumDoc, namespace, symbols);
        enumSchemaCache.put(cacheKey, enumSchema);

        return enumSchema;
    }

    /**
     * Generate a record schema.
     */
    private Schema generateRecordSchema(AvroTypeInfo typeInfo, String name, String namespace) {
        String recordName = typeInfo.getRecordName() != null ? typeInfo.getRecordName() : sanitizeName(name);
        // Child namespace deepens the hierarchy: parent.namespace + "." + this record name
        String childNamespace = namespace + "." + recordName.toLowerCase();

        List<Schema.Field> fields = new ArrayList<>();

        if (typeInfo.getFields() != null) {
            for (Map.Entry<String, AvroTypeInfo> entry : typeInfo.getFields().entrySet()) {
                String fieldName = entry.getKey();
                AvroTypeInfo fieldType = entry.getValue();

                Schema fieldSchema = generateTypeSchema(fieldType, fieldName, childNamespace);

                // Add default: null for nullable fields (union with null first)
                Object defaultValue = null;
                if (fieldType.getAvroType() == Schema.Type.UNION &&
                        fieldType.getUnionTypes() != null &&
                        !fieldType.getUnionTypes().isEmpty() &&
                        fieldType.getUnionTypes().get(0).getAvroType() == Schema.Type.NULL) {
                    defaultValue = Schema.Field.NULL_VALUE;
                }

                String fieldDoc = includeDoc ? fieldType.getDoc() : null;
                Schema.Field field = new Schema.Field(fieldName, fieldSchema, fieldDoc, defaultValue);
                fields.add(field);
            }
        }

        String recordDoc = includeDoc ? typeInfo.getDoc() : null;
        return Schema.createRecord(recordName, recordDoc, namespace, false, fields);
    }

    /**
     * Generate a union schema.
     */
    private Schema generateUnionSchema(AvroTypeInfo typeInfo, String name, String namespace) {
        List<Schema> unionTypes = new ArrayList<>();

        if (typeInfo.getUnionTypes() != null && !typeInfo.getUnionTypes().isEmpty()) {
            for (AvroTypeInfo unionType : typeInfo.getUnionTypes()) {
                Schema schema = generateTypeSchema(unionType, name, namespace);
                unionTypes.add(schema);
            }
        }

        return Schema.createUnion(unionTypes);
    }

    /**
     * Sanitize name for Avro schema (remove invalid characters).
     */
    private String sanitizeName(String name) {
        if (name == null || name.isEmpty()) {
            return "Field" + (enumCounter++);
        }

        String sanitized = name.replaceAll("[^a-zA-Z0-9_]", "_");

        if (!Character.isLetter(sanitized.charAt(0)) && sanitized.charAt(0) != '_') {
            sanitized = "_" + sanitized;
        }

        return sanitized;
    }

    /**
     * Convert the schema to pretty-printed JSON with custom formatting for named
     * types.
     *
     * @param schema the Avro schema
     * @return the pretty-printed JSON string
     */
    public String toPrettyJson(Schema schema) {
        String json = schema.toString(true);
        return json;
    }

    public String generateSchemaJson(AvroTypeInfo rootType, String recordName) {
        Schema schema = generateSchema(rootType, recordName);
        return schema.toString(true);
    }
}
