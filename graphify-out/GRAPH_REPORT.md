# Graph Report - .  (2026-07-25)

## Corpus Check
- Corpus is ~23,646 words - fits in a single context window. You may not need a graph.

## Summary
- 435 nodes · 1008 edges · 20 communities (16 shown, 4 thin omitted)
- Extraction: 81% EXTRACTED · 19% INFERRED · 0% AMBIGUOUS · INFERRED: 187 edges (avg confidence: 0.81)
- Token cost: 0 input · 68,889 output

## Community Hubs (Navigation)
- CLI Entry Point & Arguments
- Registry Schema Generation
- JSON-to-Avro Conversion Core
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

## God Nodes (most connected - your core abstractions)
1. `AvroTypeInfo` - 63 edges
2. `OpenApiToAvroTypeMapper` - 32 edges
3. `SchemaGenerator` - 27 edges
4. `OpenApiToAvroConverter` - 24 edges
5. `JSON/OpenAPI → Avro Converter Architecture Diagram` - 19 edges
6. `ConverterCli` - 18 edges
7. `TypeInferenceEngine` - 18 edges
8. `Builder` - 18 edges
9. `RegistrySchemaGenerator` - 17 edges
10. `OpenApiParser` - 16 edges

## Surprising Connections (you probably didn't know these)
- `JSON/OpenAPI → Avro Converter Architecture Diagram` --references--> `JsonToAvroConverter`  [EXTRACTED]
  docs/diagrams/converter-architecture.drawio.png → src/main/java/com/shanks/converter/JsonToAvroConverter.java
- `JSON/OpenAPI → Avro Converter Architecture Diagram` --references--> `RegistrySchemaGenerator`  [EXTRACTED]
  docs/diagrams/converter-architecture.drawio.png → src/main/java/com/shanks/converter/RegistrySchemaGenerator.java
- `SchemaFileWriter` --shares_data_with--> `.avsc output schema file`  [EXTRACTED]
  src/main/java/com/shanks/converter/SchemaFileWriter.java → docs/diagrams/converter-architecture.drawio.png
- `JSON/OpenAPI → Avro Converter Architecture Diagram` --references--> `SchemaGenerator`  [EXTRACTED]
  docs/diagrams/converter-architecture.drawio.png → src/main/java/com/shanks/converter/SchemaGenerator.java
- `JSON/OpenAPI → Avro Converter Architecture Diagram` --references--> `TypeInferenceEngine`  [EXTRACTED]
  docs/diagrams/converter-architecture.drawio.png → src/main/java/com/shanks/converter/TypeInferenceEngine.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Main Flow: App -> ConverterCli -> (JsonToAvroConverter | OpenApiToAvroConverter | SchemaLoader) -> output files** — src_main_java_com_shanks_app_app, src_main_java_com_shanks_cli_convertercli_convertercli, src_main_java_com_shanks_converter_jsontoavroconverter_jsontoavroconverter, src_main_java_com_shanks_converter_openapitoavroconverter_openapitoavroconverter, src_main_java_com_shanks_serializer_schemaloader_schemaloader [EXTRACTED 1.00]
- **Converter package: schema generation pipeline components** — src_main_java_com_shanks_converter_jsontoavroconverter_jsontoavroconverter, src_main_java_com_shanks_converter_typeinferenceengine_typeinferenceengine, src_main_java_com_shanks_converter_openapitoavroconverter_openapitoavroconverter, src_main_java_com_shanks_converter_schemagenerator_schemagenerator, src_main_java_com_shanks_converter_registryschemagenerator_registryschemagenerator, src_main_java_com_shanks_converter_schemafilewriter_schemafilewriter [INFERRED 0.85]
- **Serializer package: schema load/generate/encode components** — src_main_java_com_shanks_serializer_schemaloader_schemaloader, src_main_java_com_shanks_serializer_avrojsongenerator_avrojsongenerator, src_main_java_com_shanks_serializer_avrobinaryencoder_avrobinaryencoder [INFERRED 0.85]
- **converter/ Package Components** — claude_jsontoavroconverter, claude_openapitoavroconverter, claude_schemagenerator, claude_registryschemagenerator, claude_schemafilewriter, claude_notifwrappergenerator [EXTRACTED 1.00]
- **Conversion Modes (OpenAPI only)** — claude_standard_mode, claude_registry_mode, claude_doc_mode, claude_functional_perimeter [EXTRACTED 1.00]
- **Schema Output Generation Flow** — claude_output_files, claude_notif_envelope, claude_schemafilewriter, claude_notifwrappergenerator [EXTRACTED 1.00]

## Communities (20 total, 4 thin omitted)

### Community 0 - "CLI Entry Point & Arguments"
Cohesion: 0.07
Nodes (14): .avsc output schema file, JSON/OpenAPI → Avro Converter Architecture Diagram, Operation, PathItem, App, CliArguments, Override, ConverterCli (+6 more)

### Community 1 - "Registry Schema Generation"
Cohesion: 0.09
Nodes (7): RegistrySchemaGenerator, Schema, SchemaGenerator, AvroTypeInfo, Builder, Override, Type

### Community 2 - "JSON-to-Avro Conversion Core"
Cohesion: 0.08
Nodes (20): JsonNode, TypeDetector, ObjectMapper, Schema, JsonToAvroConverter, JsonNode, Type, TypeInferenceEngine (+12 more)

### Community 3 - "OpenAPI Type Mapping"
Cohesion: 0.16
Nodes (8): ArraySchema, OpenAPI, Pattern, Schema, OpenApiToAvroTypeMapper, BeforeEach, Test, OpenApiToAvroTypeMapperTest

### Community 4 - "Ref Chaining Test Fixtures"
Cohesion: 0.08
Nodes (33): ContactInfo (phoneNumber pattern, zipCode pattern, username pattern), Address (shared ref: used by Customer, Payment, PurchaseOrder x2, Supplier, Warehouse; references GeoPoint), Catalog (references Product/Supplier), ContactInfo (phone pattern, email, website), Customer (references Address, LoyaltyTier), Dimensions (width/height/depth/weightKg/unit enum inline), Discount (references DiscountType), DiscountType (enum, defined last of all) (+25 more)

### Community 5 - "Project Documentation Concepts"
Cohesion: 0.08
Nodes (32): avro-maven-plugin, OpenAPI/JSON to Avro Converter Tool, Doc Mode (--doc), --from-request-body Flag, Functional Perimeter (--functional-perimeter), Graphify Knowledge Graph Integration, can't redefine Compile Error Behaviour, Hierarchical Namespace Strategy (+24 more)

### Community 6 - "Avro Sample JSON Generation"
Cohesion: 0.18
Nodes (7): AvroJsonGenerator, ObjectMapper, Schema, AvroJsonGeneratorTest, BeforeEach, ObjectMapper, Test

### Community 7 - "Doc Mode & Schema Doc Fields"
Cohesion: 0.17
Nodes (6): BeforeEach, Test, SchemaGeneratorDocTest, BeforeEach, Test, SchemaGeneratorPatternTest

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
Cohesion: 0.24
Nodes (6): GenericRecord, AvroBinaryEncoder, Schema, AvroBinaryEncoderTest, BeforeEach, Test

### Community 12 - "General OpenAPI Test Fixtures"
Cohesion: 0.17
Nodes (13): CardType (enum: DEBIT, CREDIT, PREPAID), Test API - Complete Type Coverage (test-openapi.yaml), Coordinates (latitude, longitude), CreditCard (number, cvv, expiry, type ref to CardType), Job (title, company, salary, startDate, isRemote), Location (street/city/state/country/zip + coordinates ref), Name (first, middle, last), ResultResponse (root response schema wrapping User array) (+5 more)

### Community 13 - "Notif Envelope Generation"
Cohesion: 0.44
Nodes (4): ObjectNode, JsonNode, ObjectMapper, NotifWrapperGenerator

### Community 14 - "Deep Nesting Test Fixtures"
Cohesion: 0.29
Nodes (8): Catalog (deep nesting via supplier/products, reuses Address/ContactInfo/Metadata names), Deep Nesting Test API (test-openapi-deep.yaml), Order (6-level deep nesting; inline Address/ContactInfo/Metadata repeated at customer, payment.billingInfo, shipment, shipment.events.location.facility), OrderStatus (enum), PaymentMethod (enum), ProductCategory (enum), ReviewRating (enum), ShipmentStatus (enum)

### Community 16 - "Mapper & Parser Docs"
Cohesion: 0.67
Nodes (3): mapper/ package (Type Mapping), parser/ package (OpenAPI/Swagger parsing), OpenApiToAvroConverter

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
- **4 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `RegistrySchemaGenerator` and `OpenApiToAvroTypeMapper`?**
  _Edge tagged AMBIGUOUS (relation: calls) - confidence is low._
- **What is the exact relationship between `SchemaGenerator` and `OpenApiToAvroTypeMapper`?**
  _Edge tagged AMBIGUOUS (relation: calls) - confidence is low._
- **What is the exact relationship between `OpenAPI/JSON to Avro Converter Tool` and `serializer/ package (JSON example + binary encoding)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `JSON/OpenAPI → Avro Converter Architecture Diagram` connect `CLI Entry Point & Arguments` to `Registry Schema Generation`, `JSON-to-Avro Conversion Core`, `OpenAPI Type Mapping`, `Avro Sample JSON Generation`, `Enum Detection`, `UUID Detection`, `OpenAPI Parsing`, `Avro Binary Encoding`?**
  _High betweenness centrality (0.280) - this node is a cross-community bridge._
- **Why does `AvroTypeInfo` connect `Registry Schema Generation` to `CLI Entry Point & Arguments`, `JSON-to-Avro Conversion Core`, `OpenAPI Type Mapping`, `Doc Mode & Schema Doc Fields`?**
  _High betweenness centrality (0.146) - this node is a cross-community bridge._
- **Why does `UuidDetector` connect `UUID Detection` to `CLI Entry Point & Arguments`, `Enum Detection`, `JSON-to-Avro Conversion Core`, `OpenAPI Type Mapping`?**
  _High betweenness centrality (0.090) - this node is a cross-community bridge._
- **What connects `com.shanks:demo`, `NULL`, `BOOLEAN` to the rest of the system?**
  _46 weakly-connected nodes found - possible documentation gaps or missing edges._