package com.shanks.converter;

import com.shanks.mapper.OpenApiToAvroTypeMapper;
import com.shanks.model.AvroTypeInfo;
import com.shanks.parser.OpenApiParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import org.apache.avro.Schema.Type;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Converter for transforming OpenAPI/Swagger specifications to Avro schemas.
 *
 * This class follows the Single Responsibility Principle by coordinating
 * between the parser, mapper, and schema generator components.
 * It also follows Dependency Inversion by depending on abstractions.
 */
public class OpenApiToAvroConverter {

    private final OpenApiParser parser;
    private final SchemaGenerator schemaGenerator;

    /**
     * Constructor with default components.
     */
    public OpenApiToAvroConverter() {
        this(new OpenApiParser(), new SchemaGenerator());
    }

    /**
     * Constructor with dependency injection for testing.
     *
     * @param parser          the OpenAPI parser
     * @param schemaGenerator the schema generator
     */
    public OpenApiToAvroConverter(OpenApiParser parser, SchemaGenerator schemaGenerator) {
        this.parser = parser;
        this.schemaGenerator = schemaGenerator;
    }

    /**
     * Convert an OpenAPI file to Avro schema files.
     * Generates one Avro schema file per record schema defined in components/schemas,
     * plus one per webhook operation's requestBody payload. Bare enums and scalars are
     * skipped.
     *
     * @param inputOpenApiPath path to input OpenAPI file (YAML or JSON)
     * @param outputDirectory  directory where Avro schema files will be written
     * @param options          doc fields, namespace and envelope to apply
     * @throws IOException if the file cannot be parsed, it exposes no record schema, or
     *                      writing fails
     */
    public void convertAll(String inputOpenApiPath, String outputDirectory, ConversionOptions options)
            throws IOException {
        NotifWrapperGenerator.validate(options.envelope());
        SpecSchemas spec = loadSchemas(inputOpenApiPath);

        File outputDir = new File(outputDirectory);
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Failed to create output directory: " + outputDirectory);
        }

        for (String schemaName : spec.getSchemaNames()) {
            String outputPath = new File(outputDir, schemaName + ".avsc").getPath();
            String written = convertNamed(spec, schemaName, outputPath, options);
            System.out.println("Generated: " + new File(written).getName());
        }
    }

    /** @return the API's own version (info.version), or null if unavailable. */
    private static String safeVersion(OpenAPI openAPI) {
        return openAPI.getInfo() == null ? null : openAPI.getInfo().getVersion();
    }

    /**
     * Parse the OpenAPI file and collect every record schema it exposes: the same set that
     * {@link #convertAll} writes to disk (components/schemas plus each webhook operation's
     * requestBody payload), without writing anything. Bare enums and scalars are left out,
     * since they can't be converted on their own.
     *
     * @param inputOpenApiPath path to input OpenAPI file (YAML or JSON)
     * @throws IOException if the file cannot be parsed, or if it exposes no record schema
     */
    public SpecSchemas loadSchemas(String inputOpenApiPath) throws IOException {
        OpenAPI openAPI = parser.parse(inputOpenApiPath);
        Map<String, Schema<?>> schemas = collectAllSchemas(openAPI);
        if (schemas.isEmpty()) {
            throw new IOException("No schemas found in OpenAPI specification "
                    + "(neither components/schemas nor webhooks)");
        }

        OpenApiToAvroTypeMapper mapper = new OpenApiToAvroTypeMapper(openAPI);
        Map<String, AvroTypeInfo> records = new LinkedHashMap<>();
        for (Map.Entry<String, Schema<?>> entry : schemas.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            AvroTypeInfo typeInfo = mapper.mapSchema(entry.getValue(), entry.getKey());
            if (typeInfo.getAvroType() == Type.RECORD) {
                records.put(entry.getKey(), typeInfo);
            }
        }
        if (records.isEmpty()) {
            throw new IOException("No record schemas found in OpenAPI specification "
                    + "(only bare enums/scalars: " + schemas.keySet() + ")");
        }
        return new SpecSchemas(records, safeVersion(openAPI));
    }

    /**
     * Convert one schema previously collected by {@link #loadSchemas} to an Avro
     * schema file.
     *
     * @param spec           the parsed spec, as returned by {@link #loadSchemas}
     * @param schemaName     the schema name to convert (one of {@code spec.getSchemaNames()})
     * @param outputAvscPath path to output AVSC file
     * @param options        doc fields, namespace and envelope to apply
     * @return the path of the main .avsc file actually written (version-suffixed when the
     *         spec declares info.version)
     * @throws IOException if the schema name is unknown or writing the output files fails
     */
    public String convertNamed(SpecSchemas spec, String schemaName, String outputAvscPath,
            ConversionOptions options) throws IOException {
        AvroTypeInfo typeInfo = spec.recordsByName.get(schemaName);
        if (typeInfo == null) {
            throw new IOException("Schema '" + schemaName + "' not found. Available: "
                    + spec.recordsByName.keySet());
        }
        return write(typeInfo, schemaName, outputAvscPath, options, spec.apiVersion);
    }

    /**
     * Collect every named schema an OpenAPI spec exposes: components/schemas plus,
     * for each webhook operation (OpenAPI 3.1 {@code webhooks:}), its requestBody
     * payload — the same name derivation as before (operationId capitalized, else
     * {@code <Webhook><Method>}). Response schemas are intentionally not included.
     */
    private Map<String, Schema<?>> collectAllSchemas(OpenAPI openAPI) {
        Map<String, Schema<?>> schemas = new LinkedHashMap<>();

        if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null) {
            for (Map.Entry<String, Schema> entry : openAPI.getComponents().getSchemas().entrySet()) {
                schemas.put(entry.getKey(), entry.getValue());
            }
        }

        if (openAPI.getWebhooks() != null) {
            for (Map.Entry<String, PathItem> webhook : openAPI.getWebhooks().entrySet()) {
                PathItem pathItem = webhook.getValue();
                if (pathItem == null) {
                    continue;
                }
                for (Map.Entry<PathItem.HttpMethod, Operation> op : pathItem.readOperationsMap().entrySet()) {
                    Operation operation = op.getValue();
                    String baseName = (operation.getOperationId() != null
                            && !operation.getOperationId().trim().isEmpty())
                                    ? capitalize(operation.getOperationId())
                                    : capitalize(webhook.getKey()) + capitalize(op.getKey().name().toLowerCase());

                    Schema<?> requestSchema = extractSchema(
                            operation.getRequestBody() == null ? null : operation.getRequestBody().getContent());
                    if (requestSchema != null) {
                        schemas.put(baseName, requestSchema);
                    }
                }
            }
        }

        return schemas;
    }

    /**
     * Immutable handle on a parsed spec's record schemas, already mapped, returned by
     * {@link #loadSchemas} and consumed by {@link #convertNamed} — avoids re-parsing
     * the file between "list the available schemas" and "convert the chosen one".
     */
    public static final class SpecSchemas {
        private final Map<String, AvroTypeInfo> recordsByName;
        private final String apiVersion;

        private SpecSchemas(Map<String, AvroTypeInfo> recordsByName, String apiVersion) {
            this.recordsByName = recordsByName;
            this.apiVersion = apiVersion;
        }

        public List<String> getSchemaNames() {
            return new ArrayList<>(recordsByName.keySet());
        }
    }

    /**
     * Convert a specific schema from an OpenAPI file to an Avro schema file.
     *
     * @param inputOpenApiPath path to input OpenAPI file (YAML or JSON)
     * @param schemaName       name of the schema in components/schemas to convert
     * @param outputAvscPath   path to output AVSC file
     * @param options          doc fields, namespace and envelope to apply
     * @throws IOException if file operations fail
     */
    public void convert(String inputOpenApiPath, String schemaName, String outputAvscPath,
            ConversionOptions options) throws IOException {
        NotifWrapperGenerator.validate(options.envelope());
        MappedSchema resolved = loadAndMap(inputOpenApiPath, schemaName);
        write(resolved.typeInfo, schemaName, outputAvscPath, options, resolved.apiVersion);
    }

    /**
     * Convert the requestBody schema of a specific OpenAPI operation to an Avro
     * schema file.
     *
     * @param inputOpenApiPath path to input OpenAPI file (YAML or JSON)
     * @param pathKey          the OpenAPI path (e.g. "/users")
     * @param httpMethod       the HTTP method of the operation (e.g. "POST")
     * @param outputAvscPath   path to output AVSC file
     * @param options          doc fields, namespace and envelope to apply
     * @throws IOException if file operations fail
     */
    public void convertFromRequestBody(String inputOpenApiPath, String pathKey, String httpMethod,
            String outputAvscPath, ConversionOptions options) throws IOException {
        NotifWrapperGenerator.validate(options.envelope());
        RequestBodySchema resolved = loadAndMapFromRequestBody(inputOpenApiPath, pathKey, httpMethod);
        write(resolved.typeInfo, resolved.schemaName, outputAvscPath, options, resolved.apiVersion);
    }

    /** Generate the schema JSON and write it (plus its companions); returns the main file path. */
    private String write(AvroTypeInfo typeInfo, String schemaName, String outputAvscPath,
            ConversionOptions options, String apiVersion) throws IOException {
        String schemaJson = schemaGenerator.generateSchemaJson(typeInfo, schemaName, options);
        return SchemaFileWriter.write(schemaJson, outputAvscPath, options.envelope(), apiVersion);
    }

    /**
     * Parse the OpenAPI file, find the named schema, and map it to AvroTypeInfo.
     */
    private MappedSchema loadAndMap(String inputOpenApiPath, String schemaName) throws IOException {
        OpenAPI openAPI = parser.parse(inputOpenApiPath);

        if (openAPI.getComponents() == null || openAPI.getComponents().getSchemas() == null) {
            throw new IOException("No schemas found in OpenAPI specification");
        }

        Schema<?> schema = openAPI.getComponents().getSchemas().get(schemaName);
        if (schema == null) {
            throw new IOException("Schema '" + schemaName + "' not found in OpenAPI specification. " +
                    "Available schemas: " + openAPI.getComponents().getSchemas().keySet());
        }

        AvroTypeInfo typeInfo = new OpenApiToAvroTypeMapper(openAPI).mapSchema(schema, schemaName);
        return new MappedSchema(typeInfo, safeVersion(openAPI));
    }

    /**
     * Holds the resolved AvroTypeInfo and the source API's version for a
     * {@code convert} conversion.
     */
    private static final class MappedSchema {
        final AvroTypeInfo typeInfo;
        final String apiVersion;

        MappedSchema(AvroTypeInfo typeInfo, String apiVersion) {
            this.typeInfo = typeInfo;
            this.apiVersion = apiVersion;
        }
    }

    /**
     * Parse the OpenAPI file, locate the requestBody schema of the given
     * path/method operation,
     * and map it to AvroTypeInfo.
     */
    private RequestBodySchema loadAndMapFromRequestBody(String inputOpenApiPath, String pathKey,
            String httpMethod) throws IOException {
        OpenAPI openAPI = parser.parse(inputOpenApiPath);

        if (openAPI.getPaths() == null || openAPI.getPaths().get(pathKey) == null) {
            throw new IOException("Path '" + pathKey + "' not found in OpenAPI specification. " +
                    "Available paths: " + (openAPI.getPaths() == null ? "none" : openAPI.getPaths().keySet()));
        }

        PathItem pathItem = openAPI.getPaths().get(pathKey);
        Operation operation = getOperation(pathItem, httpMethod);
        if (operation == null) {
            throw new IOException("HTTP method '" + httpMethod + "' not found for path '" + pathKey + "'");
        }

        RequestBody requestBody = operation.getRequestBody();
        if (requestBody == null || requestBody.getContent() == null || requestBody.getContent().isEmpty()) {
            throw new IOException("No requestBody defined for " + httpMethod.toUpperCase() + " " + pathKey);
        }

        Content content = requestBody.getContent();
        MediaType mediaType = content.get("application/json");
        if (mediaType == null) {
            // Fall back to the first available media type (e.g. application/xml only)
            mediaType = content.values().iterator().next();
        }

        Schema<?> schema = mediaType.getSchema();
        if (schema == null) {
            throw new IOException("requestBody for " + httpMethod.toUpperCase() + " " + pathKey +
                    " has no schema");
        }

        String schemaName = deriveRequestBodySchemaName(operation, pathKey, httpMethod);
        AvroTypeInfo typeInfo = new OpenApiToAvroTypeMapper(openAPI).mapSchema(schema, schemaName);
        return new RequestBodySchema(typeInfo, schemaName, safeVersion(openAPI));
    }

    /**
     * Get the operation for the given HTTP method from a path item.
     */
    private Operation getOperation(PathItem pathItem, String httpMethod) {
        switch (httpMethod.toUpperCase()) {
            case "GET":
                return pathItem.getGet();
            case "POST":
                return pathItem.getPost();
            case "PUT":
                return pathItem.getPut();
            case "PATCH":
                return pathItem.getPatch();
            case "DELETE":
                return pathItem.getDelete();
            case "HEAD":
                return pathItem.getHead();
            case "OPTIONS":
                return pathItem.getOptions();
            case "TRACE":
                return pathItem.getTrace();
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + httpMethod);
        }
    }

    /**
     * Derive a record name for a requestBody schema: prefer the operationId,
     * otherwise
     * fall back to a name built from the path and HTTP method.
     */
    private String deriveRequestBodySchemaName(Operation operation, String pathKey, String httpMethod) {
        if (operation.getOperationId() != null && !operation.getOperationId().trim().isEmpty()) {
            return capitalize(operation.getOperationId());
        }

        String sanitizedPath = INVALID_NAME_CHARS.matcher(pathKey).replaceAll(" ");
        StringBuilder name = new StringBuilder();
        for (String part : sanitizedPath.trim().split("\\s+")) {
            if (!part.isEmpty()) {
                name.append(capitalize(part));
            }
        }
        name.append(capitalize(httpMethod.toLowerCase()));
        return name.toString();
    }

    private static final Pattern INVALID_NAME_CHARS = Pattern.compile("[^a-zA-Z0-9]+");

    /** Capitalize first letter of a string, stripping invalid characters. */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        String sanitized = INVALID_NAME_CHARS.matcher(str).replaceAll("");
        if (sanitized.isEmpty()) {
            return sanitized;
        }
        return Character.toUpperCase(sanitized.charAt(0)) + sanitized.substring(1);
    }

    /**
     * Holds the resolved AvroTypeInfo and derived record name for a requestBody
     * conversion.
     */
    private static final class RequestBodySchema {
        final AvroTypeInfo typeInfo;
        final String schemaName;
        final String apiVersion;

        RequestBodySchema(AvroTypeInfo typeInfo, String schemaName, String apiVersion) {
            this.typeInfo = typeInfo;
            this.schemaName = schemaName;
            this.apiVersion = apiVersion;
        }
    }

    /**
     * Pull the schema from application/json, falling back to the first available
     * media type.
     */
    private Schema<?> extractSchema(Content content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        MediaType mediaType = content.get("application/json");
        if (mediaType == null) {
            mediaType = content.values().iterator().next();
        }
        return mediaType == null ? null : mediaType.getSchema();
    }
}
