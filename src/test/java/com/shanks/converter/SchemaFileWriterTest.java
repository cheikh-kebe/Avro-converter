package com.shanks.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaFileWriterTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private static final String SCHEMA_JSON = "{"
            + "\"type\":\"record\","
            + "\"name\":\"User\","
            + "\"namespace\":\"com.shanks.generated\","
            + "\"fields\":[{\"name\":\"id\",\"type\":\"string\"}]"
            + "}";

    private static final String SCHEMA_JSON_WITH_DOC = "{"
            + "\"type\":\"record\","
            + "\"name\":\"User\","
            + "\"namespace\":\"com.shanks.generated\","
            + "\"doc\":\"A user record\","
            + "\"fields\":[{\"name\":\"id\",\"type\":\"string\"}]"
            + "}";

    @TempDir
    Path tempDir;

    @Test
    void shouldVersionAllThreeFilenamesWhenApiVersionGiven() throws IOException {
        String outputPath = tempDir.resolve("User.avsc").toString();

        SchemaFileWriter.write(SCHEMA_JSON, outputPath, "default", "1.0.0");

        assertThat(tempDir.resolve("User.v1.0.0.avsc")).exists();
        assertThat(tempDir.resolve("User.v1.0.0.min.avsc")).exists();
        assertThat(tempDir.resolve("User.v1.0.0.webhook.avsc")).exists();
        assertThat(tempDir.resolve("User.avsc")).doesNotExist();
    }

    @Test
    void shouldSetDocToApiVersionWhenSchemaHasNoDoc() throws IOException {
        String outputPath = tempDir.resolve("User.avsc").toString();

        SchemaFileWriter.write(SCHEMA_JSON, outputPath, "default", "1.0.0");

        JsonNode root = mapper.readTree(Files.readString(tempDir.resolve("User.v1.0.0.avsc")));
        assertThat(root.get("doc").asText()).isEqualTo("API version: 1.0.0");
    }

    @Test
    void shouldAppendApiVersionWhenSchemaAlreadyHasDoc() throws IOException {
        String outputPath = tempDir.resolve("User.avsc").toString();

        SchemaFileWriter.write(SCHEMA_JSON_WITH_DOC, outputPath, "default", "1.0.0");

        JsonNode root = mapper.readTree(Files.readString(tempDir.resolve("User.v1.0.0.avsc")));
        assertThat(root.get("doc").asText()).isEqualTo("A user record | API version: 1.0.0");
    }

    @Test
    void shouldNotVersionFilenameOrAddDocWhenApiVersionIsNull() throws IOException {
        String outputPath = tempDir.resolve("User.avsc").toString();

        SchemaFileWriter.write(SCHEMA_JSON, outputPath, "default", null);

        assertThat(tempDir.resolve("User.avsc")).exists();
        assertThat(tempDir.resolve("User.min.avsc")).exists();
        assertThat(tempDir.resolve("User.webhook.avsc")).exists();

        JsonNode root = mapper.readTree(Files.readString(tempDir.resolve("User.avsc")));
        assertThat(root.has("doc")).isFalse();
    }

    @Test
    void shouldNotVersionFilenameOrAddDocWhenApiVersionIsBlank() throws IOException {
        String outputPath = tempDir.resolve("User.avsc").toString();

        SchemaFileWriter.write(SCHEMA_JSON, outputPath, "default", "  ");

        assertThat(tempDir.resolve("User.avsc")).exists();
        JsonNode root = mapper.readTree(Files.readString(tempDir.resolve("User.avsc")));
        assertThat(root.has("doc")).isFalse();
    }
}
