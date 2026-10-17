package com.shanks.cli;

import com.shanks.converter.ConversionOptions;

import java.io.File;

/**
 * Parses and validates the arguments of the default convert command:
 * {@code <input> <output> [schema-name] [flags...]}.
 *
 * This class follows the Single Responsibility Principle by focusing solely on
 * argument parsing and validation — every flag is recognized here, in one place.
 */
public class CliArguments {

    private final String inputPath;
    private final String outputPath;
    private String schemaName;
    private boolean registry;
    private boolean includeDoc;
    private String functionalPerimeter;
    private String envelope;
    private String requestBodyPath;
    private String requestBodyMethod;

    /**
     * Constructor with validation, without any flag.
     *
     * @param inputPath  path to the input file (JSON data or OpenAPI spec) or directory
     * @param outputPath path to the output .avsc file or directory
     * @throws IllegalArgumentException if arguments are invalid
     */
    public CliArguments(String inputPath, String outputPath) {
        if (inputPath == null || inputPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Input path cannot be null or empty");
        }
        if (outputPath == null || outputPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Output path cannot be null or empty");
        }

        this.inputPath = inputPath.trim();
        this.outputPath = outputPath.trim();
    }

    /**
     * Parse arguments from command-line args array. Flags may appear in any order
     * after {@code <input> <output>}; the first non-flag argument there is the
     * schema name.
     *
     * @param args command-line arguments
     * @return parsed CliArguments
     * @throws IllegalArgumentException if arguments are missing, a flag lacks its
     *                                  value(s), or a flag is unknown
     */
    public static CliArguments parse(String[] args) {
        if (args == null || args.length < 2) {
            throw new IllegalArgumentException(
                    "Usage: java -jar target/json-to-avro-converter.jar <input> <output> [schema-name] [flags...]"
            );
        }

        CliArguments parsed = new CliArguments(args[0], args[1]);
        for (int i = 2; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--registry" -> parsed.registry = true;
                case "--doc" -> parsed.includeDoc = true;
                case "--stacktrace" -> {
                    // handled by ConverterCli.run before parsing
                }
                case "--functional-perimeter" -> parsed.functionalPerimeter = valueAfter(args, i++, arg);
                case "--envelope" -> parsed.envelope = valueAfter(args, i++, arg);
                case "--from-request-body" -> {
                    if (i + 2 >= args.length || args[i + 1].startsWith("--") || args[i + 2].startsWith("--")) {
                        throw new IllegalArgumentException(
                                "--from-request-body requires <path> <method>, e.g. --from-request-body /users POST");
                    }
                    parsed.requestBodyPath = args[++i];
                    parsed.requestBodyMethod = args[++i];
                }
                default -> {
                    if (arg.startsWith("--")) {
                        throw new IllegalArgumentException("Unknown option: " + arg);
                    }
                    if (parsed.schemaName == null) {
                        parsed.schemaName = arg;
                    }
                }
            }
        }
        return parsed;
    }

    /** @return the value following the flag at {@code flagIndex} */
    private static String valueAfter(String[] args, int flagIndex, String flag) {
        if (flagIndex + 1 >= args.length || args[flagIndex + 1].startsWith("--")) {
            throw new IllegalArgumentException(flag + " requires a value");
        }
        return args[flagIndex + 1];
    }

    /**
     * @return true if the input path points to an existing directory (mass
     *         conversion mode)
     */
    public boolean isInputDirectory() {
        return new File(inputPath).isDirectory();
    }

    /**
     * Validate that input file exists.
     *
     * @throws IllegalArgumentException if input file doesn't exist
     */
    public void validateInputExists() {
        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            throw new IllegalArgumentException("Input file does not exist: " + inputPath);
        }
        if (!inputFile.isFile()) {
            throw new IllegalArgumentException("Input path is not a file: " + inputPath);
        }
        if (!inputFile.canRead()) {
            throw new IllegalArgumentException("Input file is not readable: " + inputPath);
        }
    }

    /**
     * Validate that output directory is writable.
     *
     * @throws IllegalArgumentException if output directory is not writable
     */
    public void validateOutputWritable() {
        File outputFile = new File(outputPath);
        File parentDir = outputFile.getParentFile();

        if (parentDir != null && !parentDir.exists()) {
            return;
        }

        if (parentDir != null && !parentDir.canWrite()) {
            throw new IllegalArgumentException("Output directory is not writable: " + parentDir.getAbsolutePath());
        }
    }

    /**
     * Interpret the output argument as a directory, for the modes that write several
     * files (all schemas of a spec, interactive directory mode):
     * <ul>
     *   <li>a path ending with {@code .avsc} is a file: its parent directory is used
     *       ({@code .} when it has none);</li>
     *   <li>anything else is the directory itself, with or without a trailing separator
     *       ({@code out}, {@code out/}).</li>
     * </ul>
     *
     * @return the output directory
     */
    public String getOutputDirectory() {
        String path = outputPath;
        while (path.length() > 1 && (path.endsWith("/") || path.endsWith("\\"))) {
            path = path.substring(0, path.length() - 1);
        }
        if (!path.toLowerCase().endsWith(".avsc")) {
            return path;
        }
        String parent = new File(path).getParent();
        return parent == null ? "." : parent;
    }

    /**
     * @return the conversion options selected by the flags (--doc,
     *         --functional-perimeter, --envelope)
     */
    public ConversionOptions toConversionOptions() {
        return new ConversionOptions(includeDoc, functionalPerimeter, envelope);
    }

    /** @return the input file or directory path */
    public String getInputPath() {
        return inputPath;
    }

    /** @return the output path, as given */
    public String getOutputPath() {
        return outputPath;
    }

    /** @return the schema name positional argument, or null if absent */
    public String getSchemaName() {
        return schemaName;
    }

    /** @return true if --registry was given */
    public boolean isRegistry() {
        return registry;
    }

    /** @return true if --from-request-body was given */
    public boolean hasRequestBody() {
        return requestBodyPath != null;
    }

    /** @return the --from-request-body path, or null */
    public String getRequestBodyPath() {
        return requestBodyPath;
    }

    /** @return the --from-request-body HTTP method, or null */
    public String getRequestBodyMethod() {
        return requestBodyMethod;
    }

    @Override
    public String toString() {
        return "CliArguments{" +
                "inputPath='" + inputPath + '\'' +
                ", outputPath='" + outputPath + '\'' +
                ", schemaName='" + schemaName + '\'' +
                ", registry=" + registry +
                ", options=" + toConversionOptions() +
                ", requestBody=" + (hasRequestBody() ? requestBodyMethod + " " + requestBodyPath : "none") +
                '}';
    }
}
