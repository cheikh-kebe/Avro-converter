package com.shanks.converter;

import com.shanks.model.AvroTypeInfo;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.SchemaParseException;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Builds real {@link org.apache.avro.Schema} object graphs from {@link AvroTypeInfo}.
 *
 * This is the single source of truth for turning inferred/mapped type information into
 * Avro schemas. Every output mode (standard files, schema-registry payloads, ...) should
 * build through here and serialize with {@code Schema.toString(true)} rather than hand-rolling
 * JSON, so Avro's own naming/structural validation and named-type deduplication apply uniformly.
 */
class AvroSchemaBuilder {

    // Avro's own naming rule (org.apache.avro.Schema#validateName): applies identically to
    // record names, field names, enum names, and enum symbols.
    private static final Pattern AVRO_NAME_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private final Map<String, Schema> enumSchemaCache = new HashMap<>();
    private final boolean includeDoc;
    private int nameCounter = 0;

    AvroSchemaBuilder(boolean includeDoc) {
        this.includeDoc = includeDoc;
    }

    /**
     * Build the schema for a root type.
     *
     * @param rootType the root type information
     * @param rootName the name for the root record
     * @param namespace the namespace for the root type
     * @return the built Avro schema
     */
    Schema build(AvroTypeInfo rootType, String rootName, String namespace) {
        if (rootType.getAvroType() == Schema.Type.RECORD) {
            return generateRecordSchema(rootType, rootName, namespace);
        }

        return generateTypeSchema(rootType, rootName, namespace);
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
        String fullName = (namespace != null && !namespace.isEmpty() ? namespace + "." : "") + enumName;

        requireValidAvroName("enum name", enumName, "enum " + fullName);
        for (String symbol : symbols) {
            requireValidAvroName("enum symbol", symbol, "enum " + fullName);
        }

        String cacheKey = (namespace != null ? namespace : "") + "." + enumName;
        if (enumSchemaCache.containsKey(cacheKey)) {
            return enumSchemaCache.get(cacheKey);
        }

        String enumDoc = includeDoc ? typeInfo.getDoc() : null;
        Schema enumSchema;
        try {
            enumSchema = Schema.createEnum(enumName, enumDoc, namespace, symbols);
        } catch (SchemaParseException e) {
            // Safety net for any Avro naming/structural rule we didn't pre-validate above.
            throw new AvroSchemaValidationException(
                    "Invalid Avro enum \"" + fullName + "\": " + e.getMessage(), e);
        }
        enumSchemaCache.put(cacheKey, enumSchema);

        return enumSchema;
    }

    /**
     * Generate a record schema.
     */
    private Schema generateRecordSchema(AvroTypeInfo typeInfo, String name, String namespace) {
        String recordName = typeInfo.getRecordName() != null ? typeInfo.getRecordName() : sanitizeName(name);
        String fullName = (namespace != null && !namespace.isEmpty() ? namespace + "." : "") + recordName;
        requireValidAvroName("record name", recordName, "record " + fullName);

        // Child namespace deepens the hierarchy: parent.namespace + "." + this record name
        String childNamespace = namespace + "." + recordName.toLowerCase();

        List<Schema.Field> fields = new ArrayList<>();

        if (typeInfo.getFields() != null) {
            for (Map.Entry<String, AvroTypeInfo> entry : typeInfo.getFields().entrySet()) {
                String fieldName = entry.getKey();
                requireValidAvroName("field name", fieldName, "field of record " + fullName);
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
        try {
            return Schema.createRecord(recordName, recordDoc, namespace, false, fields);
        } catch (SchemaParseException e) {
            // Safety net for any Avro naming/structural rule we didn't pre-validate above.
            throw new AvroSchemaValidationException(
                    "Invalid Avro record \"" + fullName + "\": " + e.getMessage(), e);
        }
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
            return "Field" + (nameCounter++);
        }

        String sanitized = name.replaceAll("[^a-zA-Z0-9_]", "_");

        if (!Character.isLetter(sanitized.charAt(0)) && sanitized.charAt(0) != '_') {
            sanitized = "_" + sanitized;
        }

        return sanitized;
    }

    /**
     * Validate a value against Avro's naming rule, raising a human-readable, actionable
     * error instead of leaving Avro's own (context-free) exception to surface as-is.
     *
     * @param kind  what is being validated, e.g. "enum symbol", "field name"
     * @param value the value to check
     * @param where where it was found, e.g. "enum com.shanks.generated.CardType"
     */
    private static void requireValidAvroName(String kind, String value, String where) {
        if (value != null && AVRO_NAME_PATTERN.matcher(value).matches()) {
            return;
        }

        throw new AvroSchemaValidationException(String.format(
                "Invalid Avro %s%n" +
                "%n" +
                "  Value:   \"%s\"%n" +
                "  Where:   %s%n" +
                "  Reason:  Avro names must start with a letter or underscore, and contain only%n" +
                "           letters, digits, and underscores (rule: [A-Za-z_][A-Za-z0-9_]*)%n" +
                "%n" +
                "  Fix: rename this value in your OpenAPI/JSON source.",
                kind, value, where));
    }
}
