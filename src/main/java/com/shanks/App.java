package com.shanks;

import com.shanks.cli.ConverterCli;

/**
 * JSON to Avro Schema Converter CLI Application.
 *
 * This application converts JSON files to Avro schema files (.avsc)
 * with automatic type inference.
 */
public class App {

    /**
     * Entry point: delegates to {@link ConverterCli} and exits with its return code.
     *
     * @param args command-line arguments (see {@link ConverterCli#run})
     */
    public static void main(String[] args) {
        ConverterCli cli = new ConverterCli();
        int exitCode = cli.run(args);
        System.exit(exitCode);
    }
}
