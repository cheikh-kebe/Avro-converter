package com.shanks.mapper;

import com.shanks.model.AvroTypeInfo;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.avro.Schema.Type;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Maps OpenAPI/Swagger schema types to Avro type information.
 *
 * This class follows the Single Responsibility Principle by focusing solely
 * on type mapping between OpenAPI and Avro schemas.
 */
public class OpenApiToAvroTypeMapper {

    private static final Pattern INVALID_CHARS = Pattern.compile("[^a-zA-Z0-9_]");
    /** Avro naming rule, mirrored from {@code AvroSchemaBuilder}. */
    private static final Pattern AVRO_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final String X_REPLACED_BY = "x-replaced-by";

    private final OpenAPI openAPI;

    /**
     * Constructor with OpenAPI specification.
     *
     * @param openAPI the parsed OpenAPI specification
     */
    public OpenApiToAvroTypeMapper(OpenAPI openAPI) {
        this.openAPI = openAPI;
    }

    /**
     * Map an OpenAPI schema to Avro type information.
     *
     * @param schema    the OpenAPI schema
     * @param fieldName the field name
     * @return the mapped Avro type information
     */
    public AvroTypeInfo mapSchema(Schema<?> schema, String fieldName) {
        if (schema == null) {
            return AvroTypeInfo.builder()
                    .avroType(Type.STRING)
                    .build();
        }

        // Handle $ref
        if (schema.get$ref() != null) {
            return mapReference(schema.get$ref(), fieldName);
        }

        // Handle enums
        if (schema.getEnum() != null && !schema.getEnum().isEmpty()) {
            return mapEnum(schema, fieldName);
        }

        // Handle anyOf / oneOf (e.g. an optional child record modelled in OpenAPI 3.1
        // as anyOf: [ { $ref: Child }, { type: "null" } ])
        if (schema.getAnyOf() != null && !schema.getAnyOf().isEmpty()) {
            return mapComposedSchema(schema.getAnyOf(), fieldName);
        }
        if (schema.getOneOf() != null && !schema.getOneOf().isEmpty()) {
            return mapComposedSchema(schema.getOneOf(), fieldName);
        }

        String type = resolveType(schema);
        String format = schema.getFormat();
        String description = schema.getDescription();

        if (type == null) {
            return AvroTypeInfo.builder()
                    .avroType(Type.STRING)
                    .build();
        }

        switch (type.toLowerCase()) {
            case "string":
                return mapStringType(format, schema.getPattern(), description);
            case "integer":
                return mapIntegerType(format, description);
            case "number":
                return mapNumberType(format, description);
            case "boolean":
                AvroTypeInfo.Builder boolBuilder = AvroTypeInfo.builder()
                        .avroType(Type.BOOLEAN);
                if (description != null && !description.isEmpty()) {
                    boolBuilder.doc(description);
                }
                return boolBuilder.build();
            case "array":
                return mapArrayType(schema, fieldName);
            case "object":
                return mapObjectType(schema, fieldName);
            default:
                return AvroTypeInfo.builder()
                        .avroType(Type.STRING)
                        .build();
        }
    }

    /**
     * Map string type with format, pattern and description.
     */
    private AvroTypeInfo mapStringType(String format, String pattern, String description) {
        AvroTypeInfo.Builder builder = AvroTypeInfo.builder()
                .avroType(Type.STRING);

        if (format != null) {
            switch (format.toLowerCase()) {
                case "uuid":
                    builder.logicalType("uuid");
                    break;
                case "date":
                case "date-time":
                    break;
            }
        }

        if (pattern != null && !pattern.isEmpty()) {
            builder.pattern(pattern);
        }

        if (description != null && !description.isEmpty()) {
            builder.doc(description);
        }

        return builder.build();
    }

    /** Map integer type to Avro string (avoids precision loss across int32/int64 formats). */
    private AvroTypeInfo mapIntegerType(String format, String description) {
        AvroTypeInfo.Builder builder = AvroTypeInfo.builder().avroType(Type.STRING);
        if (description != null && !description.isEmpty()) {
            builder.doc(description);
        }
        return builder.build();
    }

    /** Map number type to Avro string (avoids precision loss across float/double formats). */
    private AvroTypeInfo mapNumberType(String format, String description) {
        AvroTypeInfo.Builder builder = AvroTypeInfo.builder().avroType(Type.STRING);
        if (description != null && !description.isEmpty()) {
            builder.doc(description);
        }
        return builder.build();
    }

    /**
     * Map enum type.
     */
    private AvroTypeInfo mapEnum(Schema<?> schema, String fieldName) {
        List<String> symbols = new ArrayList<>();
        for (Object enumValue : schema.getEnum()) {
            symbols.add(enumValue.toString());
        }

        AvroTypeInfo.Builder builder = AvroTypeInfo.builder()
                .avroType(Type.ENUM)
                .enumSymbols(symbols)
                .recordName(capitalize(fieldName));

        String description = schema.getDescription();
        if (description != null && !description.isEmpty()) {
            builder.doc(description);
        }

        return builder.build();
    }

    /**
     * Map an {@code anyOf} / {@code oneOf} branch list. A {@code {"type": "null"}}
     * branch (OpenAPI 3.1's way of marking a child schema optional) makes the result
     * nullable; the remaining branches collapse to a single type when there is only
     * one, otherwise to a flat Avro union.
     */
    private AvroTypeInfo mapComposedSchema(List<Schema> branches, String fieldName) {
        List<Schema<?>> valueBranches = new ArrayList<>();
        boolean nullable = false;

        for (Schema<?> branch : branches) {
            if (branch == null) {
                continue;
            }
            if (isNullBranch(branch)) {
                nullable = true;
            } else {
                valueBranches.add(branch);
            }
        }

        if (valueBranches.isEmpty()) {
            return AvroTypeInfo.builder().avroType(Type.STRING).build();
        }

        if (valueBranches.size() == 1) {
            AvroTypeInfo mapped = mapSchema(valueBranches.get(0), fieldName);
            return nullable ? makeNullable(mapped) : mapped;
        }

        AvroTypeInfo.Builder union = AvroTypeInfo.builder().avroType(Type.UNION);
        if (nullable) {
            union.addUnionType(AvroTypeInfo.builder().avroType(Type.NULL).build());
        }
        for (Schema<?> branch : valueBranches) {
            union.addUnionType(mapSchema(branch, fieldName));
        }
        return union.build();
    }

    /** True when a schema branch represents only the JSON {@code null} type. */
    private boolean isNullBranch(Schema<?> branch) {
        if ("null".equalsIgnoreCase(branch.getType())) {
            return true;
        }
        Set<String> types = branch.getTypes();
        return types != null && types.size() == 1 && types.contains("null");
    }

    /**
     * Resolve the schema type, handling both OpenAPI 3.0 ({@code type: object}) and
     * OpenAPI 3.1, where swagger-parser exposes the type as a JSON Schema set via
     * {@link Schema#getTypes()} and leaves {@link Schema#getType()} null.
     */
    private String resolveType(Schema<?> schema) {
        if (schema.getType() != null) {
            return schema.getType();
        }
        Set<String> types = schema.getTypes();
        if (types != null) {
            for (String t : types) {
                if (t != null && !"null".equalsIgnoreCase(t)) {
                    return t;
                }
            }
        }
        return null;
    }

    /**
     * Map array type.
     */
    private AvroTypeInfo mapArrayType(Schema<?> arraySchema, String fieldName) {
        Schema<?> items = arraySchema.getItems();
        AvroTypeInfo itemType = mapSchema(items, fieldName);

        AvroTypeInfo.Builder builder = AvroTypeInfo.builder()
                .avroType(Type.ARRAY)
                .arrayItemType(itemType);

        String description = arraySchema.getDescription();
        if (description != null && !description.isEmpty()) {
            builder.doc(description);
        }

        return builder.build();
    }

    /**
     * Map object type to record.
     */
    private AvroTypeInfo mapObjectType(Schema<?> schema, String fieldName) {
        Map<String, AvroTypeInfo> fields = new LinkedHashMap<>();
        Map<String, Schema> properties = schema.getProperties();

        if (properties != null) {
            Set<String> requiredFields = schema.getRequired() != null
                    ? new HashSet<>(schema.getRequired())
                    : Collections.emptySet();

            for (Map.Entry<String, Schema> entry : properties.entrySet()) {
                String propName = entry.getKey();
                Schema<?> propSchema = entry.getValue();

                AvroTypeInfo fieldType = mapSchema(propSchema, propName);

                // A deprecated field whose enum cannot be expressed in Avro (e.g. a symbol
                // starting with a digit) is dropped instead of failing the whole conversion:
                // the spec already points consumers to its replacement (x-replaced-by).
                // Valid deprecated fields are kept untouched.
                String invalidSymbol = findInvalidEnumName(fieldType);
                if (invalidSymbol != null && isDeprecated(propSchema)) {
                    warnSkippedDeprecatedField(fieldName, propName, propSchema, invalidSymbol);
                    continue;
                }

                // Make field nullable if not required
                if (!requiredFields.contains(propName)) {
                    fieldType = makeNullable(fieldType);
                }

                fields.put(sanitizeFieldName(propName), fieldType);
            }
        }

        AvroTypeInfo.Builder builder = AvroTypeInfo.builder()
                .avroType(Type.RECORD)
                .recordName(capitalize(fieldName))
                .fields(fields);

        String description = schema.getDescription();
        if (description != null && !description.isEmpty()) {
            builder.doc(description);
        }

        return builder.build();
    }

    /**
     * Return the first enum name or symbol in {@code typeInfo}'s subtree that violates
     * Avro's naming rule, or {@code null} when every enum in it is valid.
     */
    private String findInvalidEnumName(AvroTypeInfo typeInfo) {
        if (typeInfo == null) {
            return null;
        }
        switch (typeInfo.getAvroType()) {
            case ENUM:
                if (!isValidAvroName(typeInfo.getRecordName())) {
                    return typeInfo.getRecordName();
                }
                if (typeInfo.getEnumSymbols() != null) {
                    for (String symbol : typeInfo.getEnumSymbols()) {
                        if (!isValidAvroName(symbol)) {
                            return symbol;
                        }
                    }
                }
                return null;
            case ARRAY:
                return findInvalidEnumName(typeInfo.getArrayItemType());
            case UNION:
                if (typeInfo.getUnionTypes() != null) {
                    for (AvroTypeInfo branch : typeInfo.getUnionTypes()) {
                        String invalid = findInvalidEnumName(branch);
                        if (invalid != null) {
                            return invalid;
                        }
                    }
                }
                return null;
            case RECORD:
                if (typeInfo.getFields() != null) {
                    for (AvroTypeInfo field : typeInfo.getFields().values()) {
                        String invalid = findInvalidEnumName(field);
                        if (invalid != null) {
                            return invalid;
                        }
                    }
                }
                return null;
            default:
                return null;
        }
    }

    private static boolean isValidAvroName(String value) {
        return value != null && AVRO_NAME.matcher(value).matches();
    }

    /**
     * True when a property is marked deprecated in the spec, either on the property itself
     * or on the component it references ({@code $ref}, single-branch {@code allOf}, or the
     * {@code items} of an array). Only an explicit {@code deprecated: true} counts: a
     * {@code DEPRECATED} description or an {@code x-replaced-by} extension alone is not
     * enough, so a field is never silently dropped on a loose hint.
     */
    private boolean isDeprecated(Schema<?> schema) {
        if (schema == null) {
            return false;
        }
        if (hasDeprecationMarker(schema)) {
            return true;
        }
        if (schema.get$ref() != null) {
            return hasDeprecationMarker(resolveRef(schema.get$ref()));
        }
        if (schema.getAllOf() != null && schema.getAllOf().size() == 1) {
            return isDeprecated(schema.getAllOf().get(0));
        }
        if (schema.getItems() != null) {
            return isDeprecated(schema.getItems());
        }
        return false;
    }

    private static boolean hasDeprecationMarker(Schema<?> schema) {
        return schema != null && Boolean.TRUE.equals(schema.getDeprecated());
    }

    /** Collect the {@code x-replaced-by} value from the property or its referenced schema. */
    private String replacedBy(Schema<?> schema) {
        for (Schema<?> candidate : Arrays.asList(schema,
                schema.get$ref() != null ? resolveRef(schema.get$ref()) : null,
                schema.getAllOf() != null && schema.getAllOf().size() == 1 ? schema.getAllOf().get(0) : null,
                schema.getItems())) {
            if (candidate != null && candidate.getExtensions() != null
                    && candidate.getExtensions().get(X_REPLACED_BY) != null) {
                return candidate.getExtensions().get(X_REPLACED_BY).toString();
            }
        }
        return null;
    }

    private void warnSkippedDeprecatedField(String recordName, String propName,
                                            Schema<?> propSchema, String invalidSymbol) {
        String replacement = replacedBy(propSchema);
        System.err.printf("WARN: deprecated field \"%s.%s\" skipped: enum value \"%s\" is not a valid Avro name%s%n",
                capitalize(recordName), propName, invalidSymbol,
                replacement != null ? " (replaced by: " + replacement + ")" : "");
    }

    private Schema<?> resolveRef(String ref) {
        if (openAPI == null || openAPI.getComponents() == null
                || openAPI.getComponents().getSchemas() == null) {
            return null;
        }
        return openAPI.getComponents().getSchemas().get(extractSchemaNameFromRef(ref));
    }

    /**
     * Map reference to another schema.
     */
    private AvroTypeInfo mapReference(String ref, String fieldName) {
        String schemaName = extractSchemaNameFromRef(ref);

        if (openAPI.getComponents() != null &&
                openAPI.getComponents().getSchemas() != null) {
            Schema<?> referencedSchema = openAPI.getComponents().getSchemas().get(schemaName);

            if (referencedSchema != null) {
                // Check if it's an enum
                if (referencedSchema.getEnum() != null && !referencedSchema.getEnum().isEmpty()) {
                    return mapEnum(referencedSchema, schemaName);
                }

                // For objects, create a reference
                return mapSchema(referencedSchema, schemaName);
            }
        }

        return AvroTypeInfo.builder()
                .avroType(Type.STRING)
                .build();
    }

    /**
     * Extract schema name from $ref.
     */
    private String extractSchemaNameFromRef(String ref) {
        if (ref.contains("/")) {
            String[] parts = ref.split("/");
            return parts[parts.length - 1];
        }
        return ref;
    }

    /**
     * Make a type nullable.
     */
    private AvroTypeInfo makeNullable(AvroTypeInfo typeInfo) {
        // Already a union — merge null in rather than nesting unions (Avro forbids that).
        if (typeInfo.getAvroType() == Type.UNION) {
            boolean hasNull = typeInfo.getUnionTypes() != null && typeInfo.getUnionTypes().stream()
                    .anyMatch(t -> t.getAvroType() == Type.NULL);
            if (hasNull) {
                return typeInfo;
            }

            AvroTypeInfo.Builder merged = AvroTypeInfo.builder()
                    .avroType(Type.UNION)
                    .addUnionType(AvroTypeInfo.builder().avroType(Type.NULL).build());
            typeInfo.getUnionTypes().forEach(merged::addUnionType);
            if (typeInfo.getDoc() != null) {
                merged.doc(typeInfo.getDoc());
            }
            return merged.build();
        }

        AvroTypeInfo.Builder builder = AvroTypeInfo.builder()
                .avroType(Type.UNION)
                .addUnionType(AvroTypeInfo.builder().avroType(Type.NULL).build())
                .addUnionType(typeInfo);

        if (typeInfo.getDoc() != null) {
            builder.doc(typeInfo.getDoc());
        }

        return builder.build();
    }

    /**
     * Sanitize field name for Avro.
     */
    private String sanitizeFieldName(String name) {
        return INVALID_CHARS.matcher(name).replaceAll("_");
    }

    /**
     * Capitalize first letter of a string.
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        String sanitized = sanitizeFieldName(str);
        return Character.toUpperCase(sanitized.charAt(0)) + sanitized.substring(1);
    }
}
