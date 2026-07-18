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
     * @param functionalPerimeter the functional perimeter name, or null/blank to reset to the default namespace
     */
    public void setFunctionalPerimeter(String functionalPerimeter) {
        this.schemaGenerator.setFunctionalPerimeter(functionalPerimeter);
        this.registrySchemaGenerator.setFunctionalPerimeter(functionalPerimeter);
    }

    /**
     * Convert an OpenAPI file to Avro schema files.
     * Generates one Avro schema file per schema defined in components/schemas.
     *
     * @param inputOpenApiPath path to input OpenAPI file (YAML or JSON)
     * @param outputDirectory  directory where Avro schema files will be written
     * @throws IOException if file operations fail
     */
    public void convertAll(String inputOpenApiPath, String outputDirectory) throws IOException {
        OpenAPI openAPI = parser.parse(inputOpenApiPath);

        if (openAPI.getComponents() == null ||
                openAPI.getComponents().getSchemas() == null ||
                openAPI.getComponents().getSchemas().isEmpty()) {
            throw new IOException("No schemas found in OpenAPI specification");
        }

        File outputDir = new File(outputDirectory);
        if (!outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                throw new IOException("Failed to create output directory: " + outputDirectory);
            }
        }

        OpenApiToAvroTypeMapper mapper = new OpenApiToAvroTypeMapper(openAPI);

        for (Map.Entry<String, Schema> entry : openAPI.getComponents().getSchemas().entrySet()) {
            String schemaName = entry.getKey();
            Schema<?> schema = entry.getValue();

            AvroTypeInfo typeInfo = mapper.mapSchema(schema, schemaName);

            // Only generate files for record types (not simple enums)
            if (typeInfo.getAvroType() == Type.RECORD) {
                String outputFileName = schemaName + ".avsc";
                String outputPath = new File(outputDir, outputFileName).getPath();

                String schemaJson = schemaGenerator.generateSchemaJson(typeInfo, schemaName);
                SchemaFileWriter.write(schemaJson, outputPath);

                System.out.println("Generated: " + outputFileName);
            }
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
        AvroTypeInfo typeInfo = loadAndMap(inputOpenApiPath, schemaName);
        String schemaJson = schemaGenerator.generateSchemaJson(typeInfo, schemaName);
        SchemaFileWriter.write(schemaJson, outputAvscPath);
    }

    /**
     * Convert a specific schema from an OpenAPI file to an IBM Schema Registry compatible Avro schema file.
     * Produces a single self-contained JSON object with all nested types embedded inline.
     *
     * @param inputOpenApiPath path to input OpenAPI file (YAML or JSON)
     * @param schemaName       name of the schema in components/schemas to convert
     * @param outputAvscPath   path to output AVSC file
     * @throws IOException if file operations fail
     */
    public void convertRegistry(String inputOpenApiPath, String schemaName, String outputAvscPath) throws IOException {
        AvroTypeInfo typeInfo = loadAndMap(inputOpenApiPath, schemaName);
        String schemaJson = registrySchemaGenerator.generateRegistrySchema(typeInfo, schemaName);
        SchemaFileWriter.write(schemaJson, outputAvscPath);
    }

    /**
     * Convert the requestBody schema of a specific OpenAPI operation to an Avro schema file.
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
        SchemaFileWriter.write(schemaJson, outputAvscPath);
    }

    /**
     * Convert the requestBody schema of a specific OpenAPI operation to an IBM Schema Registry
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
        SchemaFileWriter.write(schemaJson, outputAvscPath);
    }

    /**
     * Parse the OpenAPI file, find the named schema, and map it to AvroTypeInfo.
     */
    private AvroTypeInfo loadAndMap(String inputOpenApiPath, String schemaName) throws IOException {
        OpenAPI openAPI = parser.parse(inputOpenApiPath);

        if (openAPI.getComponents() == null || openAPI.getComponents().getSchemas() == null) {
            throw new IOException("No schemas found in OpenAPI specification");
        }

        Schema<?> schema = openAPI.getComponents().getSchemas().get(schemaName);
        if (schema == null) {
            throw new IOException("Schema '" + schemaName + "' not found in OpenAPI specification. " +
                    "Available schemas: " + openAPI.getComponents().getSchemas().keySet());
        }

        return new OpenApiToAvroTypeMapper(openAPI).mapSchema(schema, schemaName);
    }

    /**
     * Parse the OpenAPI file, locate the requestBody schema of the given path/method operation,
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
        return new RequestBodySchema(typeInfo, schemaName);
    }

    /**
     * Get the operation for the given HTTP method from a path item.
     */
    private Operation getOperation(PathItem pathItem, String httpMethod) {
        switch (httpMethod.toUpperCase()) {
            case "GET": return pathItem.getGet();
            case "POST": return pathItem.getPost();
            case "PUT": return pathItem.getPut();
            case "PATCH": return pathItem.getPatch();
            case "DELETE": return pathItem.getDelete();
            case "HEAD": return pathItem.getHead();
            case "OPTIONS": return pathItem.getOptions();
            case "TRACE": return pathItem.getTrace();
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + httpMethod);
        }
    }

    /**
     * Derive a record name for a requestBody schema: prefer the operationId, otherwise
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
     * Holds the resolved AvroTypeInfo and derived record name for a requestBody conversion.
     */
    private static final class RequestBodySchema {
        final AvroTypeInfo typeInfo;
        final String schemaName;

        RequestBodySchema(AvroTypeInfo typeInfo, String schemaName) {
            this.typeInfo = typeInfo;
            this.schemaName = schemaName;
        }
    }

    /**
     * Get the schema generator (for testing).
     */
    public SchemaGenerator getSchemaGenerator() {
        return schemaGenerator;
    }
}
