# Graph Report - demo  (2026-11-20)

## Corpus Check
- 47 files · ~31,791 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 557 nodes · 1347 edges · 24 communities (19 shown, 5 thin omitted)
- Extraction: 82% EXTRACTED · 18% INFERRED · 0% AMBIGUOUS · INFERRED: 245 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `d0a71978`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- CLI Entry Point & Arguments
- Registry Schema Generation
- TypeInferenceEngine
- OpenAPI Type Mapping
- Ref Chaining Test Fixtures
- Project Documentation Concepts
- Avro Sample JSON Generation
- Doc Mode & Schema Doc Fields
- Enum Detection
- UUID Detection
- OpenAPI Parsing
- Avro Binary Encoding
- General OpenAPI Test Fixtures
- Notif Envelope Generation
- Deep Nesting Test Fixtures
- App Smoke Test
- Mapper & Parser Docs
- CLI Docs
- JSON Converter & Util Docs
- Maven Project Root
- AvroTypeInfo
- User (combines strings, booleans, numbers, enums, nested objects, arrays)
- RequestBody Conversion Test API (test-openapi-requestbody.yaml) — validates OpenAPI requestBody -> Avro conversion
- AvroSchemaBuilderTest

## God Nodes (most connected - your core abstractions)
1. `AvroTypeInfo` - 64 edges
2. `OpenApiToAvroTypeMapper` - 46 edges
3. `OpenApiToAvroConverter` - 34 edges
4. `ConverterCli` - 30 edges
5. `SchemaGenerator` - 23 edges
6. `JSON/OpenAPI → Avro Converter Architecture Diagram` - 19 edges
7. `TypeInferenceEngine` - 18 edges
8. `Builder` - 18 edges
9. `OpenApiParser` - 18 edges
10. `RegistrySchemaGenerator` - 16 edges

## Surprising Connections (you probably didn't know these)
- `SchemaFileWriter` --shares_data_with--> `.avsc output schema file`  [EXTRACTED]
  src/main/java/com/shanks/converter/SchemaFileWriter.java → docs/diagrams/converter-architecture.drawio.png
- `Address (street, city, zipCode pattern)` --semantically_similar_to--> `Address (shared ref: used by Customer, Payment, PurchaseOrder x2, Supplier, Warehouse; references GeoPoint)`  [INFERRED] [semantically similar]
  test-openapi-requestbody.yaml → test-openapi-refs.yaml
- `JSON/OpenAPI → Avro Converter Architecture Diagram` --references--> `JsonToAvroConverter`  [EXTRACTED]
  docs/diagrams/converter-architecture.drawio.png → src/main/java/com/shanks/converter/JsonToAvroConverter.java
- `JSON/OpenAPI → Avro Converter Architecture Diagram` --references--> `OpenApiToAvroConverter`  [EXTRACTED]
  docs/diagrams/converter-architecture.drawio.png → src/main/java/com/shanks/converter/OpenApiToAvroConverter.java
- `JSON/OpenAPI → Avro Converter Architecture Diagram` --references--> `RegistrySchemaGenerator`  [EXTRACTED]
  docs/diagrams/converter-architecture.drawio.png → src/main/java/com/shanks/converter/RegistrySchemaGenerator.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Main Flow: App -> ConverterCli -> (JsonToAvroConverter | OpenApiToAvroConverter | SchemaLoader) -> output files** — src_main_java_com_shanks_app_app, src_main_java_com_shanks_cli_convertercli_convertercli, src_main_java_com_shanks_converter_jsontoavroconverter_jsontoavroconverter, src_main_java_com_shanks_converter_openapitoavroconverter_openapitoavroconverter, src_main_java_com_shanks_serializer_schemaloader_schemaloader [EXTRACTED 1.00]
- **Converter package: schema generation pipeline components** — src_main_java_com_shanks_converter_jsontoavroconverter_jsontoavroconverter, src_main_java_com_shanks_converter_typeinferenceengine_typeinferenceengine, src_main_java_com_shanks_converter_openapitoavroconverter_openapitoavroconverter, src_main_java_com_shanks_converter_schemagenerator_schemagenerator, src_main_java_com_shanks_converter_registryschemagenerator_registryschemagenerator, src_main_java_com_shanks_converter_schemafilewriter_schemafilewriter [INFERRED 0.85]
- **Serializer package: schema load/generate/encode components** — src_main_java_com_shanks_serializer_schemaloader_schemaloader, src_main_java_com_shanks_serializer_avrojsongenerator_avrojsongenerator, src_main_java_com_shanks_serializer_avrobinaryencoder_avrobinaryencoder [INFERRED 0.85]
- **converter/ Package Components** — claude_jsontoavroconverter, claude_openapitoavroconverter, claude_schemagenerator, claude_registryschemagenerator, claude_schemafilewriter, claude_notifwrappergenerator [EXTRACTED 1.00]
- **Conversion Modes (OpenAPI only)** — claude_standard_mode, claude_registry_mode, claude_doc_mode, claude_functional_perimeter [EXTRACTED 1.00]
- **Schema Output Generation Flow** — claude_output_files, claude_notif_envelope, claude_schemafilewriter, claude_notifwrappergenerator [EXTRACTED 1.00]

## Communities (24 total, 5 thin omitted)

### Community 0 - "CLI Entry Point & Arguments"
Cohesion: 0.09
Nodes (14): Content, Operation, PathItem, OpenAPI, Pattern, Schema, MappedSchema, OpenApiToAvroConverter (+6 more)

### Community 1 - "Registry Schema Generation"
Cohesion: 0.23
Nodes (5): ObjectMapper, SchemaFileWriter, ObjectMapper, Test, SchemaFileWriterTest

### Community 2 - "TypeInferenceEngine"
Cohesion: 0.34
Nodes (4): AfterEach, ConverterCliMassConvertTest, BeforeEach, Test

### Community 3 - "OpenAPI Type Mapping"
Cohesion: 0.08
Nodes (14): AvroSchemaBuilder, Pattern, Schema, OpenAPI, Pattern, Schema, OpenApiToAvroTypeMapper, AvroTypeInfo (+6 more)

### Community 4 - "Ref Chaining Test Fixtures"
Cohesion: 0.07
Nodes (33): CardType (enum: DEBIT, CREDIT, PREPAID), Test API - Complete Type Coverage (test-openapi.yaml), ContactInfo (phoneNumber pattern, zipCode pattern, username pattern), Coordinates (latitude, longitude), CreditCard (number, cvv, expiry, type ref to CardType), Job (title, company, salary, startDate, isRemote), Location (street/city/state/country/zip + coordinates ref), Name (first, middle, last) (+25 more)

### Community 5 - "Project Documentation Concepts"
Cohesion: 0.08
Nodes (32): avro-maven-plugin, OpenAPI/JSON to Avro Converter Tool, Doc Mode (--doc), --from-request-body Flag, Functional Perimeter (--functional-perimeter), Graphify Knowledge Graph Integration, can't redefine Compile Error Behaviour, Hierarchical Namespace Strategy (+24 more)

### Community 6 - "Avro Sample JSON Generation"
Cohesion: 0.18
Nodes (7): AvroJsonGenerator, ObjectMapper, Schema, AvroJsonGeneratorTest, BeforeEach, ObjectMapper, Test

### Community 7 - "Doc Mode & Schema Doc Fields"
Cohesion: 0.09
Nodes (13): RegistrySchemaGenerator, Schema, SchemaGenerator, Test, RegistrySchemaGeneratorTest, BeforeEach, Test, SchemaGeneratorDocTest (+5 more)

### Community 8 - "Enum Detection"
Cohesion: 0.18
Nodes (8): EnumDetector, JsonNode, Override, Pattern, EnumDetectorTest, BeforeEach, ObjectMapper, Test

### Community 9 - "UUID Detection"
Cohesion: 0.19
Nodes (8): JsonNode, Override, Pattern, UuidDetector, BeforeEach, ObjectMapper, Test, UuidDetectorTest

### Community 10 - "OpenAPI Parsing"
Cohesion: 0.18
Nodes (7): OpenAPIV3Parser, ParseOptions, OpenAPI, OpenApiParser, BeforeEach, Test, OpenApiParserTest

### Community 11 - "Avro Binary Encoding"
Cohesion: 0.15
Nodes (9): GenericRecord, AvroSchemaValidationException, AvroBinaryEncoder, Schema, Schema, SchemaLoader, AvroBinaryEncoderTest, BeforeEach (+1 more)

### Community 12 - "General OpenAPI Test Fixtures"
Cohesion: 0.09
Nodes (10): .avsc output schema file, JSON/OpenAPI → Avro Converter Architecture Diagram, App, CliArguments, Override, ConverterCli, EofSignal, QuitSignal (+2 more)

### Community 13 - "Notif Envelope Generation"
Cohesion: 0.23
Nodes (7): JsonNode, ObjectMapper, NotifWrapperGenerator, JsonNode, ObjectMapper, Test, NotifWrapperGeneratorTest

### Community 14 - "Deep Nesting Test Fixtures"
Cohesion: 0.29
Nodes (8): Catalog (deep nesting via supplier/products, reuses Address/ContactInfo/Metadata names), Deep Nesting Test API (test-openapi-deep.yaml), Order (6-level deep nesting; inline Address/ContactInfo/Metadata repeated at customer, payment.billingInfo, shipment, shipment.events.location.facility), OrderStatus (enum), PaymentMethod (enum), ProductCategory (enum), ReviewRating (enum), ShipmentStatus (enum)

### Community 16 - "Mapper & Parser Docs"
Cohesion: 0.67
Nodes (3): mapper/ package (Type Mapping), parser/ package (OpenAPI/Swagger parsing), OpenApiToAvroConverter

### Community 20 - "AvroTypeInfo"
Cohesion: 0.13
Nodes (8): JsonNode, TypeDetector, ObjectMapper, Schema, JsonToAvroConverter, JsonNode, Type, TypeInferenceEngine

### Community 21 - "User (combines strings, booleans, numbers, enums, nested objects, arrays)"
Cohesion: 0.15
Nodes (12): JsonType, ARRAY, BOOLEAN, DOUBLE, ENUM, FLOAT, INTEGER, LONG (+4 more)

### Community 22 - "RequestBody Conversion Test API (test-openapi-requestbody.yaml) — validates OpenAPI requestBody -> Avro conversion"
Cohesion: 0.22
Nodes (13): Address (street, city, zipCode pattern), POST /orders/{orderId}/cancel — no operationId, tests path/method fallback naming (OrdersOrderIdCancelPost), POST /users createUser — requestBody=CreateUserRequest, 201 response=User, CreateUserRequest (references Profile, UserRole), POST /tasks/{taskId}/status getTaskStatus — inline requestBody with nested $ref field (response: CreateUserRequest), GET /users listUsers — 200 response=array of User, Order (billingAddress/shippingAddress both reference Address), Profile (references Address) (+5 more)

## Ambiguous Edges - Review These
- `RegistrySchemaGenerator` → `OpenApiToAvroTypeMapper`  [AMBIGUOUS]
  docs/diagrams/converter-architecture.drawio.png · relation: calls
- `SchemaGenerator` → `OpenApiToAvroTypeMapper`  [AMBIGUOUS]
  docs/diagrams/converter-architecture.drawio.png · relation: calls
- `OpenAPI/JSON to Avro Converter Tool` → `serializer/ package (JSON example + binary encoding)`  [AMBIGUOUS]
  README.md · relation: conceptually_related_to

## Knowledge Gaps
- **46 isolated node(s):** `com.shanks:demo`, `NULL`, `BOOLEAN`, `INTEGER`, `LONG` (+41 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **5 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `RegistrySchemaGenerator` and `OpenApiToAvroTypeMapper`?**
  _Edge tagged AMBIGUOUS (relation: calls) - confidence is low._
- **What is the exact relationship between `SchemaGenerator` and `OpenApiToAvroTypeMapper`?**
  _Edge tagged AMBIGUOUS (relation: calls) - confidence is low._
- **What is the exact relationship between `OpenAPI/JSON to Avro Converter Tool` and `serializer/ package (JSON example + binary encoding)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `JSON/OpenAPI → Avro Converter Architecture Diagram` connect `General OpenAPI Test Fixtures` to `CLI Entry Point & Arguments`, `Registry Schema Generation`, `OpenAPI Type Mapping`, `Avro Sample JSON Generation`, `Doc Mode & Schema Doc Fields`, `Enum Detection`, `UUID Detection`, `OpenAPI Parsing`, `Avro Binary Encoding`, `AvroTypeInfo`, `User (combines strings, booleans, numbers, enums, nested objects, arrays)`?**
  _High betweenness centrality (0.256) - this node is a cross-community bridge._
- **Why does `AvroTypeInfo` connect `OpenAPI Type Mapping` to `CLI Entry Point & Arguments`, `Doc Mode & Schema Doc Fields`, `OpenAPI Parsing`, `General OpenAPI Test Fixtures`, `AvroTypeInfo`?**
  _High betweenness centrality (0.152) - this node is a cross-community bridge._
- **Why does `OpenApiToAvroTypeMapper` connect `OpenAPI Type Mapping` to `CLI Entry Point & Arguments`, `UUID Detection`, `General OpenAPI Test Fixtures`, `Doc Mode & Schema Doc Fields`?**
  _High betweenness centrality (0.102) - this node is a cross-community bridge._
- **Are the 4 inferred relationships involving `SchemaGenerator` (e.g. with `.invalidEnum_stillFails_whenOnlyDescriptionAndReplacedByMarkIt()` and `.originalSpec_stillFails_whenInvalidEnumIsNotDeprecated()`) actually correct?**
  _`SchemaGenerator` has 4 INFERRED edges - model-reasoned connections that need verification._