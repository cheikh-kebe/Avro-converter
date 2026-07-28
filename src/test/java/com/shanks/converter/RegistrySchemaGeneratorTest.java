package com.shanks.converter;

import com.shanks.model.AvroTypeInfo;
import org.apache.avro.Schema;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistrySchemaGeneratorTest {

    @Test
    void generateRegistrySchema_throwsSameAvroErrorAsStandardMode_whenEnumSymbolStartsWithDigit() {
        AvroTypeInfo enumType = AvroTypeInfo.builder()
                .avroType(Schema.Type.ENUM)
                .recordName("CardType")
                .enumSymbols(List.of("1RED", "BLUE"))
                .build();

        AvroTypeInfo root = AvroTypeInfo.builder()
                .avroType(Schema.Type.RECORD)
                .addField("cardType", enumType)
                .build();

        RegistrySchemaGenerator registryGenerator = new RegistrySchemaGenerator();

        assertThatThrownBy(() -> registryGenerator.generateRegistrySchema(root, "CreditCard"))
                .isInstanceOf(AvroSchemaValidationException.class)
                .hasMessageContaining("1RED")
                .hasMessageContaining("enum symbol");
    }

    @Test
    void generateRegistrySchema_succeeds_whenEnumSymbolsAreValid() {
        AvroTypeInfo enumType = AvroTypeInfo.builder()
                .avroType(Schema.Type.ENUM)
                .recordName("CardType")
                .enumSymbols(List.of("RED", "BLUE"))
                .build();

        AvroTypeInfo root = AvroTypeInfo.builder()
                .avroType(Schema.Type.RECORD)
                .addField("cardType", enumType)
                .build();

        RegistrySchemaGenerator registryGenerator = new RegistrySchemaGenerator();

        String schema = registryGenerator.generateRegistrySchema(root, "CreditCard");

        assertThat(schema).contains("\"type\" : \"enum\"");
        assertThat(schema).contains("\"RED\"");
    }

    @Test
    void generateRegistrySchema_dedupesRepeatedNamedType_byFullyQualifiedNameReference() {
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

        RegistrySchemaGenerator registryGenerator = new RegistrySchemaGenerator();

        String schema = registryGenerator.generateRegistrySchema(root, "Order");

        assertThat(schema).containsOnlyOnce("\"name\" : \"Address\"");
        assertThat(schema).contains("\"com.shanks.generated.order.Address\"");
    }
}
