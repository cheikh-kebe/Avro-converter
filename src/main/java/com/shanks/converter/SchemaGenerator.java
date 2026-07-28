package com.shanks.converter;

import com.shanks.model.AvroTypeInfo;
import org.apache.avro.Schema;

/**
 * Generates Avro schemas from inferred type information, as separate per-schema output files.
 *
 * Schema construction itself is delegated to {@link AvroSchemaBuilder}; this class only owns
 * the standard-mode configuration (doc fields, functional perimeter) and serialization.
 */
public class SchemaGenerator {

    private static final String DEFAULT_NAMESPACE = "com.shanks.generated";
    private boolean includeDoc = false;
    private String namespace = DEFAULT_NAMESPACE;

    /**
     * Set whether to include doc fields in generated schemas.
     *
     * @param includeDoc true to include doc fields from OpenAPI descriptions
     */
    public void setIncludeDoc(boolean includeDoc) {
        this.includeDoc = includeDoc;
    }

    /**
     * Set the functional perimeter to append to the default namespace
     * (e.g. "users" produces "com.shanks.generated.users").
     *
     * @param functionalPerimeter the functional perimeter name, or null/blank to reset to the default namespace
     */
    public void setFunctionalPerimeter(String functionalPerimeter) {
        this.namespace = (functionalPerimeter == null || functionalPerimeter.trim().isEmpty())
                ? DEFAULT_NAMESPACE
                : DEFAULT_NAMESPACE + "." + functionalPerimeter.trim();
    }

    /**
     * Generate an Avro schema from the root type information.
     *
     * @param rootType   the root type information
     * @param recordName the name for the root record
     * @return the generated Avro schema
     */
    public Schema generateSchema(AvroTypeInfo rootType, String recordName) {
        return new AvroSchemaBuilder(includeDoc).build(rootType, recordName, namespace);
    }

    /**
     * Convert the schema to pretty-printed JSON.
     *
     * @param schema the Avro schema
     * @return the pretty-printed JSON string
     */
    public String toPrettyJson(Schema schema) {
        return schema.toString(true);
    }

    /**
     * Generate an Avro schema from the root type information and return it as
     * pretty-printed JSON.
     *
     * @param rootType   the root type information
     * @param recordName the name for the root record
     * @return the pretty-printed JSON string
     */
    public String generateSchemaJson(AvroTypeInfo rootType, String recordName) {
        return generateSchema(rootType, recordName).toString(true);
    }
}
