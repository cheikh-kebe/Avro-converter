package com.shanks.converter;

import com.shanks.model.AvroTypeInfo;
import org.apache.avro.Schema;

/**
 * Generates Avro schemas from inferred/mapped type information.
 *
 * Schema construction itself is delegated to {@link AvroSchemaBuilder}; this class only applies
 * the {@link ConversionOptions} (doc fields, functional perimeter namespace) and serializes.
 *
 * The output is a single self-contained JSON object: {@code Schema.toString(true)} embeds each
 * named type inline at its first occurrence and references it by full name afterwards, which is
 * the format IBM Schema Registry and Confluent Schema Registry expect. Standard and registry mode
 * therefore share this one generator.
 */
public class SchemaGenerator {

    /**
     * Generate an Avro schema from the root type information, with default options.
     *
     * @param rootType   the root type information
     * @param recordName the name for the root record
     * @return the generated Avro schema
     */
    public Schema generateSchema(AvroTypeInfo rootType, String recordName) {
        return generateSchema(rootType, recordName, ConversionOptions.defaults());
    }

    /**
     * Generate an Avro schema from the root type information.
     *
     * @param rootType   the root type information
     * @param recordName the name for the root record
     * @param options    doc fields and namespace to apply
     * @return the generated Avro schema
     */
    public Schema generateSchema(AvroTypeInfo rootType, String recordName, ConversionOptions options) {
        return new AvroSchemaBuilder(options.includeDoc()).build(rootType, recordName, options.namespace());
    }

    /**
     * Generate an Avro schema from the root type information and return it as
     * pretty-printed JSON.
     *
     * @param rootType   the root type information
     * @param recordName the name for the root record
     * @param options    doc fields and namespace to apply
     * @return the pretty-printed JSON string
     */
    public String generateSchemaJson(AvroTypeInfo rootType, String recordName, ConversionOptions options) {
        return generateSchema(rootType, recordName, options).toString(true);
    }
}
