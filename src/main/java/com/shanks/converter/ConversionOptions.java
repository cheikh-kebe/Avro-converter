package com.shanks.converter;

/**
 * Options shared by every schema conversion (OpenAPI or JSON input): whether to emit
 * {@code doc} fields, the functional perimeter appended to the root namespace, and the
 * notif envelope template used for the {@code .webhook.avsc} output.
 *
 * Immutable — derive variants with the {@code with*} methods.
 *
 * @param includeDoc          true to include doc fields from OpenAPI descriptions
 * @param functionalPerimeter the functional perimeter name, or null/blank for none
 * @param envelope            the envelope template name (see {@link NotifWrapperGenerator})
 */
public record ConversionOptions(boolean includeDoc, String functionalPerimeter, String envelope) {

    /** Root namespace of every generated schema, before any functional perimeter. */
    public static final String DEFAULT_NAMESPACE = "com.shanks.generated";

    /** Envelope template used when none is selected. */
    public static final String DEFAULT_ENVELOPE = "default";

    public ConversionOptions {
        functionalPerimeter = functionalPerimeter == null || functionalPerimeter.isBlank()
                ? null
                : functionalPerimeter.trim();
        envelope = envelope == null || envelope.isBlank() ? DEFAULT_ENVELOPE : envelope.trim();
    }

    /** @return no doc fields, no functional perimeter, default envelope */
    public static ConversionOptions defaults() {
        return new ConversionOptions(false, null, DEFAULT_ENVELOPE);
    }

    public ConversionOptions withIncludeDoc(boolean includeDoc) {
        return new ConversionOptions(includeDoc, functionalPerimeter, envelope);
    }

    public ConversionOptions withFunctionalPerimeter(String functionalPerimeter) {
        return new ConversionOptions(includeDoc, functionalPerimeter, envelope);
    }

    public ConversionOptions withEnvelope(String envelope) {
        return new ConversionOptions(includeDoc, functionalPerimeter, envelope);
    }

    /**
     * @return the root namespace: {@code com.shanks.generated}, or
     *         {@code com.shanks.generated.<functionalPerimeter>} when a perimeter is set
     */
    public String namespace() {
        return functionalPerimeter == null ? DEFAULT_NAMESPACE : DEFAULT_NAMESPACE + "." + functionalPerimeter;
    }
}
