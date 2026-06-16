package com.shanks.converter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Writes Avro schema JSON to disk, producing both a pretty-printed file
 * and a minified single-line companion file (.min.avsc).
 */
class SchemaFileWriter {

    /**
     * Write a schema JSON string to the given path and a minified companion.
     *
     * @param schemaJson the schema JSON to write
     * @param outputPath path to the output file (e.g. User.avsc)
     * @throws IOException if file operations fail
     */
    static void write(String schemaJson, String outputPath) throws IOException {
        File outputFile = new File(outputPath);
        File parentDir = outputFile.getParentFile();

        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("Failed to create output directory: " + parentDir.getAbsolutePath());
            }
        }

        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(schemaJson);
        }

        String minPath = buildMinPath(outputPath);
        try (FileWriter writer = new FileWriter(new File(minPath))) {
            writer.write(minifyJson(schemaJson));
        }
    }

    private static String buildMinPath(String outputPath) {
        int dotIndex = outputPath.lastIndexOf('.');
        if (dotIndex > 0) {
            return outputPath.substring(0, dotIndex) + ".min" + outputPath.substring(dotIndex);
        }
        return outputPath + ".min";
    }

    private static String minifyJson(String json) {
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean escape = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escape) {
                result.append(c);
                escape = false;
                continue;
            }

            if (c == '\\' && inString) {
                result.append(c);
                escape = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                result.append(c);
                continue;
            }

            if (inString) {
                result.append(c);
            } else if (!Character.isWhitespace(c)) {
                result.append(c);
            }
        }

        return result.toString();
    }
}
