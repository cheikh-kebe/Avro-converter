package com.shanks.converter;

/**
 * Thrown when a notif envelope template cannot be used: no template with that name on the
 * classpath, a template that is not valid JSON, or a template with no {@code payload} field.
 *
 * Carries a human-readable message that the CLI prints as-is, without a stack trace.
 */
public class InvalidEnvelopeException extends RuntimeException {

    public InvalidEnvelopeException(String message) {
        super(message);
    }

    public InvalidEnvelopeException(String message, Throwable cause) {
        super(message, cause);
    }
}
