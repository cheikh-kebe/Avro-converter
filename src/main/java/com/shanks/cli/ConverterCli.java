package com.shanks.cli;

import com.shanks.converter.AvroSchemaValidationException;
import com.shanks.converter.ConversionOptions;
import com.shanks.converter.InvalidEnvelopeException;
import com.shanks.converter.JsonToAvroConverter;
import com.shanks.converter.OpenApiToAvroConverter;
import com.shanks.serializer.AvroBinaryEncoder;
import com.shanks.serializer.AvroJsonGenerator;
import com.shanks.serializer.SchemaLoader;
import org.apache.avro.Schema;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CLI orchestrator for converting to Avro schemas.
 *
 * Supports both OpenAPI/Swagger specifications and JSON data files.
 * Automatically detects input file type.
 *
 * Sub-commands:
 *   generate - Generate sample JSON from an Avro schema
 *   encode   - Encode JSON to Avro binary format
 *
 * This class follows the Single Responsibility Principle by coordinating
 * CLI operations and user interaction.
 */
public class ConverterCli {

    private final JsonToAvroConverter jsonConverter;
    private final OpenApiToAvroConverter openApiConverter;
    private final InputStream stdin;

    /**
     * Constructor with default converters.
     */
    public ConverterCli() {
        this(new JsonToAvroConverter(), new OpenApiToAvroConverter(), System.in);
    }

    /**
     * Constructor with dependency injection for testing.
     *
     * @param jsonConverter the JSON to Avro converter
     * @param openApiConverter the OpenAPI to Avro converter
     */
    public ConverterCli(JsonToAvroConverter jsonConverter, OpenApiToAvroConverter openApiConverter) {
        this(jsonConverter, openApiConverter, System.in);
    }

    /**
     * Constructor with dependency injection for testing, including the input
     * stream used to drive the interactive directory (mass conversion) prompts.
     *
     * @param jsonConverter the JSON to Avro converter
     * @param openApiConverter the OpenAPI to Avro converter
     * @param stdin the input stream to read interactive prompt answers from
     */
    public ConverterCli(JsonToAvroConverter jsonConverter, OpenApiToAvroConverter openApiConverter,
            InputStream stdin) {
        this.jsonConverter = jsonConverter;
        this.openApiConverter = openApiConverter;
        this.stdin = stdin;
    }

    /**
     * Run the CLI with command-line arguments.
     *
     * @param args command-line arguments
     * @return exit code (0 for success, 1 for error)
     */
    public int run(String[] args) {
        boolean showStackTrace = args != null && Arrays.asList(args).contains("--stacktrace");

        try {
            if (args != null && args.length > 0) {
                String command = args[0];
                if ("generate".equals(command)) {
                    return runGenerate(args);
                }
                if ("encode".equals(command)) {
                    return runEncode(args);
                }
            }

            return runConvert(args);

        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println();
            printUsage();
            return 1;

        } catch (AvroSchemaValidationException | InvalidEnvelopeException e) {
            System.err.println("Error: " + e.getMessage());
            if (showStackTrace) {
                e.printStackTrace();
            }
            return 1;

        } catch (Exception e) {
            System.err.println("Error during operation: " + e.getMessage());
            if (showStackTrace) {
                e.printStackTrace();
            } else {
                System.err.println("(run with --stacktrace for the full Java stack trace)");
            }
            return 1;
        }
    }

    /**
     * Run the 'generate' sub-command: generate sample JSON from an Avro schema.
     * Usage: generate <schema.avsc> <output.json> [SchemaName]
     */
    private int runGenerate(String[] args) throws Exception {
        if (args.length < 3) {
            throw new IllegalArgumentException(
                    "Usage: generate <schema.avsc> <output.json> [SchemaName]");
        }

        String schemaPath = args[1];
        String outputPath = args[2];
        String schemaName = args.length >= 4 ? args[3] : null;

        SchemaLoader loader = new SchemaLoader();
        Schema schema = loader.load(schemaPath, schemaName);

        System.out.println("Generating sample JSON from Avro schema...");
        System.out.println("  Schema: " + schemaPath);
        System.out.println("  Output: " + outputPath);
        if (schemaName != null) {
            System.out.println("  Record: " + schemaName);
        }

        AvroJsonGenerator generator = new AvroJsonGenerator();
        generator.generateToFile(schema, outputPath);

        System.out.println("JSON generation completed successfully!");
        return 0;
    }

    /**
     * Run the 'encode' sub-command: encode JSON to Avro binary.
     * Usage: encode <schema.avsc> <input.json|--generate> <output.avro> [SchemaName]
     */
    private int runEncode(String[] args) throws Exception {
        if (args.length < 4) {
            throw new IllegalArgumentException(
                    "Usage: encode <schema.avsc> <input.json|--generate> <output.avro> [SchemaName]");
        }

        String schemaPath = args[1];
        String jsonInputOrFlag = args[2];
        String outputPath = args[3];
        String schemaName = args.length >= 5 ? args[4] : null;

        SchemaLoader loader = new SchemaLoader();
        Schema schema = loader.load(schemaPath, schemaName);

        boolean autoGenerate = "--generate".equals(jsonInputOrFlag);

        System.out.println("Encoding to Avro binary...");
        System.out.println("  Schema: " + schemaPath);
        System.out.println("  Output: " + outputPath);
        if (schemaName != null) {
            System.out.println("  Record: " + schemaName);
        }

        AvroBinaryEncoder encoder = new AvroBinaryEncoder();

        if (autoGenerate) {
            System.out.println("  Mode:   Auto-generate JSON then encode");
            AvroJsonGenerator generator = new AvroJsonGenerator();
            String json = generator.generate(schema);
            System.out.println("  Generated JSON:");
            System.out.println(json);
            encoder.encode(schema, json, outputPath);
        } else {
            System.out.println("  Input:  " + jsonInputOrFlag);
            encoder.encodeFromFile(schema, jsonInputOrFlag, outputPath);
        }

        System.out.println("Avro binary encoding completed successfully!");
        return 0;
    }

    /**
     * Run the original convert flow (JSON→Avro schema, OpenAPI→Avro schema).
     */
    private int runConvert(String[] args) throws Exception {
        CliArguments cliArgs = CliArguments.parse(args);
        cliArgs.validateOutputWritable();

        if (cliArgs.isInputDirectory()) {
            return runMassConvert(cliArgs.getInputPath(), cliArgs.getOutputDirectory());
        }

        cliArgs.validateInputExists();

        String inputPath = cliArgs.getInputPath();
        String outputPath = cliArgs.getOutputPath();
        ConversionOptions options = cliArgs.toConversionOptions();
        boolean openApi = isOpenApiFile(inputPath);

        System.out.println(openApi
                ? "Converting OpenAPI/Swagger to Avro schema..."
                : "Converting JSON to Avro schema...");
        System.out.println("  Input:  " + inputPath);
        System.out.println("  Output: " + outputPath);
        printOptions(cliArgs, options, openApi);

        if (!openApi) {
            jsonConverter.convert(inputPath, outputPath, options);
        } else if (cliArgs.hasRequestBody()) {
            System.out.println("  RequestBody: " + cliArgs.getRequestBodyMethod().toUpperCase() + " "
                    + cliArgs.getRequestBodyPath());
            openApiConverter.convertFromRequestBody(inputPath, cliArgs.getRequestBodyPath(),
                    cliArgs.getRequestBodyMethod(), outputPath, options);
        } else if (cliArgs.getSchemaName() != null) {
            System.out.println("  Schema: " + cliArgs.getSchemaName());
            openApiConverter.convert(inputPath, cliArgs.getSchemaName(), outputPath, options);
        } else {
            String outputDir = cliArgs.getOutputDirectory();
            System.out.println("  Generating all schemas to directory: " + outputDir);
            openApiConverter.convertAll(inputPath, outputDir, options);
        }

        System.out.println("Conversion completed successfully!");
        return 0;
    }

    /**
     * Echo the selected flags. --registry needs no converter setting: every schema is
     * already generated as a single self-contained record, the format schema registries
     * expect.
     */
    private void printOptions(CliArguments cliArgs, ConversionOptions options, boolean openApi) {
        if (!ConversionOptions.DEFAULT_ENVELOPE.equals(options.envelope())) {
            System.out.println("  Envelope: " + options.envelope());
        }
        if (options.functionalPerimeter() != null) {
            System.out.println("  Namespace: " + options.namespace());
        }
        if (options.includeDoc()) {
            System.out.println(openApi
                    ? "  Doc:    Enabled (include descriptions as doc fields)"
                    : "  Doc:    Ignored (a JSON data file has no descriptions)");
        }
        if (cliArgs.isRegistry()) {
            System.out.println("  Mode:   Registry (single self-contained schema for IBM/Confluent Schema Registry)");
        }
    }

    /**
     * Interactive directory (mass conversion) mode: for every OpenAPI spec found
     * directly in {@code inputDir} (non-recursive), list every schema it exposes
     * (components/schemas plus webhook requestBody payloads), let the user pick
     * one on stdin, then prompt for the conversion flags to apply to that one
     * file, and convert it. A file with no usable schema, or that fails to parse
     * or convert, does not stop the batch — it is journaled and reported in the
     * final summary.
     */
    private int runMassConvert(String inputDir, String outputDir) throws IOException {
        File[] specs = new File(inputDir).listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".yaml") || lower.endsWith(".yml") || lower.endsWith(".json");
        });
        if (specs == null || specs.length == 0) {
            throw new IllegalArgumentException(
                    "No OpenAPI spec files (*.yaml, *.yml, *.json) found in directory: " + inputDir);
        }
        Arrays.sort(specs);

        BufferedReader reader = new BufferedReader(new InputStreamReader(stdin));

        List<String> converted = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        Map<String, String> failed = new LinkedHashMap<>();

        for (int i = 0; i < specs.length; i++) {
            File specFile = specs[i];
            try {
                processMassConvertFile(reader, specFile, outputDir, converted, skipped, failed);
            } catch (EofSignal | QuitSignal e) {
                skipped.add(specFile.getName());
                for (int j = i + 1; j < specs.length; j++) {
                    skipped.add(specs[j].getName());
                }
                break;
            }
        }

        System.out.println();
        System.out.println(converted.size() + " converti(s), " + skipped.size() + " ignoré(s), "
                + failed.size() + " en échec");
        if (!skipped.isEmpty()) {
            System.out.println("  Ignorés : " + skipped);
        }
        if (!failed.isEmpty()) {
            for (Map.Entry<String, String> entry : failed.entrySet()) {
                System.err.println("  Échec " + entry.getKey() + " : " + entry.getValue());
            }
        }

        return failed.isEmpty() ? 0 : 1;
    }

    /**
     * Handle one spec file of a directory (mass conversion) batch: list its
     * schemas, prompt the user to pick one and to set the conversion flags, then
     * convert. Populates {@code converted}/{@code skipped}/{@code failed}
     * directly; throws {@link EofSignal}/{@link QuitSignal} to abort the whole
     * batch.
     */
    private void processMassConvertFile(BufferedReader reader, File specFile, String outputDir,
            List<String> converted, List<String> skipped, Map<String, String> failed) {
        OpenApiToAvroConverter.SpecSchemas spec;
        try {
            spec = openApiConverter.loadSchemas(specFile.getPath());
        } catch (Exception e) {
            failed.put(specFile.getName(), e.getMessage());
            return;
        }

        List<String> names = spec.getSchemaNames();
        System.out.println();
        System.out.println("== " + specFile.getName() + " ==");
        for (int i = 0; i < names.size(); i++) {
            System.out.println("  " + (i + 1) + ") " + names.get(i));
        }
        System.out.print("Schéma à convertir (numéro ou nom, 's' = ignorer ce fichier, 'q' = arrêter) : ");

        String choice = promptSchemaChoice(reader, names);
        if (choice == null) {
            skipped.add(specFile.getName());
            return;
        }

        boolean docMode = promptYesNo(reader, "  Inclure les docs ?", false);
        String functionalPerimeter = promptText(reader, "  Functional perimeter (namespace, vide = aucun)", null);
        String envelope = promptText(reader, "  Envelope", ConversionOptions.DEFAULT_ENVELOPE);
        ConversionOptions options = new ConversionOptions(docMode, functionalPerimeter, envelope);

        String outputPath = new File(outputDir, choice + ".avsc").getPath();
        try {
            String written = openApiConverter.convertNamed(spec, choice, outputPath, options);
            System.out.println("  Generated: " + written);
            converted.add(specFile.getName() + " -> " + choice);
        } catch (Exception e) {
            failed.put(specFile.getName(), e.getMessage());
        }
    }

    /**
     * Read one line from an interactive prompt.
     *
     * @throws EofSignal if the input stream has no more lines
     */
    private String readLine(BufferedReader reader) {
        try {
            String line = reader.readLine();
            if (line == null) {
                throw new EofSignal();
            }
            return line.trim();
        } catch (IOException e) {
            throw new EofSignal();
        }
    }

    /**
     * Prompt for a yes/no answer, defaulting to {@code defaultValue} on an empty
     * line.
     */
    private boolean promptYesNo(BufferedReader reader, String label, boolean defaultValue) {
        System.out.print(label + (defaultValue ? " (O/n) : " : " (o/N) : "));
        String line = readLine(reader);
        if (line.isEmpty()) {
            return defaultValue;
        }
        String lower = line.toLowerCase();
        return lower.equals("o") || lower.equals("oui") || lower.equals("y") || lower.equals("yes");
    }

    /**
     * Prompt for a free-text answer, defaulting to {@code defaultValue} on an
     * empty line.
     */
    private String promptText(BufferedReader reader, String label, String defaultValue) {
        String shown = defaultValue == null || defaultValue.isBlank() ? "vide" : defaultValue;
        System.out.print(label + " (" + shown + ") : ");
        String line = readLine(reader);
        return line.isEmpty() ? defaultValue : line;
    }

    /**
     * Prompt for the schema to convert from a numbered list, reprompting on
     * invalid input.
     *
     * @return the chosen schema name, or {@code null} if the user chose to skip
     *         this file ('s'/'skip')
     * @throws QuitSignal if the user asked to abort the whole batch ('q'/'quit')
     */
    private String promptSchemaChoice(BufferedReader reader, List<String> names) {
        while (true) {
            String line = readLine(reader);
            String lower = line.toLowerCase();
            if (lower.equals("s") || lower.equals("skip")) {
                return null;
            }
            if (lower.equals("q") || lower.equals("quit")) {
                throw new QuitSignal();
            }
            try {
                int index = Integer.parseInt(line);
                if (index >= 1 && index <= names.size()) {
                    return names.get(index - 1);
                }
            } catch (NumberFormatException ignored) {
                // not a number, fall through to exact-name matching below
            }
            if (names.contains(line)) {
                return line;
            }
            System.out.print("Choix invalide, réessaie : ");
        }
    }

    /**
     * Signals that stdin has no more input while running the interactive
     * directory (mass conversion) prompts — treated like 'quit'.
     */
    private static final class EofSignal extends RuntimeException {
    }

    /**
     * Signals that the user asked ('q'/'quit') to abort the rest of an
     * interactive directory (mass conversion) batch.
     */
    private static final class QuitSignal extends RuntimeException {
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
        System.err.println("  java -jar target/json-to-avro-converter.jar <command> [options]");
        System.err.println();
        System.err.println("Commands:");
        System.err.println("  generate  Generate sample JSON from an Avro schema");
        System.err.println("  encode    Encode JSON data to Avro binary format");
        System.err.println("  (none)    Convert JSON/OpenAPI to Avro schema (default)");
        System.err.println();
        System.err.println("Generate usage:");
        System.err.println("  generate <schema.avsc> <output.json> [SchemaName]");
        System.err.println();
        System.err.println("Encode usage:");
        System.err.println("  encode <schema.avsc> <input.json> <output.avro> [SchemaName]");
        System.err.println("  encode <schema.avsc> --generate <output.avro> [SchemaName]");
        System.err.println();
        System.err.println("Convert usage (default):");
        System.err.println("  <input-file> <output.avsc> [schema-name] [--registry] [--doc] [--functional-perimeter <name>] [--envelope <name>] [--stacktrace]");
        System.err.println("  <input-file> <output.avsc> --from-request-body <path> <method> [--registry] [--doc] [--functional-perimeter <name>] [--envelope <name>] [--stacktrace]");
        System.err.println("  <openapi-file> <output-dir> [flags...]  (no schema name: converts every record schema; an output not ending in .avsc is a directory)");
        System.err.println("  <input-dir> <output-dir>  (interactive: prompts for a schema then flags for each spec file in the directory)");
        System.err.println();
        System.err.println("  --stacktrace  Print the full Java stack trace on unexpected errors (hidden by default)");
        System.err.println();
        System.err.println("Examples:");
        System.err.println("  # Generate sample JSON from Avro schema");
        System.err.println("  java -jar target/json-to-avro-converter.jar generate src/main/avro/ResultResponse.avsc output.json ResultResponse");
        System.err.println();
        System.err.println("  # Encode JSON to Avro binary");
        System.err.println("  java -jar target/json-to-avro-converter.jar encode src/main/avro/ResultResponse.avsc data.json output.avro ResultResponse");
        System.err.println();
        System.err.println("  # Auto-generate JSON and encode to Avro binary");
        System.err.println("  java -jar target/json-to-avro-converter.jar encode src/main/avro/ResultResponse.avsc --generate output.avro ResultResponse");
        System.err.println();
        System.err.println("  # Convert JSON data to Avro schema");
        System.err.println("  java -jar target/json-to-avro-converter.jar data.json schema.avsc");
        System.err.println();
        System.err.println("  # Convert JSON data in registry mode with a custom functional-perimeter namespace");
        System.err.println("  java -jar target/json-to-avro-converter.jar data.json schema.avsc --registry --functional-perimeter orders");
        System.err.println();
        System.err.println("  # Convert OpenAPI to registry-compatible Avro schema (IBM / Confluent Schema Registry)");
        System.err.println("  java -jar target/json-to-avro-converter.jar api.yaml ResultResponse.avsc ResultResponse --registry");
        System.err.println();
        System.err.println("  # Convert OpenAPI with doc fields (include descriptions from OpenAPI spec)");
        System.err.println("  java -jar target/json-to-avro-converter.jar api.yaml User.avsc User --doc");
        System.err.println();
        System.err.println("  # Registry mode with doc fields");
        System.err.println("  java -jar target/json-to-avro-converter.jar api.yaml ResultResponse.avsc ResultResponse --registry --doc");
        System.err.println();
        System.err.println("  # Convert the requestBody schema of a specific path/method operation");
        System.err.println("  java -jar target/json-to-avro-converter.jar api.yaml CreateUser.avsc --from-request-body /users POST");
        System.err.println();
        System.err.println("  # Convert with a custom functional-perimeter namespace (com.shanks.generated.<name>)");
        System.err.println("  java -jar target/json-to-avro-converter.jar api.yaml CreateUser.avsc --functional-perimeter users --from-request-body /users POST");
        System.err.println();
        System.err.println("  # Convert using a non-default notif envelope (src/main/resources/envelopes/<name>.json)");
        System.err.println("  java -jar target/json-to-avro-converter.jar api.yaml User.avsc User --envelope minimal");
        System.err.println();
        System.err.println("  # Interactive: convert a directory of OpenAPI specs, one schema per file, chosen at the prompt");
        System.err.println("  java -jar target/json-to-avro-converter.jar specs/ out/");
    }
}
