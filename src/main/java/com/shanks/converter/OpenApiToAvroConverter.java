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
    private final RegistrySchemaGenerator registrySchemaGenerator;
    private String envelopeName = "default";

    /**
     * Constructor with default components.
     */
    public OpenApiToAvroConverter() {
        this.parser = new OpenApiParser();
        this.schemaGenerator = new SchemaGenerator();
        this.registrySchemaGenerator = new RegistrySchemaGenerator();
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
        this.registrySchemaGenerator = new RegistrySchemaGenerator();
    }

    /**
     * Set whether to include doc fields in generated schemas.
     *
     * @param includeDoc true to include doc fields from OpenAPI descriptions
     */
    public void setIncludeDoc(boolean includeDoc) {
        this.schemaGenerator.setIncludeDoc(includeDoc);
        this.registrySchemaGenerator.setIncludeDoc(includeDoc);
    }

    /**
     * Set the functional perimeter to append to the default namespace
     * (e.g. "users" produces "com.shanks.generated.users").
     *
     * @param functionalPerimeter the functional perimeter name, or null/blank to
     *                            reset to the default namespace
     */
    public void setFunctionalPerimeter(String functionalPerimeter) {
        this.schemaGenerator.setFunctionalPerimeter(functionalPerimeter);
        this.registrySchemaGenerator.setFunctionalPerimeter(functionalPerimeter);
    }

    /**
     * Set the envelope template used to build the notif-wrapped (.webhook.avsc)
     * output.
     *
     * @param envelopeName the envelope template name (see
     *                     {@link NotifWrapperGenerator})
     */
    public void setEnvelope(String envelopeName) {
        this.envelopeName = envelopeName;
    }

    /**
     * Convert an OpenAPI file to Avro schema files.
     * Generates one Avro schema file per schema defined in components/schemas,
     * plus one per webhook operation's requestBody payload.
     *
     * @param inputOpenApiPath path to input OpenAPI file (YAML or JSON)
     * @param outputDirectory  directory where Avro schema files will be written
     * @throws IOException if file operations fail
     */
    public void convertAll(String inputOpenApiPath, String outputDirectory) throws IOException {
        OpenAPI openAPI = parser.parse(inputOpenApiPath);
        Map<String, Schema<?>> schemas = collectAllSchemas(openAPI);

        if (schemas.isEmpty()) {
            throw new IOException("No schemas found in OpenAPI specification "
                    + "(neither components/schemas nor webhooks)");
        }

        File outputDir = new File(outputDirectory);
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Failed to create output directory: " + outputDirectory);
        }

        OpenApiToAvroTypeMapper mapper = new OpenApiToAvroTypeMapper(openAPI);
        String apiVersion = safeVersion(openAPI);
        for (Map.Entry<String, Schema<?>> entry : schemas.entrySet()) {
            generateRecordFile(mapper, entry.getValue(), entry.getKey(), outputDir, apiVersion);
        }
    }

    /** @return the API's own version (info.version), or null if unavailable. */
    private static String safeVersion(OpenAPI openAPI) {
        return openAPI.getInfo() == null ? null : openAPI.getInfo().getVersion();
    }

    /**
     * Parse the OpenAPI file and collect every named schema it exposes, for
     * interactive per-file selection (mass conversion mode): the same set that
     * {@link #convertAll} writes to disk (components/schemas plus each webhook
     * operation's requestBody payload), without writing anything.
     *
     * @param inputOpenApiPath path to input OpenAPI file (YAML or JSON)
     * @throws IOException if the file cannot be parsed, or if it exposes no schema
     *                      at all (neither components/schemas nor webhooks)
     */
    public SpecSchemas loadSchemas(String inputOpenApiPath) throws IOException {
        OpenAPI openAPI = parser.parse(inputOpenApiPath);
        Map<String, Schema<?>> schemas = collectAllSchemas(openAPI);
        if (schemas.isEmpty()) {
            throw new IOException("No schemas found in OpenAPI specification "
                    + "(neither components/schemas nor webhooks)");
        }
        return new SpecSchemas(new OpenApiToAvroTypeMapper(openAPI), schemas, safeVersion(openAPI));
    }

    /**
     * Convert one schema previously collected by {@link #loadSchemas} to an Avro
     * schema file, using whichever mode/flags are currently set on this converter
     * (see {@link #setIncludeDoc}, {@link #setFunctionalPerimeter}, {@link #setEnvelope}).
     *
     * @param spec           the parsed spec, as returned by {@link #loadSchemas}
     * @param schemaName     the schema name to convert (one of {@code spec.getSchemaNames()})
     * @param outputAvscPath path to output AVSC file
     * @param registryMode   true to generate a self-contained registry schema, false for standard mode
     * @throws IOException if the schema name is unknown, if it does not resolve to an
     *                      Avro record (bare enums/scalars can't be converted alone), or
     *                      if writing the output files fails
     */
    public void convertNamed(SpecSchemas spec, String schemaName, String outputAvscPath, boolean registryMode)
            throws IOException {
        Schema<?> schema = spec.schemasByName.get(schemaName);
        if (schema == null) {
            throw new IOException("Schema '" + schemaName + "' not found. Available: "
                    + spec.schemasByName.keySet());
        }

        AvroTypeInfo typeInfo = spec.mapper.mapSchema(schema, schemaName);
        if (typeInfo.getAvroType() != Type.RECORD) {
            throw new IOException("'" + schemaName + "' resolves to a " + typeInfo.getAvroType()
                    + ", not a record — bare enums/scalars can't be converted on their own");
        }

        String schemaJson = registryMode
                ? registrySchemaGenerator.generateRegistrySchema(typeInfo, schemaName)
                : schemaGenerator.generateSchemaJson(typeInfo, schemaName);
        SchemaFileWriter.write(schemaJson, outputAvscPath, envelopeName, spec.apiVersion);
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
     * Immutable handle on a parsed spec's available named schemas, returned by
     * {@link #loadSchemas} and consumed by {@link #convertNamed} — avoids re-parsing
     * the file between "list the available schemas" and "convert the chosen one".
     */
    public static final class SpecSchemas {
        private final OpenApiToAvroTypeMapper mapper;
        private final Map<String, Schema<?>> schemasByName;
        private final String apiVersion;

        private SpecSchemas(OpenApiToAvroTypeMapper mapper, Map<String, Schema<?>> schemasByName,
                String apiVersion) {
            this.mapper = mapper;
            this.schemasByName = schemasByName;
            this.apiVersion = apiVersion;
        }

        public List<String> getSchemaNames() {
            return new ArrayList<>(schemasByName.keySet());
        }
    }

    /**
     * Convert a specific schema from an OpenAPI file to an Avro schema file.
     *
     * @param inputOpenApiPath path to input OpenAPI file (YAML or JSON)
     * @param schemaName       name of the schema in components/schemas to convert
     * @param outputAvscPath   path to output AVSC file
     * @throws IOException if file operations fail
     */
    public void convert(String inputOpenApiPath, String schemaName, String outputAvscPath) throws IOException {
        MappedSchema resolved = loadAndMap(inputOpenApiPath, schemaName);
        String schemaJson = schemaGenerator.generateSchemaJson(resolved.typeInfo, schemaName);
        SchemaFileWriter.write(schemaJson, outputAvscPath, envelopeName, resolved.apiVersion);
    }

    /**
     * Convert a specific schema from an OpenAPI file to an IBM Schema Registry
     * compatible Avro schema file.
     * Produces a single self-contained JSON object with all nested types embedded
     * inline.
     *
     * @param inputOpenApiPath path to input OpenAPI file (YAML or JSON)
     * @param schemaName       name of the schema in components/schemas to convert
     * @param outputAvscPath   path to output AVSC file
     * @throws IOException if file operations fail
     */
    public void convertRegistry(String inputOpenApiPath, String schemaName, String outputAvscPath) throws IOException {
        MappedSchema resolved = loadAndMap(inputOpenApiPath, schemaName);
        String schemaJson = registrySchemaGenerator.generateRegistrySchema(resolved.typeInfo, schemaName);
        SchemaFileWriter.write(schemaJson, outputAvscPath, envelopeName, resolved.apiVersion);
    }

    /**
     * Convert the requestBody schema of a specific OpenAPI operation to an Avro
     * schema file.
     *
     * @param inputOpenApiPath path to input OpenAPI file (YAML or JSON)
     * @param pathKey          the OpenAPI path (e.g. "/users")
     * @param httpMethod       the HTTP method of the operation (e.g. "POST")
     * @param outputAvscPath   path to output AVSC file
     * @throws IOException if file operations fail
     */
    public void convertFromRequestBody(String inputOpenApiPath, String pathKey, String httpMethod,
            String outputAvscPath) throws IOException {
        RequestBodySchema resolved = loadAndMapFromRequestBody(inputOpenApiPath, pathKey, httpMethod);
        String schemaJson = schemaGenerator.generateSchemaJson(resolved.typeInfo, resolved.schemaName);
        SchemaFileWriter.write(schemaJson, outputAvscPath, envelopeName, resolved.apiVersion);
    }

    /**
     * Convert the requestBody schema of a specific OpenAPI operation to an IBM
     * Schema Registry
     * compatible Avro schema file.
     *
     * @param inputOpenApiPath path to input OpenAPI file (YAML or JSON)
     * @param pathKey          the OpenAPI path (e.g. "/users")
     * @param httpMethod       the HTTP method of the operation (e.g. "POST")
     * @param outputAvscPath   path to output AVSC file
     * @throws IOException if file operations fail
     */
    public void convertRegistryFromRequestBody(String inputOpenApiPath, String pathKey, String httpMethod,
            String outputAvscPath) throws IOException {
        RequestBodySchema resolved = loadAndMapFromRequestBody(inputOpenApiPath, pathKey, httpMethod);
        String schemaJson = registrySchemaGenerator.generateRegistrySchema(resolved.typeInfo, resolved.schemaName);
        SchemaFileWriter.write(schemaJson, outputAvscPath, envelopeName, resolved.apiVersion);
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
     * {@code convert}/{@code convertRegistry} conversion.
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
     * Map a single OpenAPI schema and, if it resolves to a record, write its
     * .avsc (+ .min.avsc / .webhook.avsc) files. Bare enums and scalars are
     * skipped.
     */
    private void generateRecordFile(OpenApiToAvroTypeMapper mapper, Schema<?> schema,
            String schemaName, File outputDir, String apiVersion) throws IOException {
        if (schema == null) {
            return;
        }
        AvroTypeInfo typeInfo = mapper.mapSchema(schema, schemaName);
        if (typeInfo.getAvroType() != Type.RECORD) {
            return;
        }

        String outputPath = new File(outputDir, schemaName + ".avsc").getPath();
        String schemaJson = schemaGenerator.generateSchemaJson(typeInfo, schemaName);
        SchemaFileWriter.write(schemaJson, outputPath, envelopeName, apiVersion);
        System.out.println("Generated: " + schemaName + ".avsc");
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

    /**
     * Get the schema generator (for testing).
     */
    public SchemaGenerator getSchemaGenerator() {
        return schemaGenerator;
    }
}
