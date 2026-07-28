package com.shanks.converter;

/**
 * Signals that a mapped type cannot be turned into a valid Avro schema — an invalid
 * record/field/enum name, an invalid enum symbol, or any other Avro naming/structural
 * rule violation caught while building a {@link org.apache.avro.Schema}.
 *
 * The message is meant to be shown directly to the user (CLI), not just logged: it names
 * the offending value, where it was found, and how to fix it in the source OpenAPI/JSON.
 */
public class AvroSchemaValidationException extends RuntimeException {

    public AvroSchemaValidationException(String message) {
        super(message);
    }

    public AvroSchemaValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
