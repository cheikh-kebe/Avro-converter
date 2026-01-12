package com.shanks.converter;

import com.shanks.model.AvroTypeInfo;
import org.apache.avro.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnifiedSchemaGeneratorDocTest {

    private UnifiedSchemaGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new UnifiedSchemaGenerator();
    }

    @Test
    void shouldGenerateUnifiedSchemaWithRecordDoc() {
        AvroTypeInfo recordType = AvroTypeInfo.builder()
                .avroType(Schema.Type.RECORD)
                .recordName("User")
                .doc("Represents a user in the system")
                .addField("id", AvroTypeInfo.builder()
                        .avroType(Schema.Type.INT)
                        .build())
                .build();

        String schemaJson = generator.generateUnifiedSchema(recordType, "User");

        assertThat(schemaJson).isNotNull();
        assertThat(schemaJson).contains("\"doc\": \"Represents a user in the system\"");
    }

    @Test
    void shouldGenerateUnifiedSchemaWithFieldDoc() {
        AvroTypeInfo idField = AvroTypeInfo.builder()
                .avroType(Schema.Type.INT)
                .doc("Unique identifier for the user")
                .build();

        AvroTypeInfo nameField = AvroTypeInfo.builder()
                .avroType(Schema.Type.STRING)
                .doc("Full name of the user")
                .build();

        AvroTypeInfo recordType = AvroTypeInfo.builder()
                .avroType(Schema.Type.RECORD)
                .recordName("User")
                .addField("id", idField)
                .addField("name", nameField)
                .build();

        String schemaJson = generator.generateUnifiedSchema(recordType, "User");

        assertThat(schemaJson).isNotNull();
        assertThat(schemaJson).contains("\"doc\": \"Unique identifier for the user\"");
        assertThat(schemaJson).contains("\"doc\": \"Full name of the user\"");
    }

    @Test
    void shouldGenerateUnifiedSchemaWithEnumDoc() {
        AvroTypeInfo statusEnum = AvroTypeInfo.builder()
                .avroType(Schema.Type.ENUM)
                .recordName("StatusEnum")
                .doc("Current status of the order")
                .addEnumSymbol("PENDING")
                .addEnumSymbol("APPROVED")
                .addEnumSymbol("REJECTED")
                .build();

        AvroTypeInfo recordType = AvroTypeInfo.builder()
                .avroType(Schema.Type.RECORD)
                .recordName("Order")
                .addField("status", statusEnum)
                .build();

        String schemaJson = generator.generateUnifiedSchema(recordType, "Order");

        assertThat(schemaJson).isNotNull();
        assertThat(schemaJson).contains("\"doc\": \"Current status of the order\"");
    }

    @Test
    void shouldEscapeSpecialCharactersInDoc() {
        AvroTypeInfo recordType = AvroTypeInfo.builder()
                .avroType(Schema.Type.RECORD)
                .recordName("TestRecord")
                .doc("This is a \"quoted\" description with\nnewline and\ttab")
                .addField("field", AvroTypeInfo.builder()
                        .avroType(Schema.Type.STRING)
                        .build())
                .build();

        String schemaJson = generator.generateUnifiedSchema(recordType, "TestRecord");

        assertThat(schemaJson).isNotNull();
        // Should contain escaped quotes, newlines, and tabs
        assertThat(schemaJson).contains("\\\"quoted\\\"");
        assertThat(schemaJson).contains("\\n");
        assertThat(schemaJson).contains("\\t");
    }

    @Test
    void shouldGenerateUnifiedSchemaWithBothDocAndFieldDoc() {
        AvroTypeInfo emailField = AvroTypeInfo.builder()
                .avroType(Schema.Type.STRING)
                .doc("User email address")
                .build();

        AvroTypeInfo phoneField = AvroTypeInfo.builder()
                .avroType(Schema.Type.STRING)
                .doc("User phone number")
                .build();

        AvroTypeInfo recordType = AvroTypeInfo.builder()
                .avroType(Schema.Type.RECORD)
                .recordName("Contact")
                .doc("Contact information for a user")
                .addField("email", emailField)
                .addField("phone", phoneField)
                .build();

        String schemaJson = generator.generateUnifiedSchema(recordType, "Contact");

        assertThat(schemaJson).isNotNull();
        assertThat(schemaJson).contains("\"doc\": \"Contact information for a user\"");
        assertThat(schemaJson).contains("\"doc\": \"User email address\"");
        assertThat(schemaJson).contains("\"doc\": \"User phone number\"");
    }

    @Test
    void shouldNotIncludeDocWhenNotProvided() {
        AvroTypeInfo recordType = AvroTypeInfo.builder()
                .avroType(Schema.Type.RECORD)
                .recordName("SimpleRecord")
                .addField("name", AvroTypeInfo.builder()
                        .avroType(Schema.Type.STRING)
                        .build())
                .build();

        String schemaJson = generator.generateUnifiedSchema(recordType, "SimpleRecord");

        assertThat(schemaJson).isNotNull();
        // Should not contain any doc fields
        assertThat(schemaJson).doesNotContain("\"doc\":");
    }
}
