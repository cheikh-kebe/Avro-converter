package com.shanks.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotifWrapperGeneratorTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private static final String SCHEMA_JSON = "{"
            + "\"type\":\"record\","
            + "\"name\":\"User\","
            + "\"namespace\":\"com.shanks.generated\","
            + "\"fields\":[{\"name\":\"id\",\"type\":\"string\"}]"
            + "}";

    @Test
    void shouldInjectPayloadIntoDefaultEnvelope() throws IOException {
        String wrapped = NotifWrapperGenerator.wrap(SCHEMA_JSON);

        JsonNode root = mapper.readTree(wrapped);
        assertThat(root.get("name").asText()).isEqualTo("Notif");
        assertThat(root.get("namespace").asText()).isEqualTo("com.shanks.generated");

        JsonNode payloadField = findField(root, "payload");
        assertThat(payloadField).isNotNull();
        assertThat(payloadField.get("type").get("name").asText()).isEqualTo("User");
    }

    @Test
    void shouldInjectPayloadRegardlessOfEnvelopeNesting() throws IOException {
        String wrapped = NotifWrapperGenerator.wrap(SCHEMA_JSON, "nested-test");

        JsonNode root = mapper.readTree(wrapped);
        JsonNode wrapperField = findField(root, "wrapper");
        JsonNode deeplyNestedField = findField(wrapperField.get("type"), "deeplyNested");
        JsonNode payloadField = findField(deeplyNestedField.get("type"), "payload");

        assertThat(payloadField.get("type").get("name").asText()).isEqualTo("User");
    }

    @Test
    void shouldThrowWhenEnvelopeIsMissing() {
        assertThatThrownBy(() -> NotifWrapperGenerator.wrap(SCHEMA_JSON, "does-not-exist"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("does-not-exist");
    }

    @Test
    void shouldThrowWhenEnvelopeHasNoPayloadField() {
        assertThatThrownBy(() -> NotifWrapperGenerator.wrap(SCHEMA_JSON, "no-payload-test"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("payload");
    }

    private JsonNode findField(JsonNode recordType, String fieldName) {
        for (JsonNode field : recordType.get("fields")) {
            if (fieldName.equals(field.get("name").asText())) {
                return field;
            }
        }
        return null;
    }
}
