package com.shanks.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Wraps a generated Avro schema into an envelope template loaded from
 * {@code src/main/resources/envelopes/<name>.json}. The envelope's structure is
 * opaque to this class — the only contract is that the template contains a field
 * named "payload" somewhere in its tree, located via a recursive search, whose
 * "type" is replaced with the generated schema.
 */
class NotifWrapperGenerator {

    private static final String NAMESPACE_TOKEN = "${namespace}";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Wrap the given generated schema JSON into the default notif envelope.
     *
     * @param schemaJson the already-generated Avro schema JSON (payload content)
     * @return the pretty-printed consolidated wrapper JSON
     * @throws IOException if the schema JSON or the envelope template cannot be parsed
     */
    static String wrap(String schemaJson) throws IOException {
        return wrap(schemaJson, ConversionOptions.DEFAULT_ENVELOPE);
    }

    /**
     * Check that the named envelope template exists, is valid JSON and contains a
     * "payload" field, without wrapping anything. Lets callers fail before writing
     * any output file.
     *
     * @param envelopeName the envelope template name
     * @throws InvalidEnvelopeException if the template is missing, malformed, or has no "payload" field
     */
    static void validate(String envelopeName) {
        JsonNode envelope = parseTemplate(envelopeName, ConversionOptions.DEFAULT_NAMESPACE);
        if (!injectPayload(envelope, MAPPER.nullNode())) {
            throw missingPayload(envelopeName);
        }
    }

    /**
     * Wrap the given generated schema JSON into the named envelope template.
     *
     * @param schemaJson   the already-generated Avro schema JSON (payload content)
     * @param envelopeName the envelope template name (loaded from
     *                     {@code envelopes/<envelopeName>.json} on the classpath)
     * @return the pretty-printed consolidated wrapper JSON
     * @throws IOException if the schema JSON cannot be parsed
     * @throws InvalidEnvelopeException if the envelope template is missing, malformed,
     *                                  or has no "payload" field
     */
    static String wrap(String schemaJson, String envelopeName) throws IOException {
        JsonNode payloadType = MAPPER.readTree(schemaJson);
        String baseNamespace = payloadType.has("namespace")
                ? payloadType.get("namespace").asText()
                : ConversionOptions.DEFAULT_NAMESPACE;

        JsonNode envelope = parseTemplate(envelopeName, baseNamespace);
        if (!injectPayload(envelope, payloadType)) {
            throw missingPayload(envelopeName);
        }

        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(envelope);
    }

    /** Load the named template, substitute the namespace token, and parse it. */
    private static JsonNode parseTemplate(String envelopeName, String namespace) {
        String templateText = loadEnvelopeTemplate(envelopeName).replace(NAMESPACE_TOKEN, namespace);
        try {
            return MAPPER.readTree(templateText);
        } catch (IOException e) {
            throw new InvalidEnvelopeException(
                    "Envelope template '" + envelopeName + "' is not valid JSON: " + e.getMessage(), e);
        }
    }

    private static InvalidEnvelopeException missingPayload(String envelopeName) {
        return new InvalidEnvelopeException(
                "Envelope template '" + envelopeName + "' does not contain a 'payload' field");
    }

    /** Load the raw text of an envelope template from the classpath. */
    private static String loadEnvelopeTemplate(String envelopeName) {
        String resourcePath = "/envelopes/" + envelopeName + ".json";
        try (InputStream in = NotifWrapperGenerator.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new InvalidEnvelopeException("Envelope '" + envelopeName + "' not found, expected "
                        + "src/main/resources" + resourcePath + " on the classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new InvalidEnvelopeException("Envelope '" + envelopeName + "' could not be read: "
                    + e.getMessage(), e);
        }
    }

    /**
     * Recursively search {@code node} for an Avro field definition named "payload"
     * (an object with a "name" of "payload" and a sibling "type" key) and replace its
     * "type" with {@code payloadType}. Stops at the first match.
     *
     * @return true if a "payload" field was found and injected
     */
    private static boolean injectPayload(JsonNode node, JsonNode payloadType) {
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            if (object.has("name") && object.has("type")
                    && "payload".equals(object.get("name").asText())) {
                object.set("type", payloadType);
                return true;
            }
        }

        for (JsonNode child : node) {
            if (injectPayload(child, payloadType)) {
                return true;
            }
        }

        return false;
    }
}
