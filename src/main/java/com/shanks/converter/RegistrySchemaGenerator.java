package com.shanks.converter;

import com.shanks.model.AvroTypeInfo;
import org.apache.avro.Schema;

/**
 * Generates Avro schemas compatible with schema registries (e.g. IBM Schema Registry / Apicurio).
 *
 * Produces a single self-contained JSON object (not an array). Schema construction is delegated
 * to {@link AvroSchemaBuilder}, the same builder used by standard mode, then serialized with
 * {@code Schema.toString(true)} — Avro's own serializer already embeds named types inline at
 * their first occurrence and references them by full qualified name afterwards, and it enforces
 * Avro's naming/structural rules while doing so. This means a schema that fails to generate here
 * would also be rejected on publish by a real schema registry, instead of only surfacing there.
 *
 * This format is required by IBM Schema Registry and Confluent Schema Registry.
 */
public class RegistrySchemaGenerator {

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
     * Generate an IBM Schema Registry compatible Avro schema.
     *
     * @param rootType the root type information
     * @param rootName the name for the root record
     * @return single JSON object string (not a JSON array)
     */
    public String generateRegistrySchema(AvroTypeInfo rootType, String rootName) {
        Schema schema = new AvroSchemaBuilder(includeDoc).build(rootType, rootName, namespace);
        return schema.toString(true);
    }
}
