package com.shanks.converter;

import com.shanks.model.AvroTypeInfo;
import org.apache.avro.Schema;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SchemaGeneratorTest {

    private final SchemaGenerator generator = new SchemaGenerator();

    private static AvroTypeInfo cardTypeRecord(List<String> symbols) {
        AvroTypeInfo enumType = AvroTypeInfo.builder()
                .avroType(Schema.Type.ENUM)
                .recordName("CardType")
                .enumSymbols(symbols)
                .build();

        return AvroTypeInfo.builder()
                .avroType(Schema.Type.RECORD)
                .addField("cardType", enumType)
                .build();
    }

    @Test
    void generateSchemaJson_throwsReadableError_whenEnumSymbolStartsWithDigit() {
        AvroTypeInfo root = cardTypeRecord(List.of("1RED", "BLUE"));

        assertThatThrownBy(() -> generator.generateSchemaJson(root, "CreditCard", ConversionOptions.defaults()))
                .isInstanceOf(AvroSchemaValidationException.class)
                .hasMessageContaining("1RED")
                .hasMessageContaining("enum symbol");
    }

    @Test
    void generateSchemaJson_succeeds_whenEnumSymbolsAreValid() {
        AvroTypeInfo root = cardTypeRecord(List.of("RED", "BLUE"));

        String schema = generator.generateSchemaJson(root, "CreditCard", ConversionOptions.defaults());

        assertThat(schema).contains("\"type\" : \"enum\"");
        assertThat(schema).contains("\"RED\"");
    }

    @Test
    void generateSchemaJson_isSelfContained_andDedupesRepeatedNamedTypeByFullName() {
        AvroTypeInfo address = AvroTypeInfo.builder()
                .avroType(Schema.Type.RECORD)
                .recordName("Address")
                .addField("city", AvroTypeInfo.builder().avroType(Schema.Type.STRING).build())
                .build();

        AvroTypeInfo root = AvroTypeInfo.builder()
                .avroType(Schema.Type.RECORD)
                .addField("shippingAddress", address)
                .addField("billingAddress", address)
                .build();

        String schema = generator.generateSchemaJson(root, "Order", ConversionOptions.defaults());

        assertThat(schema.trim()).startsWith("{");
        assertThat(schema).containsOnlyOnce("\"name\" : \"Address\"");
        assertThat(schema).contains("\"com.shanks.generated.order.Address\"");
    }

    @Test
    void generateSchema_appliesFunctionalPerimeterToRootAndNestedNamespaces() {
        AvroTypeInfo root = cardTypeRecord(List.of("RED"));

        Schema schema = generator.generateSchema(root, "CreditCard",
                ConversionOptions.defaults().withFunctionalPerimeter("payments"));

        assertThat(schema.getNamespace()).isEqualTo("com.shanks.generated.payments");
        assertThat(schema.getField("cardType").schema().getFullName())
                .isEqualTo("com.shanks.generated.payments.creditcard.CardType");
    }

    @Test
    void conversionOptions_normalizesBlankValues() {
        ConversionOptions options = new ConversionOptions(false, "  ", " ");

        assertThat(options.functionalPerimeter()).isNull();
        assertThat(options.namespace()).isEqualTo(ConversionOptions.DEFAULT_NAMESPACE);
        assertThat(options.envelope()).isEqualTo(ConversionOptions.DEFAULT_ENVELOPE);
    }
}
