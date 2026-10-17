package com.shanks.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shanks.model.AvroTypeInfo;
import com.shanks.util.EnumDetector;
import com.shanks.util.UuidDetector;
import org.apache.avro.Schema;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Main converter orchestrating JSON to Avro schema conversion.
 *
 * This class follows the Single Responsibility Principle by coordinating
 * between components rather than doing the work itself.
 * It also follows Dependency Inversion by depending on abstractions.
 */
public class JsonToAvroConverter {

    private final ObjectMapper objectMapper;
    private final TypeInferenceEngine inferenceEngine;
    private final SchemaGenerator schemaGenerator;

    /**
     * Constructor with default type detectors.
     */
    public JsonToAvroConverter() {
        this(new ObjectMapper(),
                new TypeInferenceEngine(Arrays.asList(new UuidDetector(), new EnumDetector())),
                new SchemaGenerator());
    }

    /**
     * Constructor with dependency injection for testing.
     *
     * @param objectMapper the JSON object mapper
     * @param inferenceEngine the type inference engine
     * @param schemaGenerator the schema generator
     */
    public JsonToAvroConverter(ObjectMapper objectMapper,
                               TypeInferenceEngine inferenceEngine,
                               SchemaGenerator schemaGenerator) {
        this.objectMapper = objectMapper;
        this.inferenceEngine = inferenceEngine;
        this.schemaGenerator = schemaGenerator;
    }

    /**
     * Convert a JSON file to an Avro schema file.
     *
     * @param inputJsonPath path to input JSON file
     * @param outputAvscPath path to output AVSC file
     * @param options namespace and envelope to apply (a JSON data file has no
     *                descriptions, so {@code includeDoc} has no effect)
     * @throws IOException if file operations fail
     */
    public void convert(String inputJsonPath, String outputAvscPath, ConversionOptions options)
            throws IOException {
        NotifWrapperGenerator.validate(options.envelope());

        File inputFile = new File(inputJsonPath);
        if (!inputFile.exists()) {
            throw new IOException("Input JSON file not found: " + inputJsonPath);
        }

        JsonNode rootNode = objectMapper.readTree(inputFile);
        AvroTypeInfo typeInfo = inferenceEngine.inferType(rootNode, "Root");
        String prettyJson = schemaGenerator.generateSchemaJson(typeInfo, "RootRecord", options);
        SchemaFileWriter.write(prettyJson, outputAvscPath, options.envelope(), null);
    }

    /**
     * Convert a JSON string to an Avro schema.
     *
     * @param jsonString the JSON string
     * @param recordName the name for the root record
     * @return the generated Avro schema
     * @throws IOException if JSON parsing fails
     */
    public Schema convertFromString(String jsonString, String recordName) throws IOException {
        JsonNode rootNode = objectMapper.readTree(jsonString);
        AvroTypeInfo typeInfo = inferenceEngine.inferType(rootNode, recordName);
        return schemaGenerator.generateSchema(typeInfo, recordName);
    }
}
