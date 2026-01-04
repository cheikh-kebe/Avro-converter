package com.shanks.converter;

import com.shanks.model.AvroTypeInfo;
import org.apache.avro.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnifiedSchemaGeneratorPatternTest {

    private UnifiedSchemaGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new UnifiedSchemaGenerator();
    }

    @Test
    void shouldGenerateUnifiedSchemaWithPatternedFields() {
        AvroTypeInfo phoneNumberType = AvroTypeInfo.builder()
                .avroType(Schema.Type.STRING)
                .pattern("^\\+?[1-9]\\d{1,14}$")
                .build();

        AvroTypeInfo zipCodeType = AvroTypeInfo.builder()
                .avroType(Schema.Type.STRING)
                .pattern("^\\d{5}(-\\d{4})?$")
                .build();

        AvroTypeInfo recordType = AvroTypeInfo.builder()
                .avroType(Schema.Type.RECORD)
                .recordName("ContactInfo")
                .addField("phoneNumber", phoneNumberType)
                .addField("zipCode", zipCodeType)
                .build();

        String schemaJson = generator.generateUnifiedSchema(recordType, "ContactInfo");

        assertThat(schemaJson).isNotNull();
        assertThat(schemaJson).contains("\"pattern\"");
        // JSON escapes backslashes, so \+ becomes \\+
        assertThat(schemaJson).contains("^\\\\+?[1-9]\\\\d{1,14}$");
        assertThat(schemaJson).contains("^\\\\d{5}(-\\\\d{4})?$");
    }

    @Test
    void shouldGeneratePatternWithUUID() {
        AvroTypeInfo uuidWithPattern = AvroTypeInfo.builder()
                .avroType(Schema.Type.STRING)
                .logicalType("uuid")
                .pattern("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
                .build();

        AvroTypeInfo recordType = AvroTypeInfo.builder()
                .avroType(Schema.Type.RECORD)
                .recordName("TestRecord")
                .addField("id", uuidWithPattern)
                .build();

        String schemaJson = generator.generateUnifiedSchema(recordType, "TestRecord");

        assertThat(schemaJson).isNotNull();
        assertThat(schemaJson).contains("\"logicalType\": \"uuid\"");
        assertThat(schemaJson).contains("\"pattern\"");
        // Pattern without backslashes doesn't need escaping
        assertThat(schemaJson).contains("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    }

    @Test
    void shouldEscapePatternInUnifiedSchema() {
        AvroTypeInfo typeWithComplexPattern = AvroTypeInfo.builder()
                .avroType(Schema.Type.STRING)
                .pattern("^\"test\"\\s+pattern$")
                .build();

        AvroTypeInfo recordType = AvroTypeInfo.builder()
                .avroType(Schema.Type.RECORD)
                .recordName("TestRecord")
                .addField("field", typeWithComplexPattern)
                .build();

        String schemaJson = generator.generateUnifiedSchema(recordType, "TestRecord");

        assertThat(schemaJson).isNotNull();
        assertThat(schemaJson).contains("pattern");
        // Should contain escaped quotes and backslashes
        assertThat(schemaJson).contains("\\\"");
    }

    @Test
    void shouldNotIncludePatternWhenNotProvided() {
        AvroTypeInfo simpleString = AvroTypeInfo.builder()
                .avroType(Schema.Type.STRING)
                .build();

        AvroTypeInfo recordType = AvroTypeInfo.builder()
                .avroType(Schema.Type.RECORD)
                .recordName("TestRecord")
                .addField("name", simpleString)
                .build();

        String schemaJson = generator.generateUnifiedSchema(recordType, "TestRecord");

        assertThat(schemaJson).isNotNull();
        // The field should be just "string" without pattern
        assertThat(schemaJson).contains("\"name\": \"name\"");
        assertThat(schemaJson).contains("\"type\": \"string\"");
    }
}
