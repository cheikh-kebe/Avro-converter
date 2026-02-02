package com.shanks.mapper;

import com.shanks.model.AvroTypeInfo;
import com.shanks.parser.OpenApiParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiToAvroTypeMapperTest {

    private OpenApiParser parser;
    private OpenApiToAvroTypeMapper mapper;

    @BeforeEach
    void setUp() {
        parser = new OpenApiParser();
    }

    @Test
    void shouldMapStringFieldWithPattern() throws IOException {
        OpenAPI openAPI = parser.parse("test-openapi.yaml");
        mapper = new OpenApiToAvroTypeMapper(openAPI);

        Schema<?> contactInfoSchema = openAPI.getComponents().getSchemas().get("ContactInfo");
        assertThat(contactInfoSchema).isNotNull();

        Schema<?> phoneNumberSchema = contactInfoSchema.getProperties().get("phoneNumber");
        AvroTypeInfo phoneNumberType = mapper.mapSchema(phoneNumberSchema, "phoneNumber");

        assertThat(phoneNumberType).isNotNull();
        assertThat(phoneNumberType.getAvroType()).isEqualTo(org.apache.avro.Schema.Type.STRING);
        assertThat(phoneNumberType.getPattern()).isEqualTo("^\\+?[1-9]\\d{1,14}$");
    }

    @Test
    void shouldMapStringFieldWithPatternAndLogicalType() throws IOException {
        String yamlContent = """
                openapi: 3.0.3
                info:
                  title: Test API
                  version: 1.0.0
                components:
                  schemas:
                    TestSchema:
                      type: object
                      properties:
                        customId:
                          type: string
                          format: uuid
                          pattern: '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
                """;

        OpenAPI openAPI = parser.parseFromString(yamlContent);
        mapper = new OpenApiToAvroTypeMapper(openAPI);

        Schema<?> testSchema = openAPI.getComponents().getSchemas().get("TestSchema");
        Schema<?> customIdSchema = testSchema.getProperties().get("customId");
        AvroTypeInfo customIdType = mapper.mapSchema(customIdSchema, "customId");

        assertThat(customIdType).isNotNull();
        assertThat(customIdType.getAvroType()).isEqualTo(org.apache.avro.Schema.Type.STRING);
        assertThat(customIdType.getLogicalType()).isEqualTo("uuid");
        assertThat(customIdType.getPattern()).isEqualTo("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    }

    @Test
    void shouldMapStringFieldWithoutPattern() throws IOException {
        String yamlContent = """
                openapi: 3.0.3
                info:
                  title: Test API
                  version: 1.0.0
                components:
                  schemas:
                    TestSchema:
                      type: object
                      properties:
                        simpleString:
                          type: string
                """;

        OpenAPI openAPI = parser.parseFromString(yamlContent);
        mapper = new OpenApiToAvroTypeMapper(openAPI);

        Schema<?> testSchema = openAPI.getComponents().getSchemas().get("TestSchema");
        Schema<?> simpleStringSchema = testSchema.getProperties().get("simpleString");
        AvroTypeInfo simpleStringType = mapper.mapSchema(simpleStringSchema, "simpleString");

        assertThat(simpleStringType).isNotNull();
        assertThat(simpleStringType.getAvroType()).isEqualTo(org.apache.avro.Schema.Type.STRING);
        assertThat(simpleStringType.getPattern()).isNull();
    }

    @Test
    void shouldMapMultipleFieldsWithDifferentPatterns() throws IOException {
        OpenAPI openAPI = parser.parse("test-openapi.yaml");
        mapper = new OpenApiToAvroTypeMapper(openAPI);

        Schema<?> contactInfoSchema = openAPI.getComponents().getSchemas().get("ContactInfo");

        // Test phoneNumber
        Schema<?> phoneNumberSchema = contactInfoSchema.getProperties().get("phoneNumber");
        AvroTypeInfo phoneNumberType = mapper.mapSchema(phoneNumberSchema, "phoneNumber");
        assertThat(phoneNumberType.getPattern()).isEqualTo("^\\+?[1-9]\\d{1,14}$");

        // Test zipCode
        Schema<?> zipCodeSchema = contactInfoSchema.getProperties().get("zipCode");
        AvroTypeInfo zipCodeType = mapper.mapSchema(zipCodeSchema, "zipCode");
        assertThat(zipCodeType.getPattern()).isEqualTo("^\\d{5}(-\\d{4})?$");

        // Test username
        Schema<?> usernameSchema = contactInfoSchema.getProperties().get("username");
        AvroTypeInfo usernameType = mapper.mapSchema(usernameSchema, "username");
        assertThat(usernameType.getPattern()).isEqualTo("^[a-zA-Z0-9_-]{3,16}$");
    }

    @Test
    void shouldMapStringFieldWithDescription() throws IOException {
        String yamlContent = """
                openapi: 3.0.3
                info:
                  title: Test API
                  version: 1.0.0
                components:
                  schemas:
                    TestSchema:
                      type: object
                      properties:
                        firstName:
                          type: string
                          description: "The user's first name"
                """;

        OpenAPI openAPI = parser.parseFromString(yamlContent);
        mapper = new OpenApiToAvroTypeMapper(openAPI);

        Schema<?> testSchema = openAPI.getComponents().getSchemas().get("TestSchema");
        Schema<?> firstNameSchema = testSchema.getProperties().get("firstName");
        AvroTypeInfo firstNameType = mapper.mapSchema(firstNameSchema, "firstName");

        assertThat(firstNameType).isNotNull();
        assertThat(firstNameType.getAvroType()).isEqualTo(org.apache.avro.Schema.Type.STRING);
        assertThat(firstNameType.getDoc()).isEqualTo("The user's first name");
    }

    @Test
    void shouldMapObjectWithDescription() throws IOException {
        String yamlContent = """
                openapi: 3.0.3
                info:
                  title: Test API
                  version: 1.0.0
                components:
                  schemas:
                    User:
                      type: object
                      description: "Represents a user in the system"
                      properties:
                        id:
                          type: integer
                          description: "Unique identifier for the user"
                        name:
                          type: string
                          description: "Full name of the user"
                """;

        OpenAPI openAPI = parser.parseFromString(yamlContent);
        mapper = new OpenApiToAvroTypeMapper(openAPI);

        Schema<?> userSchema = openAPI.getComponents().getSchemas().get("User");
        AvroTypeInfo userType = mapper.mapSchema(userSchema, "User");

        assertThat(userType).isNotNull();
        assertThat(userType.getAvroType()).isEqualTo(org.apache.avro.Schema.Type.RECORD);
        assertThat(userType.getDoc()).isEqualTo("Represents a user in the system");

        // Check field descriptions
        AvroTypeInfo idField = userType.getFields().get("id");
        assertThat(idField.getDoc()).isEqualTo("Unique identifier for the user");

        AvroTypeInfo nameField = userType.getFields().get("name");
        assertThat(nameField.getDoc()).isEqualTo("Full name of the user");
    }

    @Test
    void shouldMapEnumWithDescription() throws IOException {
        String yamlContent = """
                openapi: 3.0.3
                info:
                  title: Test API
                  version: 1.0.0
                components:
                  schemas:
                    TestSchema:
                      type: object
                      properties:
                        status:
                          type: string
                          description: "Current status of the order"
                          enum:
                            - PENDING
                            - APPROVED
                            - REJECTED
                """;

        OpenAPI openAPI = parser.parseFromString(yamlContent);
        mapper = new OpenApiToAvroTypeMapper(openAPI);

        Schema<?> testSchema = openAPI.getComponents().getSchemas().get("TestSchema");
        Schema<?> statusSchema = testSchema.getProperties().get("status");
        AvroTypeInfo statusType = mapper.mapSchema(statusSchema, "status");

        assertThat(statusType).isNotNull();
        assertThat(statusType.getAvroType()).isEqualTo(org.apache.avro.Schema.Type.ENUM);
        assertThat(statusType.getDoc()).isEqualTo("Current status of the order");
    }
}
