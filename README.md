# OpenAPI/JSON to Avro Converter + Java Code Generator

Outil Java complet pour Apache Avro :
- 🔄 **OpenAPI/Swagger → Avro** : Conversion de specs API en schémas Avro
- 🔄 **JSON → Avro** : Inférence de schémas depuis données JSON
- ⚡ **Avro → Java** : Génération automatique de classes Java (Maven plugin)

## 🚀 Quick Start

### Prérequis
- Java 21+
- Maven 3.6+

### Installation

```bash
# Build
mvn clean package

# Génère le Fat JAR: target/json-to-avro-converter.jar
```

### Utilisation

```bash
# JSON → Avro
java -jar target/json-to-avro-converter.jar data.json schema.avsc

# OpenAPI → Avro (mode unifié recommandé)
java -jar target/json-to-avro-converter.jar api.yaml output.avsc User --unified

# Avro → Java (automatique)
mvn compile  # Les classes sont générées dans target/generated-sources/avro/
```

## 📖 Documentation Détaillée

### OpenAPI/Swagger → Avro

**Fonctionnalités clés:**
- Support OpenAPI 3.0.x, 3.1.x, Swagger 2.0 (YAML/JSON)
- Conversion directe des types et enums
- Extraction automatique des patterns regex
- Résolution des `$ref`
- **Mode unifié** (`--unified`) : Un seul fichier avec références au lieu de duplication

**Exemples:**

```bash
# Mode standard (fichiers séparés)
java -jar target/json-to-avro-converter.jar api.yaml output-dir/

# Mode unifié (recommandé - évite duplication)
java -jar target/json-to-avro-converter.jar api.yaml User.avsc User --unified
```

**Mapping des types:**
- string → STRING (+ logical types: uuid, timestamp)
- integer → INT/LONG
- number → FLOAT/DOUBLE
- boolean → BOOLEAN
- object → RECORD
- array → ARRAY
- enum → ENUM

### JSON → Avro

**Détection automatique:**
- Types primitifs (string, boolean, number)
- **UUID** : Détection regex → `logicalType: uuid`
- **ENUM** : Patterns UPPER_CASE → enum Avro
- Arrays, Records imbriqués
- Champs null → union `["null", "type"]`

**Exemple:**

```bash
java -jar target/json-to-avro-converter.jar data.json schema.avsc
```

### Avro → Java (Maven Plugin)

**Structure:**
```
src/main/avro/         ← Vos schémas .avsc (versionnés)
  ├── User.avsc
  └── Order.avsc

target/generated-sources/avro/  ← Classes générées (automatique)
  └── com/shanks/model/
      ├── User.java
      └── Order.java
```

**Workflow:**

1. Créer un schéma dans `src/main/avro/User.avsc`:
```json
{
  "type": "record",
  "name": "User",
  "namespace": "com.shanks.model",
  "fields": [
    {"name": "userId", "type": {"type": "string", "logicalType": "uuid"}},
    {"name": "username", "type": "string"},
    {"name": "email", "type": "string"},
    {"name": "age", "type": ["null", "int"], "default": null}
  ]
}
```

2. Compiler (génération automatique):
```bash
mvn compile
```

3. Utiliser dans votre code:
```java
import com.shanks.model.User;

User user = User.newBuilder()
    .setUserId(UUID.randomUUID())
    .setUsername("john.doe")
    .setEmail("john@example.com")
    .build();
```

**Configuration Maven:**
Le plugin est déjà configuré dans `pom.xml` avec:
- String type (pas CharSequence)
- Champs privés + getters/setters
- Builder pattern automatique
- Support logical types (UUID, timestamp, decimal)

**Plus d'infos:** Voir [src/main/avro/README.md](src/main/avro/README.md)

## 🔀 Deux Approches Disponibles

| Approche | Branch | Cas d'usage |
|----------|--------|-------------|
| **Maven Plugin** ⭐ | `main` | Schémas stables, build automatique, IDE integration |
| **CLI Runtime** | `feat/toJsonOrAvro` | Génération dynamique, workflow OpenAPI→Avro→Java unifié |

**Branch actuelle (`main`):** Utilise `avro-maven-plugin` pour génération automatique au build.

**Branch alternative (`feat/toJsonOrAvro`):** CLI avec `--generate-java` pour génération à la demande.

## 🏗️ Architecture

```
src/main/java/com/shanks/
├── cli/              # CLI et parsing arguments
├── converter/        # Convertisseurs (JSON, OpenAPI)
├── parser/           # Parser OpenAPI
├── mapper/           # Mapping types
├── model/            # Modèles de données
└── util/             # Détecteurs (UUID, ENUM)

src/main/avro/        # Schémas Avro versionnés
target/generated-sources/avro/  # Classes Java générées
```

**Principes:** SOLID, injection de dépendances, séparation des responsabilités.

## 🧪 Tests

```bash
mvn test  # 53 tests unitaires
```

## 🔧 Dépendances

- Apache Avro 1.11.3
- Jackson 2.16.1 (JSON)
- Swagger Parser 2.1.22 (OpenAPI)
- avro-maven-plugin 1.11.3

## 📝 Exemples Rapides

**Mode Unifié vs Standard (OpenAPI):**

```yaml
# api.yaml
components:
  schemas:
    CardType:
      type: string
      enum: [DEBIT, CREDIT]
    CreditCard:
      type: object
      properties:
        type:
          $ref: '#/components/schemas/CardType'
```

```bash
# Mode unifié (1 fichier, références)
java -jar target/json-to-avro-converter.jar api.yaml output.avsc CreditCard --unified
# → Enum défini une fois, référencé par "com.shanks.generated.CardTypeEnum"

# Mode standard (duplication possible)
java -jar target/json-to-avro-converter.jar api.yaml output.avsc CreditCard
# → Enum inline dans le record
```

**Détection automatique (JSON):**

```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "STATUS_ACTIVE",
  "tags": ["TAG_PREMIUM"]
}
```
→ Détecte automatiquement: UUID (logical type), ENUM (patterns UPPER_CASE)

## 🐛 Troubleshooting

**NoClassDefFoundError:** Utilisez le Fat JAR `json-to-avro-converter.jar`, pas `demo-1.0-SNAPSHOT.jar`

**Classes non générées:** Exécutez `mvn clean generate-sources`

**IDE ne voit pas les classes:** Recharger le projet Maven (IntelliJ: Maven → Reload)

## 📄 License

Usage éducatif et professionnel.

---

**Version:** 1.0-SNAPSHOT | **Java:** 21+ | **Maven:** 3.6+
