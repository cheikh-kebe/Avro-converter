package com.shanks.serializer;

import org.apache.avro.Schema;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads Avro schemas from .avsc files.
 * Supports both single-schema files (JSON object) and unified files (JSON array).
 */
public class SchemaLoader {

    /**
     * Load a named schema from an .avsc file.
     *
     * @param avscPath   path to the .avsc file
     * @param schemaName name of the record/enum to find (required for unified files)
     * @return the resolved Avro Schema
     * @throws IOException if the file cannot be read or parsed
     */
    public Schema load(String avscPath, String schemaName) throws IOException {
        String content = Files.readString(Path.of(avscPath)).trim();

        if (content.startsWith("[")) {
            return loadFromUnifiedFile(content, schemaName);
        } else {
            return loadFromSingleFile(content);
        }
    }

    private Schema loadFromSingleFile(String content) {
        return new Schema.Parser().parse(content);
    }

    private Schema loadFromUnifiedFile(String content, String schemaName) {
        Schema.Parser parser = new Schema.Parser();
        Schema lastParsed = null;

        // Parse the JSON array manually: extract individual schema objects
        // Use Avro's built-in support for parsing multiple schemas
        org.apache.avro.Schema.Parser schemaParser = new org.apache.avro.Schema.Parser();

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode array = mapper.readTree(content);

            if (!array.isArray()) {
                throw new IllegalArgumentException("Expected JSON array in unified schema file");
            }

            // Parse schemas in order so that references resolve correctly
            for (com.fasterxml.jackson.databind.JsonNode node : array) {
                lastParsed = schemaParser.parse(node.toString());
            }

            // Find the requested schema by name
            if (schemaName != null && !schemaName.isEmpty()) {
                Schema found = schemaParser.getTypes().get("com.shanks.generated." + schemaName);
                if (found == null) {
                    // Try without namespace prefix
                    found = schemaParser.getTypes().get(schemaName);
                }
                if (found == null) {
                    throw new IllegalArgumentException(
                            "Schema '" + schemaName + "' not found in unified file. Available: "
                                    + schemaParser.getTypes().keySet());
                }
                return found;
            }

            // If no name specified, return the last schema (typically the root record)
            return lastParsed;

        } catch (IOException e) {
            throw new RuntimeException("Failed to parse unified schema file", e);
        }
    }
}
