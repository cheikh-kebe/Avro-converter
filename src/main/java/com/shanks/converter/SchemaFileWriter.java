package com.shanks.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Writes Avro schema JSON to disk, producing a pretty-printed file, a minified
 * single-line companion file (.min.avsc), and a consolidated companion file
 * (.webhook.avsc) wrapping the schema in the standard notif envelope.
 */
class SchemaFileWriter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

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
        write(schemaJson, outputPath, envelopeName, null);
    }

    /**
     * Write a schema JSON string to the given path, plus its minified and
     * notif-wrapped companions, using the named envelope template, and stamping
     * the OpenAPI spec's own API version (from {@code info.version}) onto the
     * root schema's {@code doc} and into all three output filenames.
     *
     * @param schemaJson   the schema JSON to write
     * @param outputPath   path to the output file (e.g. User.avsc)
     * @param envelopeName the envelope template name (see {@link NotifWrapperGenerator})
     * @param apiVersion   the source API's version (e.g. "1.0.0"), or null/blank if
     *                     unavailable (e.g. JSON-input mode) — a no-op in that case
     * @throws IOException if file operations fail
     */
    static void write(String schemaJson, String outputPath, String envelopeName, String apiVersion)
            throws IOException {
        schemaJson = injectVersionDoc(schemaJson, apiVersion);
        if (apiVersion != null && !apiVersion.isBlank()) {
            outputPath = buildSuffixedPath(outputPath, ".v" + apiVersion);
        }

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

    /**
     * Set (or append to) the root schema's {@code doc} field with the source API's
     * version. No-op when {@code apiVersion} is null/blank.
     */
    private static String injectVersionDoc(String schemaJson, String apiVersion) throws IOException {
        if (apiVersion == null || apiVersion.isBlank()) {
            return schemaJson;
        }
        ObjectNode root = (ObjectNode) MAPPER.readTree(schemaJson);
        String versionNote = "API version: " + apiVersion;
        String existingDoc = root.has("doc") ? root.get("doc").asText() : "";
        root.put("doc", existingDoc.isBlank() ? versionNote : existingDoc + " | " + versionNote);
        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);
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
