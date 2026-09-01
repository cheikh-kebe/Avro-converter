package com.shanks.converter;

import com.shanks.model.AvroTypeInfo;
import org.apache.avro.Schema;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvroSchemaBuilderTest {

    @Test
    void build_throwsReadableError_whenFieldNameStartsWithDigit() {
        AvroTypeInfo root = AvroTypeInfo.builder()
                .avroType(Schema.Type.RECORD)
                .recordName("User")
                .addField("1stName", AvroTypeInfo.builder().avroType(Schema.Type.STRING).build())
                .build();

        assertThatThrownBy(() -> new AvroSchemaBuilder(false).build(root, "User", "com.shanks.generated"))
                .isInstanceOf(AvroSchemaValidationException.class)
                .hasMessageContaining("1stName")
                .hasMessageContaining("field name");
    }

    @Test
    void build_throwsReadableError_whenRecordNameStartsWithDigit() {
        AvroTypeInfo root = AvroTypeInfo.builder()
                .avroType(Schema.Type.RECORD)
                .recordName("1User")
                .build();

        assertThatThrownBy(() -> new AvroSchemaBuilder(false).build(root, "1User", "com.shanks.generated"))
                .isInstanceOf(AvroSchemaValidationException.class)
                .hasMessageContaining("1User")
                .hasMessageContaining("record name");
    }

    @Test
    void build_succeeds_forValidRecordAndFieldNames() {
        AvroTypeInfo root = AvroTypeInfo.builder()
                .avroType(Schema.Type.RECORD)
                .recordName("User")
                .addField("firstName", AvroTypeInfo.builder().avroType(Schema.Type.STRING).build())
                .build();

        Schema schema = new AvroSchemaBuilder(false).build(root, "User", "com.shanks.generated");

        assertThat(schema.getFullName()).isEqualTo("com.shanks.generated.User");
        assertThat(schema.getField("firstName")).isNotNull();
    }
}
