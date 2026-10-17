package com.shanks.mapper;

import com.shanks.converter.AvroSchemaValidationException;
import com.shanks.converter.ConversionOptions;
import com.shanks.converter.SchemaGenerator;
import com.shanks.model.AvroTypeInfo;
import com.shanks.parser.OpenApiParser;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A deprecated field whose enum can't be expressed in Avro (e.g. a symbol starting with a
 * digit) is dropped instead of failing the conversion; everything else is unchanged.
 */
class DeprecatedInvalidEnumTest {

    /** Original spec: invalid enum, not deprecated. */
    private static final String SPEC_V1 = """
            openapi: 3.0.3
            info: {title: Test, version: 1.0.0}
            paths: {}
            components:
              schemas:
                Order:
                  type: object
                  required: [id]
                  properties:
                    id:
                      type: string
                    travelClass:
                      type: string
                      enum: [1ST_CLASS, 2ND_CLASS]
            """;

    /** Patched spec: invalid field marked deprecated and replaced by a valid one. */
    private static final String SPEC_PATCHED = """
            openapi: 3.0.3
            info: {title: Test, version: 1.0.1}
            paths: {}
            components:
              schemas:
                Order:
                  type: object
                  required: [id]
                  properties:
                    id:
                      type: string
                    travelClass:
                      type: string
                      enum: [1ST_CLASS, 2ND_CLASS]
                      deprecated: true
                      description: DEPRECATED - use travelClassCode instead
                      x-replaced-by: travelClassCode
                    travelClassCode:
                      type: string
                      enum: [FIRST_CLASS, SECOND_CLASS]
            """;

    private final OpenApiParser parser = new OpenApiParser();

    private AvroTypeInfo map(String yaml, String schemaName) {
        OpenAPI openAPI;
        try {
            openAPI = parser.parseFromString(yaml);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new OpenApiToAvroTypeMapper(openAPI)
                .mapSchema(openAPI.getComponents().getSchemas().get(schemaName), schemaName);
    }

    @Test
    void originalSpec_stillFails_whenInvalidEnumIsNotDeprecated() {
        AvroTypeInfo order = map(SPEC_V1, "Order");

        assertThatThrownBy(() -> new SchemaGenerator().generateSchemaJson(order, "Order", ConversionOptions.defaults()))
                .isInstanceOf(AvroSchemaValidationException.class)
                .hasMessageContaining("1ST_CLASS");
    }

    @Test
    void patchedSpec_dropsDeprecatedInvalidEnumField() {
        AvroTypeInfo order = map(SPEC_PATCHED, "Order");

        assertThat(order.getFields()).containsOnlyKeys("id", "travelClassCode");

        String json = new SchemaGenerator().generateSchemaJson(order, "Order", ConversionOptions.defaults());
        assertThat(json).doesNotContain("travelClass\"").doesNotContain("1ST_CLASS");
        assertThat(json).contains("travelClassCode").contains("FIRST_CLASS");
    }

    @Test
    void patchedSpec_printsWarningNamingFieldAndReplacement() {
        PrintStream originalErr = System.err;
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));
        try {
            map(SPEC_PATCHED, "Order");
        } finally {
            System.setErr(originalErr);
        }

        assertThat(err.toString())
                .contains("Order.travelClass")
                .contains("1ST_CLASS")
                .contains("replaced by: travelClassCode");
    }

    @Test
    void specFiles_v1Fails_patchedConvertsWithoutDeprecatedInvalidFields() throws IOException {
        OpenAPI v1 = parser.parse("test-openapi-deprecated-v1.yaml");
        AvroTypeInfo v1Booking = new OpenApiToAvroTypeMapper(v1)
                .mapSchema(v1.getComponents().getSchemas().get("Booking"), "Booking");
        assertThatThrownBy(() -> new SchemaGenerator().generateSchemaJson(v1Booking, "Booking", ConversionOptions.defaults()))
                .isInstanceOf(AvroSchemaValidationException.class);

        OpenAPI patched = parser.parse("test-openapi-deprecated-patched.yaml");
        AvroTypeInfo booking = new OpenApiToAvroTypeMapper(patched)
                .mapSchema(patched.getComponents().getSchemas().get("Booking"), "Booking");

        // Dropped: travelClass (inline), ticketFormat ($ref), mealOptions (array).
        // Kept: replacements, the deprecated-but-valid "channel", and untouched fields.
        assertThat(booking.getFields()).containsOnlyKeys(
                "bookingId", "travelClassCode", "ticketFormatCode", "mealCodes",
                "channel", "salesChannel", "status");

        String json = new SchemaGenerator().generateSchemaJson(booking, "Booking", ConversionOptions.defaults());
        assertThat(json).doesNotContain("1ST_CLASS", "2D_BARCODE", "0_MEAL");
    }

    @Test
    void invalidEnum_stillFails_whenOnlyDescriptionAndReplacedByMarkIt() {
        AvroTypeInfo order = map("""
                openapi: 3.0.3
                info: {title: Test, version: 1.0.0}
                paths: {}
                components:
                  schemas:
                    Order:
                      type: object
                      properties:
                        travelClass:
                          type: string
                          enum: [1ST_CLASS, 2ND_CLASS]
                          description: DEPRECATED - use travelClassCode instead
                          x-replaced-by: travelClassCode
                """, "Order");

        assertThat(order.getFields()).containsKey("travelClass");
        assertThatThrownBy(() -> new SchemaGenerator().generateSchemaJson(order, "Order", ConversionOptions.defaults()))
                .isInstanceOf(AvroSchemaValidationException.class)
                .hasMessageContaining("1ST_CLASS");
    }

    @Test
    void deprecatedFieldWithValidEnum_isKept() {
        AvroTypeInfo order = map("""
                openapi: 3.0.3
                info: {title: Test, version: 1.0.0}
                paths: {}
                components:
                  schemas:
                    Order:
                      type: object
                      properties:
                        status:
                          type: string
                          enum: [OPEN, CLOSED]
                          deprecated: true
                          x-replaced-by: state
                """, "Order");

        assertThat(order.getFields()).containsKey("status");
    }

    @Test
    void deprecatedRefEnum_withInvalidSymbol_isDropped() {
        AvroTypeInfo order = map("""
                openapi: 3.0.3
                info: {title: Test, version: 1.0.0}
                paths: {}
                components:
                  schemas:
                    Order:
                      type: object
                      properties:
                        id:
                          type: string
                        travelClass:
                          $ref: '#/components/schemas/TravelClass'
                    TravelClass:
                      type: string
                      enum: [1ST_CLASS, 2ND_CLASS]
                      deprecated: true
                      description: DEPRECATED - use TravelClassCode
                      x-replaced-by: TravelClassCode
                """, "Order");

        assertThat(order.getFields()).containsOnlyKeys("id");
    }

    @Test
    void deprecatedArrayOfInvalidEnums_isDropped() {
        AvroTypeInfo order = map("""
                openapi: 3.0.3
                info: {title: Test, version: 1.0.0}
                paths: {}
                components:
                  schemas:
                    Order:
                      type: object
                      properties:
                        id:
                          type: string
                        classes:
                          type: array
                          deprecated: true
                          x-replaced-by: classCodes
                          items:
                            type: string
                            enum: [1ST_CLASS, 2ND_CLASS]
                """, "Order");

        assertThat(order.getFields()).containsOnlyKeys("id");
    }
}
