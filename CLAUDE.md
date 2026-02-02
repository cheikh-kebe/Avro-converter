# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Clean and build
mvn clean package

# Run application with JSON input
mvn exec:java -Dexec.mainClass="com.shanks.App" -Dexec.args="data.json schema.avsc"

# Run application with OpenAPI input (all schemas - separate files)
mvn exec:java -Dexec.mainClass="com.shanks.App" -Dexec.args="api.yaml output-dir/"

# Run application with OpenAPI input (specific schema - standard mode)
mvn exec:java -Dexec.mainClass="com.shanks.App" -Dexec.args="api.yaml User.avsc User"

# Run application with OpenAPI input (unified mode - recommended)
mvn exec:java -Dexec.mainClass="com.shanks.App" -Dexec.args="api.yaml ResultResponse.avsc ResultResponse --unified"

# Run tests
mvn test

# Run specific test class
mvn test -Dtest=OpenApiParserTest

# Run JAR directly (after build)
java -jar target/json-to-avro-converter.jar data.json schema.avsc

# Run JAR with unified mode
java -jar target/json-to-avro-converter.jar api.yaml ResultResponse.avsc ResultResponse --unified

# Generate Java classes from Avro schemas (automatic with Maven plugin)
mvn clean compile  # Generates classes during compile phase

# Just generate sources without compiling
mvn generate-sources

# Clean and rebuild with fresh generated classes
mvn clean install
```

## Project Information

- **Build Tool**: Maven
- **Group ID**: com.shanks
- **Main Class**: com.shanks.App
- **Java Version**: 21
- **Key Dependencies**:
  - Apache Avro 1.11.3 (schema handling)
  - Swagger Parser v3 2.1.22 (OpenAPI/Swagger parsing)
  - Jackson 2.16.1 (JSON parsing)
  - JUnit Jupiter 5.10.0
  - AssertJ 3.24.2
- **Build Plugins**:
  - avro-maven-plugin 1.11.3 (automatic Java class generation from .avsc schemas)

### Supported OpenAPI/Swagger Versions

The converter uses **Swagger Parser v3 (2.1.22)** which supports:

- ✅ **OpenAPI 3.0.x** (3.0.0, 3.0.1, 3.0.2, 3.0.3) - Fully supported and recommended
- ✅ **OpenAPI 3.1.x** - Partial support (some new features may not be available)
- ✅ **Swagger 2.0** - Backward compatible

**Supported formats:**
- YAML (`.yaml`, `.yml`)
- JSON (`.json`)

The test file `test-openapi.yaml` uses OpenAPI 3.0.3 specification.

## Architecture Notes

This is a converter tool that supports:
1. **OpenAPI/Swagger → Avro Schema**: Direct conversion with explicit types and enums
2. **JSON Data → Avro Schema**: Type inference from JSON data
3. **Avro Schema → Java Classes**: Automatic generation via Maven plugin during build

### Conversion Modes (OpenAPI only)

- **Standard Mode** (default): Generates one file per schema, types may be duplicated
- **Unified Mode** (`--unified`): Generates a single file with all types defined once and referenced by name
  - Recommended for most use cases
  - Avoids duplication
  - Format: JSON array `[enum1, enum2, record1, record2, ...]`
  - References types by name: `"com.shanks.generated.CardTypeEnum"`

### Java Code Generation (Avro → Java) with Maven Plugin

This project uses **avro-maven-plugin** for automatic Java class generation from Avro schemas during the Maven build process.

**How it works:**
- Avro schemas (.avsc files) are placed in `src/main/avro/`
- The Maven plugin automatically generates Java classes during the `generate-sources` phase
- Generated classes are placed in `target/generated-sources/avro/`
- Classes are automatically compiled and available for use in your code

**Directory Structure:**
```
src/
├── main/
│   ├── avro/              ← Place your .avsc schemas here (versioned)
│   │   ├── User.avsc
│   │   ├── Product.avsc
│   │   └── Order.avsc
│   └── java/
│       └── com/shanks/    ← Your application code
└── test/
    └── java/

target/
└── generated-sources/
    └── avro/              ← Generated Java classes (not versioned)
        └── com/shanks/model/
            ├── User.java
            ├── Product.java
            ├── ProductCategory.java  (enum)
            ├── Order.java
            ├── OrderItem.java
            ├── OrderStatus.java      (enum)
            └── Address.java
```

**Maven Plugin Configuration (in pom.xml):**
```xml
<plugin>
    <groupId>org.apache.avro</groupId>
    <artifactId>avro-maven-plugin</artifactId>
    <version>1.11.3</version>
    <configuration>
        <sourceDirectory>${project.basedir}/src/main/avro/</sourceDirectory>
        <outputDirectory>${project.build.directory}/generated-sources/avro/</outputDirectory>
        <stringType>String</stringType>
        <fieldVisibility>PRIVATE</fieldVisibility>
        <createSetters>true</createSetters>
        <enableDecimalLogicalType>true</enableDecimalLogicalType>
    </configuration>
</plugin>
```

**Features:**
- ✅ **Automatic generation**: Classes generated automatically during `mvn compile`
- ✅ **Convention over configuration**: Standard Maven directory structure
- ✅ **IDE integration**: IntelliJ/Eclipse recognize generated sources automatically
- ✅ **Batch processing**: All schemas in `src/main/avro/` are processed
- ✅ **Java-friendly**: Generates String (not CharSequence), private fields, getters/setters
- ✅ **Type support**: Records, enums, arrays, maps, unions, logical types (UUID, timestamp, decimal)

**Usage:**
```bash
# Generate classes (happens automatically during compile)
mvn compile

# Just generate sources without compiling
mvn generate-sources

# Clean and regenerate everything
mvn clean compile

# Full build with tests
mvn clean install
```

**Generated Code Characteristics:**
- Field visibility: `PRIVATE` (with getters/setters)
- String type: `java.lang.String` (not CharSequence)
- Setters: Enabled
- Builder pattern: Automatically generated for all records
- Logical types: UUID → java.util.UUID, timestamp-millis → java.time.Instant
- Namespace: Package structure matches Avro namespace (`com.shanks.model` → `com/shanks/model/`)

**Workflow:**
1. Create/update `.avsc` schemas in `src/main/avro/`
2. Run `mvn compile` (or just open project in IDE)
3. Generated Java classes appear in `target/generated-sources/avro/`
4. Use the generated classes in your application code
5. Classes regenerate automatically on next build if schemas change

**Example Schema (`src/main/avro/User.avsc`):**
```json
{
  "type": "record",
  "name": "User",
  "namespace": "com.shanks.model",
  "fields": [
    {"name": "userId", "type": {"type": "string", "logicalType": "uuid"}},
    {"name": "username", "type": "string"},
    {"name": "email", "type": "string"},
    {"name": "age", "type": ["null", "int"], "default": null},
    {"name": "createdAt", "type": {"type": "long", "logicalType": "timestamp-millis"}}
  ]
}
```

**Benefits of Maven Plugin Approach:**
- No manual CLI commands needed
- Schemas versioned in Git (`src/main/avro/`)
- Generated code never committed (in `target/`)
- Always up-to-date classes after build
- Works seamlessly in CI/CD pipelines
- IDE auto-completion for generated classes

### Pattern Support

The converter automatically extracts and preserves `pattern` constraints from OpenAPI/Swagger string fields:
- When a string field has a `pattern` attribute in the OpenAPI spec, it's included in the Avro schema
- Patterns are preserved in both standard and unified modes
- Example OpenAPI field:
  ```yaml
  phoneNumber:
    type: string
    pattern: '^\+?[1-9]\d{1,14}$'
  ```
  Becomes Avro field:
  ```json
  {
    "name": "phoneNumber",
    "type": {"type": "string", "pattern": "^\\+?[1-9]\\d{1,14}$"}
  }
  ```
- Patterns work alongside logical types (e.g., UUID with pattern validation)

The project follows SOLID principles with separate packages for:
- `parser/`: OpenAPI/Swagger parsing
- `mapper/`: Type mapping (OpenAPI → Avro)
- `converter/`: Conversion orchestration
  - `JsonToAvroConverter`: JSON → Avro schema conversion
  - `OpenApiToAvroConverter`: OpenAPI → Avro schema conversion
  - `SchemaGenerator`: Standard mode (inline types)
  - `UnifiedSchemaGenerator`: Unified mode (type references)
- `cli/`: Command-line interface
  - `ConverterCli`: Main CLI orchestrator
  - `CliArguments`: Arguments parser for schema generation
- `util/`: Type detectors (UUID, ENUM)
- `model/`: Generated Java classes from Avro schemas (via Maven plugin)

**Important**: All tests use JUnit 5 annotations (`@Test`, `@BeforeEach`, etc.) and AssertJ for assertions.
