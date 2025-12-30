package com.shanks.cli;

import com.shanks.converter.JsonToAvroConverter;

/**
 * CLI orchestrator for the JSON to Avro schema converter.
 *
 * This class follows the Single Responsibility Principle by coordinating
 * CLI operations and user interaction.
 */
public class ConverterCli {

    private final JsonToAvroConverter converter;

    /**
     * Constructor with default converter.
     */
    public ConverterCli() {
        this.converter = new JsonToAvroConverter();
    }

    /**
     * Constructor with dependency injection for testing.
     *
     * @param converter the JSON to Avro converter
     */
    public ConverterCli(JsonToAvroConverter converter) {
        this.converter = converter;
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

            System.out.println("Converting JSON to Avro schema...");
            System.out.println("  Input:  " + cliArgs.getInputJsonPath());
            System.out.println("  Output: " + cliArgs.getOutputAvscPath());

            converter.convert(cliArgs.getInputJsonPath(), cliArgs.getOutputAvscPath());

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
     * Print usage information.
     */
    private void printUsage() {
        System.err.println("Usage:");
        System.err.println("  java -cp target/demo-1.0-SNAPSHOT.jar com.shanks.App <input.json> <output.avsc>");
        System.err.println();
        System.err.println("Example:");
        System.err.println("  java -cp target/demo-1.0-SNAPSHOT.jar com.shanks.App data.json schema.avsc");
        System.err.println();
        System.err.println("Or with Maven:");
        System.err.println("  mvn exec:java -Dexec.mainClass=\"com.shanks.App\" -Dexec.args=\"data.json schema.avsc\"");
    }
}
