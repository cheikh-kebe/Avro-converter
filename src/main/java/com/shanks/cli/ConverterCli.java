package com.shanks.cli;

import com.shanks.converter.AvroSchemaValidationException;
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
        boolean showStackTrace = hasFlag(args, "--stacktrace");

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

        } catch (AvroSchemaValidationException e) {
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

        String inputPath = cliArgs.getInputJsonPath();
        String outputPath = cliArgs.getOutputAvscPath();

        if (cliArgs.isInputDirectory()) {
            return runMassConvert(inputPath, outputPath);
        }

        cliArgs.validateInputExists();

        String envelopeName = getFlagValue(args, "--envelope");
        if (envelopeName != null) {
            System.out.println("  Envelope: " + envelopeName);
            openApiConverter.setEnvelope(envelopeName);
            jsonConverter.setEnvelope(envelopeName);
        }

        if (isOpenApiFile(inputPath)) {
            System.out.println("Converting OpenAPI/Swagger to Avro schema...");
            System.out.println("  Input:  " + inputPath);
            System.out.println("  Output: " + outputPath);

            // Check mode flags (scan all arguments for --registry and --doc)
            boolean registryMode = hasFlag(args, "--registry");
            boolean docMode = hasFlag(args, "--doc");
            String functionalPerimeter = getFlagValue(args, "--functional-perimeter");

            if (docMode) {
                System.out.println("  Doc:    Enabled (include descriptions as doc fields)");
                openApiConverter.setIncludeDoc(true);
            }

            if (functionalPerimeter != null) {
                System.out.println("  Namespace: com.shanks.generated." + functionalPerimeter);
                openApiConverter.setFunctionalPerimeter(functionalPerimeter);
            }

            // Locate --from-request-body anywhere in the arguments (it may be preceded
            // by other flags such as --functional-perimeter).
            int fromRequestBodyIndex = indexOfFlag(args, "--from-request-body");

            // Remaining positional arguments after input/output paths, excluding recognized
            // flags and their values, are treated as the schema name.
            String schemaName = firstPositionalArgAfter(args, 2);

            if (fromRequestBodyIndex >= 0) {
                if (args.length < fromRequestBodyIndex + 3) {
                    throw new IllegalArgumentException(
                            "--from-request-body requires <path> <method>, e.g. --from-request-body /users POST");
                }
                String pathKey = args[fromRequestBodyIndex + 1];
                String httpMethod = args[fromRequestBodyIndex + 2];
                System.out.println("  RequestBody: " + httpMethod.toUpperCase() + " " + pathKey);

                if (registryMode) {
                    System.out.println("  Mode:   Registry (single self-contained schema for IBM/Confluent Schema Registry)");
                    openApiConverter.convertRegistryFromRequestBody(inputPath, pathKey, httpMethod, outputPath);
                } else {
                    openApiConverter.convertFromRequestBody(inputPath, pathKey, httpMethod, outputPath);
                }
            } else if (schemaName != null) {
                // If args contains a schema name, convert specific schema
                System.out.println("  Schema: " + schemaName);

                if (registryMode) {
                    System.out.println("  Mode:   Registry (single self-contained schema for IBM/Confluent Schema Registry)");
                    openApiConverter.convertRegistry(inputPath, schemaName, outputPath);
                } else {
                    openApiConverter.convert(inputPath, schemaName, outputPath);
                }
            } else {
                // Extract output directory and convert all schemas
                String outputDir = deriveOutputDir(outputPath);
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
    }

    /**
     * Derive an output directory from an output path argument: everything before
     * the last path separator, or "." if the path has none.
     */
    private String deriveOutputDir(String outputPath) {
        int lastSlash = outputPath.lastIndexOf('/');
        if (lastSlash == -1) {
            lastSlash = outputPath.lastIndexOf('\\');
        }
        return lastSlash > 0 ? outputPath.substring(0, lastSlash) : ".";
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
    private int runMassConvert(String inputDir, String outputPathArg) throws IOException {
        File[] specs = new File(inputDir).listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".yaml") || lower.endsWith(".yml") || lower.endsWith(".json");
        });
        if (specs == null || specs.length == 0) {
            throw new IllegalArgumentException(
                    "No OpenAPI spec files (*.yaml, *.yml, *.json) found in directory: " + inputDir);
        }
        Arrays.sort(specs);

        String outputDir = deriveOutputDir(outputPathArg);
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

        boolean registryMode = promptYesNo(reader, "  Mode registry ?", false);
        boolean docMode = promptYesNo(reader, "  Inclure les docs ?", false);
        String functionalPerimeter = promptText(reader, "  Functional perimeter (namespace, vide = aucun)", null);
        String envelope = promptText(reader, "  Envelope", "default");

        openApiConverter.setIncludeDoc(docMode);
        openApiConverter.setFunctionalPerimeter(functionalPerimeter);
        openApiConverter.setEnvelope(envelope);

        String outputPath = new File(outputDir, choice + ".avsc").getPath();
        try {
            openApiConverter.convertNamed(spec, choice, outputPath, registryMode);
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
     * Check if a flag is present in the arguments.
     */
    private boolean hasFlag(String[] args, String flag) {
        return indexOfFlag(args, flag) >= 0;
    }

    /**
     * Find the index of a flag in the arguments, or -1 if not present.
     */
    private int indexOfFlag(String[] args, String flag) {
        for (int i = 0; i < args.length; i++) {
            if (flag.equals(args[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Get the value following a flag in the arguments (e.g. --functional-perimeter users).
     *
     * @throws IllegalArgumentException if the flag is present but has no value, or its value
     *                                   looks like another flag (starts with "--")
     */
    private String getFlagValue(String[] args, String flag) {
        int index = indexOfFlag(args, flag);
        if (index < 0) {
            return null;
        }
        if (index + 1 >= args.length || args[index + 1].startsWith("--")) {
            throw new IllegalArgumentException(flag + " requires a value");
        }
        return args[index + 1];
    }

    /**
     * Find the first positional (non-flag) argument at or after the given index,
     * skipping recognized flags and the values that belong to them.
     */
    private String firstPositionalArgAfter(String[] args, int startIndex) {
        for (int i = startIndex; i < args.length; i++) {
            String arg = args[i];
            if ("--registry".equals(arg) || "--doc".equals(arg)) {
                continue;
            }
            if ("--functional-perimeter".equals(arg) || "--envelope".equals(arg)) {
                i++; // skip the flag's value
                continue;
            }
            if ("--from-request-body".equals(arg)) {
                i += 2; // skip the flag's <path> <method> values
                continue;
            }
            if (!arg.startsWith("--")) {
                return arg;
            }
        }
        return null;
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
