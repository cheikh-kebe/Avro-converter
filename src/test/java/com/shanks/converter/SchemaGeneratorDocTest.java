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
    void shouldGenerateRecordSchemaWithDoc() {
        AvroTypeInfo typeInfo = AvroTypeInfo.builder()
                .avroType(Schema.Type.RECORD)
                .recordName("User")
                .doc("Represents a user in the system")
                .addField("id", AvroTypeInfo.builder()
                        .avroType(Schema.Type.INT)
                        .build())
                .build();

        Schema schema = generator.generateSchema(typeInfo, "User");

        assertThat(schema).isNotNull();
        assertThat(schema.getType()).isEqualTo(Schema.Type.RECORD);
        assertThat(schema.getDoc()).isEqualTo("Represents a user in the system");
    }

    @Test
    void shouldGenerateRecordSchemaWithFieldDoc() {
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

        Schema schema = generator.generateSchema(recordType, "User");

        assertThat(schema).isNotNull();
        assertThat(schema.getType()).isEqualTo(Schema.Type.RECORD);

        // Check field docs
        Schema.Field idSchemaField = schema.getField("id");
        assertThat(idSchemaField).isNotNull();
        assertThat(idSchemaField.doc()).isEqualTo("Unique identifier for the user");

        Schema.Field nameSchemaField = schema.getField("name");
        assertThat(nameSchemaField).isNotNull();
        assertThat(nameSchemaField.doc()).isEqualTo("Full name of the user");
    }

    @Test
    void shouldGenerateEnumSchemaWithDoc() {
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
    void shouldGenerateSchemaWithoutDocWhenNotProvided() {
        AvroTypeInfo typeInfo = AvroTypeInfo.builder()
                .avroType(Schema.Type.RECORD)
                .recordName("User")
                .addField("id", AvroTypeInfo.builder()
                        .avroType(Schema.Type.INT)
                        .build())
                .build();

        Schema schema = generator.generateSchema(typeInfo, "User");

        assertThat(schema).isNotNull();
        assertThat(schema.getType()).isEqualTo(Schema.Type.RECORD);
        assertThat(schema.getDoc()).isNull();
    }

    @Test
    void shouldGenerateRecordSchemaWithBothDocAndFieldDoc() {
        AvroTypeInfo emailField = AvroTypeInfo.builder()
                .avroType(Schema.Type.STRING)
                .doc("User email address")
                .build();

        AvroTypeInfo recordType = AvroTypeInfo.builder()
                .avroType(Schema.Type.RECORD)
                .recordName("Contact")
                .doc("Contact information for a user")
                .addField("email", emailField)
                .build();

        Schema schema = generator.generateSchema(recordType, "Contact");

        assertThat(schema).isNotNull();
        assertThat(schema.getType()).isEqualTo(Schema.Type.RECORD);
        assertThat(schema.getDoc()).isEqualTo("Contact information for a user");

        Schema.Field emailSchemaField = schema.getField("email");
        assertThat(emailSchemaField).isNotNull();
        assertThat(emailSchemaField.doc()).isEqualTo("User email address");
    }
}
