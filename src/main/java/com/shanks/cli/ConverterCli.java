package com.shanks.cli;

import com.shanks.converter.JsonToAvroConverter;
import com.shanks.converter.OpenApiToAvroConverter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * CLI orchestrator for converting to Avro schemas.
 *
 * Supports both OpenAPI/Swagger specifications and JSON data files.
 * Automatically detects input file type.
 *
 * This class follows the Single Responsibility Principle by coordinating
 * CLI operations and user interaction.
 */
public class ConverterCli {

    private final JsonToAvroConverter jsonConverter;
    private final OpenApiToAvroConverter openApiConverter;

    /**
     * Constructor with default converters.
     */
    public ConverterCli() {
        this.jsonConverter = new JsonToAvroConverter();
        this.openApiConverter = new OpenApiToAvroConverter();
    }

    /**
     * Constructor with dependency injection for testing.
     *
     * @param jsonConverter the JSON to Avro converter
     * @param openApiConverter the OpenAPI to Avro converter
     */
    public ConverterCli(JsonToAvroConverter jsonConverter, OpenApiToAvroConverter openApiConverter) {
        this.jsonConverter = jsonConverter;
        this.openApiConverter = openApiConverter;
    }

    /**
     * Run the CLI with command-line arguments.
     *
     * @param args command-line arguments
     * @return exit code (0 for success, 1 for error)
     */
    public int run(String[] args) {
        try {
            CliArguments cliArgs = CliArguments.parse(args);
            cliArgs.validateInputExists();
            cliArgs.validateOutputWritable();

            String inputPath = cliArgs.getInputJsonPath();
            String outputPath = cliArgs.getOutputAvscPath();

            if (isOpenApiFile(inputPath)) {
                System.out.println("Converting OpenAPI/Swagger to Avro schema...");
                System.out.println("  Input:  " + inputPath);
                System.out.println("  Output: " + outputPath);

                // Check if unified mode is requested (4th argument)
                boolean unifiedMode = args.length >= 4 && "--unified".equals(args[3]);

                // If args contains a schema name (3rd argument), convert specific schema
                if (args.length >= 3 && !args[2].startsWith("--")) {
                    String schemaName = args[2];
                    System.out.println("  Schema: " + schemaName);

                    if (unifiedMode) {
                        System.out.println("  Mode:   Unified (all types in one file)");
                        openApiConverter.convertUnified(inputPath, schemaName, outputPath);
                    } else {
                        openApiConverter.convert(inputPath, schemaName, outputPath);
                    }
                } else {
                    // Extract output directory and convert all schemas
                    int lastSlash = outputPath.lastIndexOf('/');
                    if (lastSlash == -1) {
                        lastSlash = outputPath.lastIndexOf('\\');
                    }
                    String outputDir = lastSlash > 0 ? outputPath.substring(0, lastSlash) : ".";
                    System.out.println("  Generating all schemas to directory: " + outputDir);
                    openApiConverter.convertAll(inputPath, outputDir);
                }
            } else {
                System.out.println("Converting JSON to Avro schema...");
                System.out.println("  Input:  " + inputPath);
                System.out.println("  Output: " + outputPath);
                jsonConverter.convert(inputPath, outputPath);
            }

            System.out.println("Conversion completed successfully!");
            return 0;

        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println();
            printUsage();
            return 1;

        } catch (Exception e) {
            System.err.println("Error during conversion: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }

    /**
     * Detect if the input file is an OpenAPI/Swagger specification.
     */
    private boolean isOpenApiFile(String filePath) {
        // Check extension
        String lowerPath = filePath.toLowerCase();
        if (lowerPath.endsWith(".yaml") || lowerPath.endsWith(".yml")) {
            return true;
        }

        // Check file content for OpenAPI/Swagger markers
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int linesToCheck = 20;
            int linesChecked = 0;

            while ((line = reader.readLine()) != null && linesChecked < linesToCheck) {
                String trimmed = line.trim().toLowerCase();
                if (trimmed.startsWith("openapi:") ||
                    trimmed.startsWith("swagger:") ||
                    trimmed.contains("\"openapi\"") ||
                    trimmed.contains("\"swagger\"")) {
                    return true;
                }
                linesChecked++;
            }
        } catch (IOException e) {
            // If we can't read the file, assume it's JSON
            return false;
        }

        return false;
    }

    /**
     * Print usage information.
     */
    private void printUsage() {
        System.err.println("Usage:");
        System.err.println("  java -cp target/demo-1.0-SNAPSHOT.jar com.shanks.App <input-file> <output.avsc> [schema-name] [--unified]");
        System.err.println();
        System.err.println("Input file types:");
        System.err.println("  - JSON data file: Will infer types from JSON data");
        System.err.println("  - OpenAPI/Swagger file (.yaml, .yml, .json): Will convert schema definitions");
        System.err.println();
        System.err.println("Options:");
        System.err.println("  --unified: Generate a single file with all type definitions and references (OpenAPI only)");
        System.err.println();
        System.err.println("Examples:");
        System.err.println("  # Convert JSON data to Avro schema");
        System.err.println("  java -cp target/demo-1.0-SNAPSHOT.jar com.shanks.App data.json schema.avsc");
        System.err.println();
        System.err.println("  # Convert specific OpenAPI schema to Avro (separate files per type)");
        System.err.println("  java -cp target/demo-1.0-SNAPSHOT.jar com.shanks.App api.yaml User.avsc User");
        System.err.println();
        System.err.println("  # Convert specific OpenAPI schema to unified Avro (all types in one file)");
        System.err.println("  java -cp target/demo-1.0-SNAPSHOT.jar com.shanks.App api.yaml ResultResponse.avsc ResultResponse --unified");
        System.err.println();
        System.err.println("  # Convert all OpenAPI schemas (output is a directory)");
        System.err.println("  java -cp target/demo-1.0-SNAPSHOT.jar com.shanks.App api.yaml output/schema.avsc");
        System.err.println();
        System.err.println("Or with Maven:");
        System.err.println("  mvn exec:java -Dexec.mainClass=\"com.shanks.App\" -Dexec.args=\"api.yaml ResultResponse.avsc ResultResponse --unified\"");
    }
}
