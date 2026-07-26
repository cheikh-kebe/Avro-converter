package com.shanks.converter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Writes Avro schema JSON to disk, producing a pretty-printed file, a minified
 * single-line companion file (.min.avsc), and a consolidated companion file
 * (.webhook.avsc) wrapping the schema in the standard notif envelope.
 */
class SchemaFileWriter {

    /**
     * Write a schema JSON string to the given path, plus its minified and
     * notif-wrapped companions.
     *
     * @param schemaJson the schema JSON to write
     * @param outputPath path to the output file (e.g. User.avsc)
     * @throws IOException if file operations fail
     */
    static void write(String schemaJson, String outputPath) throws IOException {
        write(schemaJson, outputPath, "default");
    }

    /**
     * Write a schema JSON string to the given path, plus its minified and
     * notif-wrapped companions, using the named envelope template for the
     * notif-wrapped companion.
     *
     * @param schemaJson   the schema JSON to write
     * @param outputPath   path to the output file (e.g. User.avsc)
     * @param envelopeName the envelope template name (see {@link NotifWrapperGenerator})
     * @throws IOException if file operations fail
     */
    static void write(String schemaJson, String outputPath, String envelopeName) throws IOException {
        // Compute all three contents before touching disk, so a failure
        // (e.g. NotifWrapperGenerator.wrap() on malformed JSON) never
        // leaves a partial set of output files behind.
        String minJson = minifyJson(schemaJson);
        String webhookJson = NotifWrapperGenerator.wrap(schemaJson, envelopeName);

        File outputFile = new File(outputPath);
        File parentDir = outputFile.getParentFile();

        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("Failed to create output directory: " + parentDir.getAbsolutePath());
            }
        }

        writeFile(outputFile, schemaJson);
        writeFile(new File(buildSuffixedPath(outputPath, ".min")), minJson);
        writeFile(new File(buildSuffixedPath(outputPath, ".webhook")), webhookJson);
    }

    private static void writeFile(File file, String content) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }

    /** Insert a suffix before the file extension (e.g. "User.avsc" + ".min" → "User.min.avsc"). */
    private static String buildSuffixedPath(String outputPath, String suffix) {
        int dotIndex = outputPath.lastIndexOf('.');
        if (dotIndex > 0) {
            return outputPath.substring(0, dotIndex) + suffix + outputPath.substring(dotIndex);
        }
        return outputPath + suffix;
    }

    /** Strip whitespace outside of string literals to produce a single-line JSON string. */
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
