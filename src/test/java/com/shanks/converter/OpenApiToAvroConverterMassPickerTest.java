package com.shanks.converter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the interactive directory (mass conversion) picker: listing every
 * named schema a spec exposes (components/schemas + webhook requestBody
 * payloads) and converting a single chosen one.
 */
class OpenApiToAvroConverterMassPickerTest {

    private final OpenApiToAvroConverter converter = new OpenApiToAvroConverter();

    @Test
    void loadSchemas_listsComponentSchemasWithoutWebhookEntries() throws IOException {
        OpenApiToAvroConverter.SpecSchemas spec = converter.loadSchemas("test-openapi.yaml");

        List<String> names = spec.getSchemaNames();

        assertThat(names).contains("Name", "CardType");
    }

    @Test
    void loadSchemas_includesWebhookRequestBodyButNotResponses() throws IOException {
        OpenApiToAvroConverter.SpecSchemas spec = converter.loadSchemas("test-openapi-webhooks.yaml");

        List<String> names = spec.getSchemaNames();

        assertThat(names).contains("UserCreatedEvent", "Profile", "OnNewUser");
        assertThat(names).noneMatch(name -> name.contains("Response"));
    }

    @Test
    void convertNamed_generatesFileForChosenComponentSchema(@TempDir Path outDir) throws IOException {
        OpenApiToAvroConverter.SpecSchemas spec = converter.loadSchemas("test-openapi.yaml");
        String outputPath = outDir.resolve("Name.avsc").toString();

        converter.convertNamed(spec, "Name", outputPath, false);

        String content = Files.readString(outDir.resolve("Name.v1.0.0.avsc"));
        assertThat(content).contains("\"type\" : \"record\"");
        assertThat(content).contains("\"name\" : \"Name\"");
    }

    @Test
    void convertNamed_generatesFileForChosenWebhookSchema(@TempDir Path outDir) throws IOException {
        OpenApiToAvroConverter.SpecSchemas spec = converter.loadSchemas("test-openapi-webhooks.yaml");
        String outputPath = outDir.resolve("OnNewUser.avsc").toString();

        converter.convertNamed(spec, "OnNewUser", outputPath, false);

        String content = Files.readString(outDir.resolve("OnNewUser.v1.0.0.avsc"));
        assertThat(content).contains("\"type\" : \"record\"");
    }

    @Test
    void convertNamed_registryModeProducesSelfContainedSchema(@TempDir Path outDir) throws IOException {
        OpenApiToAvroConverter.SpecSchemas spec = converter.loadSchemas("test-openapi-webhooks.yaml");
        String outputPath = outDir.resolve("OnNewUser.avsc").toString();

        converter.convertNamed(spec, "OnNewUser", outputPath, true);

        String content = Files.readString(outDir.resolve("OnNewUser.v1.0.0.avsc"));
        assertThat(content).contains("\"type\" : \"record\"");
    }

    @Test
    void convertNamed_unknownNameThrows() throws IOException {
        OpenApiToAvroConverter.SpecSchemas spec = converter.loadSchemas("test-openapi.yaml");

        assertThatThrownBy(() -> converter.convertNamed(spec, "DoesNotExist", "out.avsc", false))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void convertNamed_nonRecordSchemaThrowsExplicitError() throws IOException {
        OpenApiToAvroConverter.SpecSchemas spec = converter.loadSchemas("test-openapi.yaml");

        assertThatThrownBy(() -> converter.convertNamed(spec, "CardType", "out.avsc", false))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("not a record");
    }

    @Test
    void loadSchemas_throwsWhenSpecHasNoSchemaAtAll(@TempDir Path dir) throws IOException {
        Path emptySpec = dir.resolve("empty.yaml");
        Files.writeString(emptySpec, "openapi: 3.0.3\ninfo:\n  title: Empty\n  version: 1.0.0\npaths: {}\n");

        assertThatThrownBy(() -> converter.loadSchemas(emptySpec.toString()))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("No schemas found");
    }
}
