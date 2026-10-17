# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Clean and build
mvn clean package

# Run application with JSON input
mvn exec:java -Dexec.mainClass="com.shanks.App" -Dexec.args="data.json schema.avsc"

# Run application with JSON input (registry mode + custom functional-perimeter namespace)
mvn exec:java -Dexec.mainClass="com.shanks.App" -Dexec.args="data.json schema.avsc --registry --functional-perimeter orders"

# Run application with OpenAPI input (all record schemas - separate files; an output not ending in .avsc is a directory, trailing slash optional)
mvn exec:java -Dexec.mainClass="com.shanks.App" -Dexec.args="api.yaml output-dir"

# Run application with OpenAPI input (specific schema - standard mode)
mvn exec:java -Dexec.mainClass="com.shanks.App" -Dexec.args="api.yaml User.avsc User"

# Run application with OpenAPI input (registry mode - for IBM/Confluent Schema Registry)
mvn exec:java -Dexec.mainClass="com.shanks.App" -Dexec.args="api.yaml ResultResponse.avsc ResultResponse --registry"

# Run application with OpenAPI input (include doc fields from descriptions)
mvn exec:java -Dexec.mainClass="com.shanks.App" -Dexec.args="api.yaml User.avsc User --doc"

# Run application with OpenAPI input (registry mode + doc fields)
mvn exec:java -Dexec.mainClass="com.shanks.App" -Dexec.args="api.yaml ResultResponse.avsc ResultResponse --registry --doc"

# Run application with OpenAPI input (custom functional-perimeter namespace)
mvn exec:java -Dexec.mainClass="com.shanks.App" -Dexec.args="api.yaml CreateUser.avsc --functional-perimeter users --from-request-body /users POST"

# Run application with OpenAPI input (non-default notif envelope template)
mvn exec:java -Dexec.mainClass="com.shanks.App" -Dexec.args="api.yaml User.avsc User --envelope minimal"

# Run tests
mvn test

# Run specific test class
mvn test -Dtest=OpenApiParserTest

# Run JAR directly (after build)
java -jar target/json-to-avro-converter.jar data.json schema.avsc

# Run JAR with registry mode (IBM Schema Registry / Confluent Schema Registry)
java -jar target/json-to-avro-converter.jar api.yaml ResultResponse.avsc ResultResponse --registry

# Run JAR with doc fields (include OpenAPI descriptions as doc in Avro schema)
java -jar target/json-to-avro-converter.jar api.yaml User.avsc User --doc

# Run JAR with registry mode + doc fields
java -jar target/json-to-avro-converter.jar api.yaml ResultResponse.avsc ResultResponse --registry --doc

# Run JAR with a custom functional-perimeter namespace (com.shanks.generated.<name>)
java -jar target/json-to-avro-converter.jar api.yaml CreateUser.avsc --functional-perimeter users --from-request-body /users POST

# Run JAR with a non-default notif envelope template (src/main/resources/envelopes/<name>.json)
java -jar target/json-to-avro-converter.jar api.yaml User.avsc User --envelope minimal

# Run JAR showing the full Java stack trace on unexpected errors (hidden by default)
java -jar target/json-to-avro-converter.jar api.yaml User.avsc User --registry --stacktrace

# Run JAR in interactive directory (mass conversion) mode: for each spec file in specs/,
# prompts for a schema then per-file flags (doc?/functional-perimeter/envelope)
java -jar target/json-to-avro-converter.jar specs/ out/
printf "1\no\n\n\n" | java -jar target/json-to-avro-converter.jar specs/ out/

# Generate a sample JSON document from an Avro schema
java -jar target/json-to-avro-converter.jar generate src/main/avro/ResultResponse.avsc output.json ResultResponse

# Encode JSON to Avro binary (or --generate a sample first)
java -jar target/json-to-avro-converter.jar encode src/main/avro/ResultResponse.avsc data.json output.avro ResultResponse
java -jar target/json-to-avro-converter.jar encode src/main/avro/ResultResponse.avsc --generate output.avro ResultResponse

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

The test files use OpenAPI 3.0.3 specification:
- `test-openapi.yaml`: General conversion tests
- `test-openapi-deep.yaml`: Deep nesting (6 levels) with repeated type names (`Address`, `ContactInfo`, `Metadata`) at multiple hierarchy positions
- `test-openapi-refs.yaml`: Forward `$ref` references (schemas defined after their first use), same `$ref` used multiple times at the same level and across different hierarchy positions

## Architecture Notes

This is a converter tool that supports:
1. **OpenAPI/Swagger → Avro Schema**: Direct conversion with explicit types and enums
2. **JSON Data → Avro Schema**: Type inference from JSON data
3. **Avro Schema → Java Classes**: Automatic generation via Maven plugin during build

### Conversion Modes

All flags are parsed in one place, `CliArguments.parse`, in any order after `<input> <output>`; the first non-flag argument there is the schema name. An unknown `--option` is rejected. The flags that shape the generated schema (`--doc`, `--functional-perimeter`, `--envelope`) are carried by an immutable `ConversionOptions` record passed to every converter method — converters hold no per-call state.

`--registry`, `--functional-perimeter` and `--envelope` apply to both OpenAPI and JSON data input. `--doc` and `--from-request-body` are OpenAPI-only (a JSON data file has no descriptions or paths; `--doc` on JSON input is accepted and reported as ignored).

- **Schema output** (every mode): a single self-contained JSON object built by `SchemaGenerator` → `AvroSchemaBuilder` and serialized with `schema.toString(true)`
  - Single top-level `record` type (not a JSON array)
  - Nested types embedded inline at first occurrence, referenced by full qualified name on subsequent uses (deduplication of repeated `$ref` types is automatic)
  - This is the format IBM Schema Registry and Confluent Schema Registry expect
- **Registry Mode** (`--registry`): accepted for compatibility and echoed as `Mode: Registry`, but produces exactly the same output as without the flag — every schema is already registry-compatible (see above). There is no separate registry generator anymore.
- **Doc Mode** (`--doc`): Includes `doc` fields in the generated Avro schema, extracted from OpenAPI `description` fields
  - Can be combined with any other flag
  - Off by default: without this flag, no description `doc` fields are included (the API-version `doc` note is separate, see [Output Files](#output-files))
- **Functional Perimeter** (`--functional-perimeter <name>`): Appends `<name>` to the default namespace (`com.shanks.generated` → `com.shanks.generated.<name>`, computed by `ConversionOptions.namespace()`)
  - Can be combined with any other flag, in any argument position after `<output>`
  - Off by default: without this flag, the namespace stays `com.shanks.generated`
- **Envelope Selection** (`--envelope <name>`): Selects which notif envelope template builds the `.webhook.avsc` output (see [Output Files](#output-files))
  - Loads `src/main/resources/envelopes/<name>.json` from the classpath
  - Defaults to `default` (the original fixed structure) when omitted
  - Validated (`NotifWrapperGenerator.validate`) at the start of every conversion, before any file or directory is created
- **All schemas** (OpenAPI input, no schema name, no `--from-request-body`): writes one file set per record schema (`components/schemas` plus webhook `requestBody` payloads) into the output directory. The output argument is a directory unless it ends with `.avsc` (then its parent is used): `out`, `out/` and `out/User.avsc` all write into `out` (`CliArguments.getOutputDirectory`)
- **Request body** (`--from-request-body <path> <method>`): converts that operation's `requestBody` schema (`application/json`, else the first media type). The record name is the capitalized `operationId`, or, without one, the path segments plus the method (e.g. `POST /orders/{orderId}/cancel` → `OrdersOrderIdCancelPost`)
- **Interactive Directory Mode** (input path is a directory instead of a file): for every `*.yaml`/`*.yml`/`*.json` spec found directly in that directory (non-recursive), prompts on stdin for which schema to convert, then prompts for doc / functional perimeter / envelope for that one file
  - Only record schemas are offered (`components/schemas` keys plus webhook `requestBody` payload names — no response schemas, no bare enums/scalars, which can't be converted on their own); a spec with no record schema is reported as failed
  - CLI-level flags are ignored in this mode — everything is re-prompted per file, with plain defaults (doc off, no perimeter, `default` envelope) accepted by pressing Enter. There is no registry prompt (see Registry Mode above)
  - Each converted file prints `Generated: <path>`; `s`/`skip` skips a file, `q`/`quit` (or EOF on stdin) aborts the rest of the batch; a file that fails to parse or convert is journaled and does not stop the batch
  - Exit code `1` if at least one file failed, `0` otherwise; requires an interactive stdin, not meant for non-interactive CI pipelines
  - The output directory follows the same rule as "All schemas" (`out` or `out/`)
  - Implemented by `OpenApiToAvroConverter.loadSchemas`/`convertNamed` (converter side) and `ConverterCli.runMassConvert` (CLI prompt loop)
- **`generate` / `encode` sub-commands** (`com.shanks.serializer`): `generate <schema.avsc> <output.json> [SchemaName]` writes a sample JSON document for a schema (`AvroJsonGenerator`); `encode <schema.avsc> <input.json|--generate> <output.avro> [SchemaName]` encodes JSON to Avro binary (`AvroBinaryEncoder`), optionally from a generated sample. `SchemaLoader` loads the schema, picking `SchemaName` from a multi-record file

### Output Files

Every schema conversion (`SchemaFileWriter.write`) always produces three files from a single `<name>.avsc` output path, regardless of mode:

| File | Purpose |
|---|---|
| `<name>.avsc` | Pretty-printed Avro schema (the raw converted schema) |
| `<name>.min.avsc` | Minified, single-line copy of the same schema |
| `<name>.webhook.avsc` | The schema consolidated into a `notif` envelope template (see below) |

**API version stamping (OpenAPI input only):** when the spec declares `info.version`, the three file names get a `.v<version>` suffix (`User.avsc` → `User.v1.0.0.avsc`, `User.v1.0.0.min.avsc`, `User.v1.0.0.webhook.avsc`) and the root schema gets `"doc": "API version: <version>"` (appended to an existing doc with ` | `). This happens with or without `--doc`. JSON data input has no version, so its file names are used as given.

**`notif` envelope (`NotifWrapperGenerator`) — envelope-agnostic:** the envelope structure is not hardcoded in Java. It's loaded from a JSON template at `src/main/resources/envelopes/<envelopeName>.json` (`envelopeName` selected via `--envelope <name>`, default `"default"`). `NotifWrapperGenerator` treats the template as opaque data: it recursively searches the tree for an Avro field definition named `payload` (an object with `"name": "payload"` and a sibling `"type"` key) and overwrites its `type` with the generated schema, regardless of how deeply that field is nested or what else surrounds it. Adding a new envelope version is just adding a new template file — no code changes required.

The bundled `default.json` template reproduces the original structure:

```
Notif (root record)
├── header : Header (record)
│   ├── technical : Technical (record, currently empty — fields TBD)
│   └── functional : Functional (record, currently empty — fields TBD)
└── payload : <the generated schema, injected here>
```

The bundled `minimal.json` template demonstrates a different shape — `payload` sits directly at the root, with no `header`:

```
Notif (root record)
└── payload : <the generated schema, injected here>
```

- Templates use the literal token `${namespace}` anywhere a namespace should follow the generated schema's own root `namespace` (so it follows `--functional-perimeter` automatically); the token is substituted by simple string replacement before the template is parsed as JSON.
- The `.webhook.avsc` file is always generated — there is no flag to opt out of it, only of which envelope template shapes it.
- Works on the already-generated schema JSON string (parsed and re-embedded via Jackson), not the Avro `Schema` object.
- An unknown `--envelope <name>` (no matching template on the classpath) or a template with no `payload` field fails the conversion with a clear error before any files are written.

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
- ✅ **Type support**: Records, enums, arrays, maps, unions, logical types (UUID, decimal)
- ✅ **Conflict-free**: Hierarchical namespaces prevent `can't redefine` errors for repeated type names

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
- Logical types: UUID → java.util.UUID
- Namespace: Package structure matches Avro namespace (`com.shanks.model` → `com/shanks/model/`)
- Hierarchical namespaces: nested records/enums get unique namespaces based on their position in the schema tree (conflict-free compilation)

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
    {"name": "age", "type": ["null", "string"], "default": null},
    {"name": "createdAt", "type": ["null", "string"], "default": null}
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

### Type Mapping (OpenAPI → Avro)

| OpenAPI type | Format | Avro type |
|---|---|---|
| `string` | (none) | `string` |
| `string` | `uuid` | `string` + `logicalType: uuid` |
| `string` | `date` | `string` |
| `string` | `date-time` | `string` |
| `string` | `email` | `string` |
| `integer` | any (`int32`, `int64`) | `string` |
| `number` | any (`float`, `double`) | `string` |
| `boolean` | — | `boolean` |
| `object` | — | `record` |
| `array` | — | `array` |
| enum values | — | `enum` |

> All numeric and date/time types are intentionally mapped to `string` to avoid precision loss and simplify cross-system compatibility.

> A property that is not listed in its object's `required` array becomes a nullable union `["null", <type>]` with `"default": null`. In JSON data input, a `null` value becomes `["null", "string"]` with `"default": null`.

### Namespace Strategy

Each named type (record or enum) receives a hierarchical namespace encoding its full ancestry path in the schema tree. This guarantees unique full names even when the same type name appears at multiple positions.

**Rule:** `childNamespace = parentNamespace + "." + parentRecordName.toLowerCase()`

**Example:**
```
Root schema Order       → namespace: com.shanks.generated
  field customer        → com.shanks.generated.order.Customer
    field address       → com.shanks.generated.order.customer.Address
  field payment         → com.shanks.generated.order.Payment
    field billingInfo   → com.shanks.generated.order.payment.BillingInfo
      field address     → com.shanks.generated.order.payment.billinginfo.Address  ← different!
```

Both `Address` records exist at different hierarchy positions and have different full names — no `can't redefine` error on `mvn compile`.

**Custom root namespace (`--functional-perimeter <name>`):** the root namespace (`com.shanks.generated` above) becomes `com.shanks.generated.<name>`, and every hierarchical child namespace is derived from that new root — e.g. with `--functional-perimeter orders`, `com.shanks.generated.order.Customer` becomes `com.shanks.generated.orders.order.Customer`.

**Applies to:** records and enums, for every input and flag combination. Enums always emit an explicit `"namespace"` field to prevent Avro namespace inheritance from a sibling record.

### $ref Handling

- **Forward refs**: SwaggerParser resolves the entire OpenAPI file before conversion — `$ref` to schemas defined later in the file are transparent.
- **Same `$ref` at the same level** (e.g., `shippingAddress` and `billingAddress` both referencing `Address`): both modes build real `Schema` objects via `AvroSchemaBuilder`, then serialize with `schema.toString(true)` — Avro's own `Names` context deduplicates by full name automatically (first occurrence inline, second occurrence emits `"com.shanks.generated.Address"`). No dedup tracking is hand-rolled.
- **Same `$ref` at different levels**: each occurrence gets a different hierarchical namespace, so each is a distinct named type with no conflict

### Avro Name Validation

`AvroSchemaBuilder` (`src/main/java/com/shanks/converter/AvroSchemaBuilder.java`) is the single place that turns mapped/inferred type information into real `org.apache.avro.Schema` objects, for every conversion — `SchemaGenerator` is a thin wrapper around it that applies `ConversionOptions` and serializes with `schema.toString(true)`. Because there is exactly one construction path, Avro's naming rule (`[A-Za-z_][A-Za-z0-9_]*`, i.e. a value must start with a letter or underscore) is enforced identically everywhere for:
- record names
- field names
- enum names
- enum symbols

Before calling `Schema.createRecord` / `Schema.createEnum`, `AvroSchemaBuilder` pre-validates each of the above and throws `AvroSchemaValidationException` with a human-readable message (offending value, its full-name context, the rule, and a fix hint) instead of letting a raw `SchemaParseException` surface. `Schema.createRecord`/`Schema.createEnum` are still called and wrapped in a try/catch as a safety net for any Avro rule not pre-validated above.

**CLI error output** (`ConverterCli.run`): `AvroSchemaValidationException` and `InvalidEnvelopeException` (unknown, unreadable or payload-less envelope template) are caught separately and printed as `Error: <message>` with no stack trace hint (the message is already actionable; `--stacktrace` still prints the trace). Invalid arguments (`IllegalArgumentException`) print the message followed by the usage. Any other unexpected exception prints `Error during operation: <message>` and hides the stack trace by default — pass `--stacktrace` to print it in full. In interactive directory mode, per-file failures are journaled with their message only.

### Known Namespace Behaviour

When compiling multiple `.avsc` files with `mvn compile`, Avro's Maven plugin processes them independently. If two files define a type with the same full name (e.g., both define `com.shanks.generated.Address`), the second file will fail with `can't redefine`. The hierarchical namespace strategy prevents this by ensuring all generated types have unique full names.

### Pattern Support

The converter automatically extracts and preserves `pattern` constraints from OpenAPI/Swagger string fields:
- When a string field has a `pattern` attribute in the OpenAPI spec, it's included in the Avro schema
- Patterns are preserved for every conversion
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
  - `AvroSchemaBuilder`: shared `AvroTypeInfo` → `org.apache.avro.Schema` construction, used by both modes below (see [Avro Name Validation](#avro-name-validation))
  - `SchemaGenerator`: the single generator (self-contained, registry-compatible schema) — thin wrapper over `AvroSchemaBuilder`
  - `ConversionOptions`: immutable record of the schema-shaping flags (doc, functional perimeter → namespace, envelope) and the `DEFAULT_NAMESPACE`/`DEFAULT_ENVELOPE` constants
  - `SchemaFileWriter` / `NotifWrapperGenerator`: write the three output files / build the envelope-wrapped one
  - `InvalidEnvelopeException`: thrown for a missing, malformed or payload-less envelope template, shown as-is by the CLI
  - `AvroSchemaValidationException`: thrown for invalid Avro names/symbols, carries a human-readable message shown as-is by the CLI
- `cli/`: Command-line interface
  - `ConverterCli`: Main CLI orchestrator
  - `CliArguments`: parses every flag of the convert command, the schema name, and the output directory rule
- `serializer/`: `generate`/`encode` sub-commands (`SchemaLoader`, `AvroJsonGenerator`, `AvroBinaryEncoder`)
- `util/`: Type detectors (UUID, ENUM)
- `model/`: Generated Java classes from Avro schemas (via Maven plugin)

**Important**: All tests use JUnit 5 annotations (`@Test`, `@BeforeEach`, etc.) and AssertJ for assertions.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
