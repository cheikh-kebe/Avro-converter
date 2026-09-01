package com.shanks.converter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiToAvroConverterWebhookTest {

    @Test
    void convertAll_generatesRecordSchemaForWebhookRequestBody(@TempDir Path outDir) throws IOException {
        OpenApiToAvroConverter converter = new OpenApiToAvroConverter();

        converter.convertAll("test-openapi-webhooks.yaml", outDir.toString());

        Path generated = outDir.resolve("OnNewUser.avsc");
        assertThat(generated).exists();

        String content = Files.readString(generated);
        assertThat(content).contains("\"type\" : \"record\"");
        assertThat(content).contains("\"name\" : \"email\"");
        assertThat(content).contains("\"name\" : \"displayName\"");
    }
}
