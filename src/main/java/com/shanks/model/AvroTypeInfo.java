package com.shanks.model;

import org.apache.avro.Schema;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents metadata about an inferred Avro type.
 *
 * This class encapsulates all information needed to generate an Avro schema,
 * following the Single Responsibility Principle.
 */
public class AvroTypeInfo {

    private final Schema.Type avroType;
    private final boolean nullable;
    private final List<AvroTypeInfo> unionTypes;
    private final String logicalType;
    private final Map<String, AvroTypeInfo> fields;
    private final List<String> enumSymbols;
    private final AvroTypeInfo arrayItemType;
    private final String recordName;
    private final String pattern;
    private final String doc;

    private AvroTypeInfo(Builder builder) {
        this.avroType = builder.avroType;
        this.nullable = builder.nullable;
        this.unionTypes = builder.unionTypes;
        this.logicalType = builder.logicalType;
        this.fields = builder.fields;
        this.enumSymbols = builder.enumSymbols;
        this.arrayItemType = builder.arrayItemType;
        this.recordName = builder.recordName;
        this.pattern = builder.pattern;
        this.doc = builder.doc;
    }

    /**
     * @return the underlying Avro type (RECORD, STRING, UNION, ...)
     */
    public Schema.Type getAvroType() {
        return avroType;
    }

    /**
     * @return true if this type should be wrapped as a nullable union
     */
    public boolean isNullable() {
        return nullable;
    }

    /**
     * @return the member types when {@link #getAvroType()} is UNION, otherwise empty
     */
    public List<AvroTypeInfo> getUnionTypes() {
        return unionTypes;
    }

    /**
     * @return the Avro logical type name (e.g. "uuid"), or null if none
     */
    public String getLogicalType() {
        return logicalType;
    }

    /**
     * @return the field name to type mapping when {@link #getAvroType()} is RECORD
     */
    public Map<String, AvroTypeInfo> getFields() {
        return fields;
    }

    /**
     * @return the enum symbols when {@link #getAvroType()} is ENUM
     */
    public List<String> getEnumSymbols() {
        return enumSymbols;
    }

    /**
     * @return the element type when {@link #getAvroType()} is ARRAY
     */
    public AvroTypeInfo getArrayItemType() {
        return arrayItemType;
    }

    /**
     * @return the name for this named type (record or enum), or null if unnamed
     */
    public String getRecordName() {
        return recordName;
    }

    /**
     * @return the regex pattern constraint for a string type, or null if none
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * @return the doc string extracted from the source description, or null if none
     */
    public String getDoc() {
        return doc;
    }

    /**
     * Builder for AvroTypeInfo following the Builder pattern.
     */
    public static class Builder {
        private Schema.Type avroType;
        private boolean nullable = false;
        private List<AvroTypeInfo> unionTypes = new ArrayList<>();
        private String logicalType;
        private Map<String, AvroTypeInfo> fields = new LinkedHashMap<>();
        private List<String> enumSymbols = new ArrayList<>();
        private AvroTypeInfo arrayItemType;
        private String recordName;
        private String pattern;
        private String doc;

        /** Set the underlying Avro type. */
        public Builder avroType(Schema.Type avroType) {
            this.avroType = avroType;
            return this;
        }

        /** Mark this type as nullable. */
        public Builder nullable(boolean nullable) {
            this.nullable = nullable;
            return this;
        }

        /** Set the full list of union member types, replacing any previously added. */
        public Builder unionTypes(List<AvroTypeInfo> unionTypes) {
            this.unionTypes = unionTypes;
            return this;
        }

        /** Append a single union member type. */
        public Builder addUnionType(AvroTypeInfo typeInfo) {
            this.unionTypes.add(typeInfo);
            return this;
        }

        /** Set the Avro logical type name (e.g. "uuid"). */
        public Builder logicalType(String logicalType) {
            this.logicalType = logicalType;
            return this;
        }

        /** Set the full field map for a record type, replacing any previously added. */
        public Builder fields(Map<String, AvroTypeInfo> fields) {
            this.fields = fields;
            return this;
        }

        /** Add a single field to a record type. */
        public Builder addField(String name, AvroTypeInfo typeInfo) {
            this.fields.put(name, typeInfo);
            return this;
        }

        /** Set the full list of enum symbols, replacing any previously added. */
        public Builder enumSymbols(List<String> symbols) {
            this.enumSymbols = symbols;
            return this;
        }

        /** Append a single enum symbol if not already present. */
        public Builder addEnumSymbol(String symbol) {
            if (!this.enumSymbols.contains(symbol)) {
                this.enumSymbols.add(symbol);
            }
            return this;
        }

        /** Set the element type for an array type. */
        public Builder arrayItemType(AvroTypeInfo itemType) {
            this.arrayItemType = itemType;
            return this;
        }

        /** Set the name for a named type (record or enum). */
        public Builder recordName(String recordName) {
            this.recordName = recordName;
            return this;
        }

        /** Set the regex pattern constraint for a string type. */
        public Builder pattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        /** Set the doc string extracted from the source description. */
        public Builder doc(String doc) {
            this.doc = doc;
            return this;
        }

        /**
         * Build the {@link AvroTypeInfo} instance.
         *
         * @return the built type information
         * @throws NullPointerException if {@link #avroType(Schema.Type)} was never set
         */
        public AvroTypeInfo build() {
            Objects.requireNonNull(avroType, "avroType must not be null");
            return new AvroTypeInfo(this);
        }
    }

    /**
     * @return a new builder for {@link AvroTypeInfo}
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "AvroTypeInfo{" +
                "avroType=" + avroType +
                ", nullable=" + nullable +
                ", logicalType='" + logicalType + '\'' +
                ", recordName='" + recordName + '\'' +
                '}';
    }
}
