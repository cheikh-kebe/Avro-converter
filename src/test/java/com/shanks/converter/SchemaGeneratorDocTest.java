package com.shanks.converter;

import com.shanks.model.AvroTypeInfo;
import org.apache.avro.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaGeneratorDocTest {

    private SchemaGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new SchemaGenerator();
    }

    @Test
    void shouldIncludeDocWhenEnabled() {
        generator.setIncludeDoc(true);

        AvroTypeInfo typeInfo = AvroTypeInfo.builder()
                .avroType(Schema.Type.RECORD)
                .recordName("User")
                .doc("Represents a user in the system")
                .addField("id", AvroTypeInfo.builder()
                        .avroType(Schema.Type.INT)
                        .doc("Unique identifier for the user")
                        .build())
                .addField("name", AvroTypeInfo.builder()
                        .avroType(Schema.Type.STRING)
                        .doc("Full name of the user")
                        .build())
                .build();

        Schema schema = generator.generateSchema(typeInfo, "User");

        assertThat(schema).isNotNull();
        assertThat(schema.getDoc()).isEqualTo("Represents a user in the system");

        Schema.Field idField = schema.getField("id");
        assertThat(idField.doc()).isEqualTo("Unique identifier for the user");

        Schema.Field nameField = schema.getField("name");
        assertThat(nameField.doc()).isEqualTo("Full name of the user");
    }

    @Test
    void shouldExcludeDocWhenDisabled() {
        generator.setIncludeDoc(false);

        AvroTypeInfo typeInfo = AvroTypeInfo.builder()
                .avroType(Schema.Type.RECORD)
                .recordName("User")
                .doc("Represents a user in the system")
                .addField("id", AvroTypeInfo.builder()
                        .avroType(Schema.Type.INT)
                        .doc("Unique identifier for the user")
                        .build())
                .build();

        Schema schema = generator.generateSchema(typeInfo, "User");

        assertThat(schema).isNotNull();
        assertThat(schema.getDoc()).isNull();

        Schema.Field idField = schema.getField("id");
        assertThat(idField.doc()).isNull();
    }

    @Test
    void shouldIncludeEnumDocWhenEnabled() {
        generator.setIncludeDoc(true);

        AvroTypeInfo typeInfo = AvroTypeInfo.builder()
                .avroType(Schema.Type.ENUM)
                .recordName("Status")
                .doc("Current status of the order")
                .addEnumSymbol("PENDING")
                .addEnumSymbol("APPROVED")
                .addEnumSymbol("REJECTED")
                .build();

        Schema schema = generator.generateSchema(typeInfo, "Status");

        assertThat(schema).isNotNull();
        assertThat(schema.getType()).isEqualTo(Schema.Type.ENUM);
        assertThat(schema.getDoc()).isEqualTo("Current status of the order");
    }

    @Test
    void shouldExcludeEnumDocWhenDisabled() {
        generator.setIncludeDoc(false);

        AvroTypeInfo typeInfo = AvroTypeInfo.builder()
                .avroType(Schema.Type.ENUM)
                .recordName("Status")
                .doc("Current status of the order")
                .addEnumSymbol("PENDING")
                .addEnumSymbol("APPROVED")
                .build();

        Schema schema = generator.generateSchema(typeInfo, "Status");

        assertThat(schema).isNotNull();
        assertThat(schema.getType()).isEqualTo(Schema.Type.ENUM);
        assertThat(schema.getDoc()).isNull();
    }

    @Test
    void shouldDefaultToExcludeDoc() {
        AvroTypeInfo typeInfo = AvroTypeInfo.builder()
                .avroType(Schema.Type.RECORD)
                .recordName("User")
                .doc("Should not appear")
                .addField("id", AvroTypeInfo.builder()
                        .avroType(Schema.Type.INT)
                        .build())
                .build();

        Schema schema = generator.generateSchema(typeInfo, "User");

        assertThat(schema.getDoc()).isNull();
    }
}
