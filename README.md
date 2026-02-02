# OpenAPI/JSON to Avro Schema Converter + Java Code Generator

Un outil Java complet pour travailler avec Apache Avro :
- 🔄 **OpenAPI/Swagger → Avro** : Conversion de spécifications API en schémas Avro
- 🔄 **JSON Data → Avro** : Inférence intelligente de schémas à partir de données JSON
- ⚡ **Avro → Java** : Génération automatique de classes Java via Maven plugin

## 🎯 Fonctionnalités

### OpenAPI/Swagger → Avro
- ✅ **Conversion directe** : Pas d'inférence, utilise les types définis dans OpenAPI
- ✅ **Support YAML et JSON** : Détection automatique du format
- ✅ **Enums explicites** : Conversion directe des enums OpenAPI avec tous leurs symboles
- ✅ **Mode unifié** (⭐ Nouveau) : Génère un seul fichier avec toutes les définitions et des références
- ✅ **Formats OpenAPI** : uuid, date-time, etc.
- ✅ **Patterns de validation** (⭐ Nouveau) : Extraction automatique des patterns regex des champs string
- ✅ **Références** : Résolution automatique des `$ref`
- ✅ **Propriétés requises** : Les champs non-required deviennent nullable
- ✅ **Types OpenAPI → Avro** :
  - string → STRING (avec logical types si format: uuid, date-time et pattern si spécifié)
  - integer → INT ou LONG
  - number → FLOAT ou DOUBLE
  - boolean → BOOLEAN
  - object → RECORD
  - array → ARRAY
  - enum → ENUM

### JSON Data → Avro
- ✅ **Inférence automatique des types** : Détection intelligente des types primitifs et complexes
- ✅ **Types primitifs** : string, boolean
- ✅ **Types complexes** :
  - **UUID** : Détection par regex avec logical type
  - **ENUM** : Détection heuristique (UPPER_CASE avec/sans underscores)
  - **Arrays** : Analyse complète de tous les éléments
  - **Records imbriqués** : Support récursif
- ✅ **Gestion des nulls** : Génération automatique d'union types `["null", "type"]` avec `default: null`
- ✅ **Noms capitalisés** : Types complexes avec noms en PascalCase

### Avro → Java (Code Generation with Maven Plugin)
- ✅ **Génération automatique** : Classes Java générées pendant le build Maven
- ✅ **Convention over configuration** : Structure de répertoires standard Maven
- ✅ **Intégration IDE** : IntelliJ IDEA et Eclipse reconnaissent automatiquement les sources générées
- ✅ **Types Java-friendly** : String au lieu de CharSequence, champs privés, getters/setters
- ✅ **Support complet** : Records, enums, arrays, maps, unions, logical types (UUID, timestamp, decimal)
- ✅ **Builder pattern** : Classes builder générées automatiquement pour tous les records
- ✅ **Workflow simplifié** : Schémas versionnés dans `src/main/avro/`, classes en `target/`

### Général
- ✅ **Architecture SOLID** : Code maintenable et extensible
- ✅ **Fat JAR** : Exécutable autonome sans dépendances externes

## 📋 Prérequis

- **Java** 21 ou supérieur
- **Maven** 3.6+

### Versions OpenAPI/Swagger supportées

Le convertisseur utilise **Swagger Parser v3 (2.1.22)** qui supporte :

- ✅ **OpenAPI 3.0.x** (3.0.0, 3.0.1, 3.0.2, 3.0.3) - **Recommandé**
- ✅ **OpenAPI 3.1.x** - Support partiel (certaines nouvelles fonctionnalités peuvent ne pas être prises en charge)
- ✅ **Swagger 2.0** - Support rétrocompatible

**Format de fichier :**
- YAML (`.yaml`, `.yml`)
- JSON (`.json`)

**Note** : Le fichier de test inclus ([test-openapi.yaml](test-openapi.yaml)) utilise OpenAPI 3.0.3.

## 🚀 Installation & Build

### 1. Cloner le projet

```bash
cd /path/to/project
```

### 2. Compiler et créer le Fat JAR

```bash
mvn clean package
```

Cela génère :
- `target/demo-1.0-SNAPSHOT.jar` (26 KB) - JAR normal
- `target/json-to-avro-converter.jar` (3.9 MB) - **Fat JAR exécutable**

## 💻 Utilisation

Le CLI détecte automatiquement le type de fichier d'entrée (OpenAPI ou JSON) et utilise le convertisseur approprié.

### Méthode 1 : Fat JAR (Recommandé)

#### Convertir un fichier JSON de données
```bash
java -jar target/json-to-avro-converter.jar data.json schema.avsc
```

#### Convertir un fichier OpenAPI (tous les schémas séparés)
```bash
java -jar target/json-to-avro-converter.jar api.yaml output-dir/
```

#### Convertir un schéma OpenAPI spécifique (mode standard)
```bash
java -jar target/json-to-avro-converter.jar api.yaml User.avsc User
```

#### Convertir un schéma OpenAPI en mode unifié (⭐ Recommandé ⭐)
```bash
java -jar target/json-to-avro-converter.jar api.yaml ResultResponse.avsc ResultResponse --unified
```

Le mode `--unified` génère un **seul fichier** contenant :
- Toutes les définitions de types (enums, records)
- Des **références** au lieu de répétitions
- Format compatible avec les outils Avro

### Méthode 2 : Via Maven

#### Convertir un fichier JSON de données
```bash
mvn exec:java -Dexec.mainClass="com.shanks.App" -Dexec.args="data.json schema.avsc"
```

#### Convertir un fichier OpenAPI (mode standard)
```bash
mvn exec:java -Dexec.mainClass="com.shanks.App" -Dexec.args="api.yaml User.avsc User"
```

#### Convertir un fichier OpenAPI (mode unifié)
```bash
mvn exec:java -Dexec.mainClass="com.shanks.App" -Dexec.args="api.yaml ResultResponse.avsc ResultResponse --unified"
```

## 🔀 Quel Mode Choisir ?

### Mode Unifié (--unified) ⭐ Recommandé

**Quand l'utiliser :**
- Vous voulez un **seul fichier** Avro contenant tous les types
- Vous avez des **types partagés** (enums, records) utilisés à plusieurs endroits
- Vous voulez éviter la **duplication** de définitions
- Vous utilisez des **outils Avro** qui supportent les fichiers multi-schémas

**Avantages :**
- ✅ Pas de duplication de code
- ✅ Un seul fichier à gérer
- ✅ Types réutilisables
- ✅ Format Avro standard pour multi-types

**Commande :**
```bash
mvn exec:java -Dexec.mainClass="com.shanks.App" -Dexec.args="api.yaml ResultResponse.avsc ResultResponse --unified"
```

### Mode Standard (par défaut)

**Quand l'utiliser :**
- Vous voulez des **fichiers séparés** pour chaque type
- Vous avez besoin de **déployer les schémas individuellement**
- Vous utilisez un **registre de schémas** qui gère un schéma par fichier

**Commande :**
```bash
mvn exec:java -Dexec.mainClass="com.shanks.App" -Dexec.args="api.yaml output/"
```

---

## ⚡ Génération de Classes Java (Avro → Java)

Le projet utilise **avro-maven-plugin** pour générer automatiquement des classes Java à partir de schémas Avro pendant le build Maven.

### 📁 Structure du Projet

```
src/
├── main/
│   ├── avro/              ← Place vos schémas .avsc ici (versionnés)
│   │   ├── User.avsc
│   │   ├── Product.avsc
│   │   └── Order.avsc
│   └── java/
│       └── com/shanks/    ← Votre code applicatif
└── test/
    └── java/

target/
└── generated-sources/
    └── avro/              ← Classes Java générées (non versionnées)
        └── com/shanks/model/
            ├── User.java
            ├── Product.java
            ├── ProductCategory.java  (enum)
            ├── Order.java
            ├── OrderItem.java
            ├── OrderStatus.java      (enum)
            └── Address.java
```

### 🚀 Workflow

1. **Créer un schéma Avro** dans `src/main/avro/`

**Exemple (`src/main/avro/User.avsc`):**
```json
{
  "type": "record",
  "name": "User",
  "namespace": "com.shanks.model",
  "doc": "User record",
  "fields": [
    {
      "name": "userId",
      "type": {"type": "string", "logicalType": "uuid"},
      "doc": "Unique user identifier"
    },
    {
      "name": "username",
      "type": "string",
      "doc": "Username for login"
    },
    {
      "name": "email",
      "type": "string"
    },
    {
      "name": "age",
      "type": ["null", "int"],
      "default": null,
      "doc": "User age (optional)"
    },
    {
      "name": "createdAt",
      "type": {"type": "long", "logicalType": "timestamp-millis"}
    },
    {
      "name": "active",
      "type": "boolean",
      "default": true
    }
  ]
}
```

2. **Compiler le projet** (génération automatique)

```bash
# Génère automatiquement les classes Java
mvn compile

# Ou juste générer les sources sans compiler
mvn generate-sources

# Ou build complet
mvn clean install
```

3. **Utiliser les classes générées** dans votre code

```java
package com.shanks;

import com.shanks.model.User;
import java.util.UUID;
import java.time.Instant;

public class Main {
    public static void main(String[] args) {
        // Utiliser le builder généré automatiquement
        User user = User.newBuilder()
            .setUserId(UUID.randomUUID())
            .setUsername("john.doe")
            .setEmail("john@example.com")
            .setAge(30)
            .setCreatedAt(Instant.now())
            .setActive(true)
            .build();

        // Accéder aux champs via getters
        System.out.println("User: " + user.getUsername());
        System.out.println("Email: " + user.getEmail());
    }
}
```

### 🔧 Configuration Maven

Le plugin est déjà configuré dans `pom.xml`:

```xml
<plugin>
    <groupId>org.apache.avro</groupId>
    <artifactId>avro-maven-plugin</artifactId>
    <version>1.11.3</version>
    <executions>
        <execution>
            <phase>generate-sources</phase>
            <goals>
                <goal>schema</goal>
            </goals>
            <configuration>
                <sourceDirectory>${project.basedir}/src/main/avro/</sourceDirectory>
                <outputDirectory>${project.build.directory}/generated-sources/avro/</outputDirectory>
                <stringType>String</stringType>
                <fieldVisibility>PRIVATE</fieldVisibility>
                <createSetters>true</createSetters>
                <enableDecimalLogicalType>true</enableDecimalLogicalType>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 📝 Caractéristiques du Code Généré

| Aspect | Configuration |
|--------|---------------|
| **Visibilité des champs** | `PRIVATE` (avec getters/setters) |
| **Type String** | `java.lang.String` (pas CharSequence) |
| **Setters** | Générés automatiquement |
| **Builder pattern** | Généré pour tous les records |
| **Logical types** | UUID → `java.util.UUID`, timestamp → `java.time.Instant` |
| **Enums** | Classes enum Java séparées |
| **Namespace** | Devient le package Java |

### 💡 Avantages de cette Approche

| Avantage | Description |
|----------|-------------|
| ✅ **Zéro configuration** | Fonctionne immédiatement après `mvn compile` |
| ✅ **Toujours à jour** | Les classes se régénèrent si les schémas changent |
| ✅ **IDE friendly** | Auto-complétion et navigation dans IntelliJ/Eclipse |
| ✅ **CI/CD ready** | Génération automatique dans les pipelines |
| ✅ **Type safety** | Compilation Java vérifie les types |
| ✅ **No manual steps** | Pas de commande CLI à lancer manuellement |

### 🎯 Cas d'Usage

**✅ Utilisez la génération Maven SI:**
- Vous avez des schémas Avro **stables et versionnés**
- Vous voulez que les classes soient **toujours à jour**
- Vous travaillez sur un **projet Maven standard**
- Vous voulez l'**intégration IDE automatique**

**Workflow typique:**
```bash
# 1. Créer/modifier un schéma
vim src/main/avro/User.avsc

# 2. Compiler (génération auto)
mvn compile

# 3. Les classes sont prêtes!
# Utilisez-les directement dans votre code Java
```

### 📖 Documentation Complète

Pour plus de détails sur les schémas Avro et leur utilisation :
- Voir [src/main/avro/README.md](src/main/avro/README.md)
- [Apache Avro Documentation](https://avro.apache.org/docs/current/)
- [avro-maven-plugin Guide](https://avro.apache.org/docs/current/gettingstartedjava.html)

---

## 📚 Exemples

### Exemple 1 : Mode Unifié vs Mode Standard

**Input OpenAPI (api.yaml) :**
```yaml
openapi: 3.0.3
info:
  title: User API
  version: 1.0.0

components:
  schemas:
    CardType:
      type: string
      enum:
        - DEBIT
        - CREDIT
        - PREPAID

    CreditCard:
      type: object
      properties:
        number:
          type: string
        type:
          $ref: '#/components/schemas/CardType'
      required:
        - number
        - type
```

#### Mode Standard (types répétés)

**Commande :**
```bash
mvn exec:java -Dexec.mainClass="com.shanks.App" -Dexec.args="api.yaml CreditCard.avsc CreditCard"
```

**Output (CreditCard.avsc) :**
```json
{
  "type": "record",
  "name": "CreditCardRecord",
  "namespace": "com.shanks.generated",
  "fields": [
    {
      "name": "number",
      "type": "string"
    },
    {
      "name": "type",
      "type": {
        "type": "enum",
        "name": "CardTypeEnum",
        "namespace": "com.shanks.generated",
        "symbols": ["DEBIT", "CREDIT", "PREPAID"]
      }
    }
  ]
}
```

⚠️ **Problème** : L'enum est défini inline. Si utilisé plusieurs fois, il sera répété.

#### Mode Unifié (⭐ Recommandé - types référencés)

**Commande :**
```bash
mvn exec:java -Dexec.mainClass="com.shanks.App" -Dexec.args="api.yaml CreditCard.avsc CreditCard --unified"
```

**Output (CreditCard.avsc) :**
```json
[
  {
    "type": "enum",
    "name": "CardTypeEnum",
    "namespace": "com.shanks.generated",
    "symbols": ["DEBIT", "CREDIT", "PREPAID"]
  },
  {
    "type": "record",
    "name": "CreditCardRecord",
    "namespace": "com.shanks.generated",
    "fields": [
      {
        "name": "number",
        "type": "string"
      },
      {
        "name": "type",
        "type": "com.shanks.generated.CardTypeEnum"
      }
    ]
  }
]
```

✅ **Avantages** :
- Enum défini **une seule fois** en haut du fichier
- Référencé par son nom `"com.shanks.generated.CardTypeEnum"`
- Pas de duplication
- Format standard Avro pour les fichiers multi-types

### Exemple 2 : Patterns de Validation (OpenAPI)

**Input OpenAPI (api.yaml) :**
```yaml
openapi: 3.0.3
info:
  title: Contact API
  version: 1.0.0

components:
  schemas:
    ContactInfo:
      type: object
      properties:
        phoneNumber:
          type: string
          pattern: '^\+?[1-9]\d{1,14}$'
          description: Phone number in E.164 format
        zipCode:
          type: string
          pattern: '^\d{5}(-\d{4})?$'
          description: US ZIP code
        email:
          type: string
          format: email
      required:
        - phoneNumber
```

**Commande (mode unifié) :**
```bash
mvn exec:java -Dexec.mainClass="com.shanks.App" -Dexec.args="api.yaml ContactInfo.avsc ContactInfo --unified"
```

**Output (ContactInfo.avsc) :**
```json
[
  {
    "type": "record",
    "name": "ContactInfoRecord",
    "namespace": "com.shanks.generated",
    "fields": [
      {
        "name": "phoneNumber",
        "type": {"type": "string", "pattern": "^\\+?[1-9]\\d{1,14}$"}
      },
      {
        "name": "zipCode",
        "type": ["null", {"type": "string", "pattern": "^\\d{5}(-\\d{4})?$"}],
        "default": null
      },
      {
        "name": "email",
        "type": ["null", "string"],
        "default": null
      }
    ]
  }
]
```

✅ **Points clés** :
- Les patterns sont **automatiquement extraits** de l'OpenAPI
- Les backslashes sont **correctement échappés** dans le JSON (`\d` → `\\d`)
- Compatible avec les **champs nullable** (union types)
- Fonctionne en **mode standard et unifié**

### Exemple 3 : Types Primitifs et Complexes (JSON Data)

**Input JSON (data.json) :**
```json
{
  "id": "12345",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "STATUS_ACTIVE",
  "balance": "1234.56",
  "tags": ["TAG_PREMIUM", "TAG_VERIFIED"],
  "metadata": {
    "created": "2024-01-01",
    "version": "2"
  },
  "optionalField": null,
  "isActive": true
}
```

**Commande :**
```bash
java -jar target/json-to-avro-converter.jar data.json schema.avsc
```

**Output Avro Schema (schema.avsc) :**
```json
{
  "type": "record",
  "name": "RootRecord",
  "namespace": "com.shanks.generated",
  "fields": [
    {
      "name": "id",
      "type": "string"
    },
    {
      "name": "userId",
      "type": {
        "name": "UserId",
        "type": "string",
        "logicalType": "uuid"
      }
    },
    {
      "name": "status",
      "type": "string"
    },
    {
      "name": "balance",
      "type": "string"
    },
    {
      "name": "tags",
      "type": {
        "type": "array",
        "items": {
          "type": "enum",
          "name": "TagsEnum",
          "namespace": "com.shanks.generated",
          "symbols": ["TAG_PREMIUM", "TAG_VERIFIED"]
        }
      }
    },
    {
      "name": "metadata",
      "type": {
        "type": "record",
        "name": "MetadataRecord",
        "namespace": "com.shanks.generated",
        "fields": [
          {
            "name": "created",
            "type": "string"
          },
          {
            "name": "version",
            "type": "string"
          }
        ]
      }
    },
    {
      "name": "optionalField",
      "type": ["null", "string"],
      "default": null
    },
    {
      "name": "isActive",
      "type": "boolean"
    }
  ]
}
```

### Exemple 3 : Objets Imbriqués (JSON Data)

**Input JSON :**
```json
{
  "user": {
    "name": "John Doe",
    "email": "john@example.com",
    "address": {
      "street": "123 Main St",
      "city": "New York"
    }
  }
}
```

**Output : Records imbriqués avec noms capitalisés**
```json
{
  "type": "record",
  "name": "RootRecord",
  "fields": [
    {
      "name": "user",
      "type": {
        "type": "record",
        "name": "UserRecord",
        "fields": [
          {"name": "name", "type": "string"},
          {"name": "email", "type": "string"},
          {
            "name": "address",
            "type": {
              "type": "record",
              "name": "AddressRecord",
              "fields": [
                {"name": "street", "type": "string"},
                {"name": "city", "type": "string"}
              ]
            }
          }
        ]
      }
    }
  ]
}
```

## 🏗️ Architecture

Le projet suit les **principes SOLID** pour assurer la maintenabilité et l'extensibilité :

### Structure des Packages

```
src/main/java/com/shanks/
├── App.java                             # Point d'entrée CLI
├── cli/
│   ├── CliArguments.java                # Parsing et validation des arguments
│   └── ConverterCli.java                # Orchestration CLI (JSON + OpenAPI)
├── converter/
│   ├── JsonToAvroConverter.java         # Convertisseur JSON → Avro
│   ├── OpenApiToAvroConverter.java      # Convertisseur OpenAPI → Avro
│   ├── TypeInferenceEngine.java         # Moteur d'inférence de types
│   ├── SchemaGenerator.java             # Générateur de schémas Avro (mode standard)
│   ├── UnifiedSchemaGenerator.java      # Générateur unifié avec références
│   └── interfaces/
│       └── TypeDetector.java            # Interface pour détecteurs (SOLID)
├── parser/
│   └── OpenApiParser.java               # Parser OpenAPI/Swagger (YAML/JSON)
├── mapper/
│   └── OpenApiToAvroTypeMapper.java     # Mapping types OpenAPI → Avro
├── model/
│   ├── JsonType.java                    # Enum des types JSON
│   ├── AvroTypeInfo.java                # Métadonnées de types Avro
│   └── InferredSchema.java              # Schéma inféré intermédiaire
└── util/
    ├── UuidDetector.java                # Détecteur UUID (implements TypeDetector)
    └── EnumDetector.java                # Détecteur ENUM (implements TypeDetector)

src/main/avro/                           # Schémas Avro (.avsc) - versionnés
├── README.md                            # Documentation des schémas
├── User.avsc
├── Product.avsc
└── Order.avsc

target/generated-sources/avro/           # Classes Java générées - NON versionnées
└── com/shanks/model/
    ├── User.java                        # Généré depuis User.avsc
    ├── Product.java                     # Généré depuis Product.avsc
    ├── ProductCategory.java             # Enum généré
    ├── Order.java                       # Généré depuis Order.avsc
    ├── OrderItem.java                   # Nested record généré
    ├── OrderStatus.java                 # Enum généré
    └── Address.java                     # Nested record généré
```

### Principes SOLID Appliqués

| Principe | Application |
|----------|-------------|
| **S** - Single Responsibility | Chaque classe a une responsabilité unique (ex: `TypeInferenceEngine` = inférence, `SchemaGenerator` = génération) |
| **O** - Open/Closed | Interface `TypeDetector` permet d'ajouter de nouveaux détecteurs sans modifier le code existant |
| **L** - Liskov Substitution | Tous les `TypeDetector` sont interchangeables |
| **I** - Interface Segregation | Interfaces minimales et focalisées (`TypeDetector` avec 4 méthodes seulement) |
| **D** - Dependency Inversion | `TypeInferenceEngine` dépend de l'abstraction `TypeDetector`, pas des implémentations |

## 🧪 Tests

Le projet contient **53 tests unitaires** couvrant tous les composants.

### Exécuter tous les tests

```bash
mvn test
```

### Exécuter des tests spécifiques

```bash
# Tests des détecteurs
mvn test -Dtest=UuidDetectorTest,EnumDetectorTest

# Test de l'application
mvn test -Dtest=AppTest
```

### Couverture des Tests

- ✅ Détection UUID (valide/invalide, arrays)
- ✅ Détection ENUM (patterns UPPER_CASE)
- ✅ Inférence de types primitifs
- ✅ Gestion des arrays et records
- ✅ Gestion des nulls et unions
- ✅ Parsing OpenAPI avec patterns
- ✅ Mapping des patterns OpenAPI → Avro
- ✅ Génération de schémas avec patterns (mode standard et unifié)
- ✅ Échappement correct des caractères spéciaux dans les patterns

## 📊 Détails Techniques

### Détection des Types

| Type | Méthode de Détection |
|------|---------------------|
| **UUID** | Regex : `^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$` |
| **ENUM** | Heuristique : `^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$` (ex: STATUS_ACTIVE) |
| **Array** | Analyse de **tous** les éléments pour déterminer le type |
| **Record** | Analyse récursive des objets imbriqués |
| **Null** | Création automatique d'union `["null", "type"]` avec `default: null` |

### Cas Limites Gérés

| Cas | Solution |
|-----|----------|
| Array vide | Défaut à `array<string>` |
| Types mixtes dans array | Union type avec tous les types détectés |
| Champs avec valeur null | Union type `["null", "string"]` avec `default: null` |
| Noms de champs invalides | Sanitisation (espaces → underscores) |
| Objets imbriqués | Records imbriqués avec noms capitalisés |

## 🔧 Dépendances

### Dependencies

| Dépendance | Version | Usage |
|------------|---------|-------|
| Apache Avro | 1.11.3 | Génération de schémas Avro |
| Jackson Databind | 2.16.1 | Parsing JSON |
| Jackson Core | 2.16.1 | Support Jackson |
| Swagger Parser | 2.1.22 | Parsing OpenAPI/Swagger (YAML/JSON) |
| JUnit Jupiter | 5.10.0 | Tests unitaires |
| AssertJ | 3.24.2 | Assertions fluides |

### Maven Plugins

| Plugin | Version | Usage |
|--------|---------|-------|
| avro-maven-plugin | 1.11.3 | Génération automatique de classes Java depuis schémas Avro |
| maven-shade-plugin | 3.5.1 | Création du Fat JAR |

## 📝 Configuration

### Personnalisation du Namespace

Par défaut, le namespace est `com.shanks.generated`. Pour le modifier, éditez `SchemaGenerator.java` :

```java
private static final String DEFAULT_NAMESPACE = "com.votreentreprise.schema";
```

### Ajout de Nouveaux Détecteurs

Grâce au principe **Open/Closed**, vous pouvez ajouter de nouveaux détecteurs sans modifier le code existant :

1. Créez une classe implémentant `TypeDetector`
2. Ajoutez-la dans `JsonToAvroConverter` :

```java
List<TypeDetector> detectors = Arrays.asList(
    new UuidDetector(),
    new EnumDetector(),
    new VotreNouveauDetector()  // ← Ajout ici
);
```

## 🐛 Dépannage

### Erreur : "NoClassDefFoundError"

Si vous utilisez le JAR normal au lieu du Fat JAR :
```bash
# ❌ Ne fonctionne pas
java -jar target/demo-1.0-SNAPSHOT.jar input.json output.avsc

# ✅ Utilisez le Fat JAR
java -jar target/json-to-avro-converter.jar input.json output.avsc
```

### Erreur : "Input file not found"

Vérifiez que le fichier JSON existe :
```bash
ls -la input.json
```

## 📄 Conformité Avro

Le convertisseur génère des schémas conformes à la spécification **Apache Avro 1.11.1** :
- [Specification | Apache Avro](https://avro.apache.org/docs/1.11.1/specification/)
- Union types avec null en première position
- Valeurs par défaut pour les champs nullable
- Logical types (UUID)

## 🔀 Branches et Approches de Génération Java

Ce projet propose **deux approches** pour générer des classes Java à partir de schémas Avro :

### Branch `main` / `feat/avro-maven-plugin` (⭐ Recommandée)
**Approche: Maven Plugin (Build-time)**

Utilise `avro-maven-plugin` pour générer automatiquement les classes pendant le build Maven.

**Avantages:**
- ✅ Automatique lors de `mvn compile`
- ✅ Intégration IDE native (IntelliJ/Eclipse)
- ✅ Schémas versionnés dans `src/main/avro/`
- ✅ Classes générées dans `target/` (non versionnées)
- ✅ Workflow standard Maven
- ✅ Parfait pour schémas stables

**Utilisation:**
```bash
# Les schémas sont dans src/main/avro/
mvn compile  # Génération automatique !
```

### Branch `feat/toJsonOrAvro`
**Approche: CLI avec SpecificCompiler (Runtime)**

Utilise une commande CLI avec l'API SpecificCompiler pour générer les classes à la demande.

**Avantages:**
- ✅ Flexibilité totale (génération à la demande)
- ✅ Mode batch (répertoires)
- ✅ Intégration avec workflow OpenAPI→Avro→Java
- ✅ Parfait pour génération dynamique

**Utilisation:**
```bash
# Génération manuelle via CLI
mvn exec:java -Dexec.args="schema.avsc output/ --generate-java"
# Ou batch mode
mvn exec:java -Dexec.args="schemas/ output/ --generate-java"
```

### Quelle Approche Choisir ?

| Critère | Maven Plugin | CLI SpecificCompiler |
|---------|--------------|---------------------|
| **Schémas stables** | ⭐ Parfait | Ok |
| **Génération dynamique** | Limité | ⭐ Parfait |
| **Intégration IDE** | ⭐ Native | Manuel |
| **CI/CD** | ⭐ Automatique | Requiert script |
| **Workflow OpenAPI→Avro→Java** | Multi-étapes | ⭐ Unifié |
| **Convention Maven** | ⭐ Standard | Custom |

**Recommandation:** Utilisez la branche `main` (Maven plugin) pour la plupart des cas. Utilisez `feat/toJsonOrAvro` si vous avez besoin de flexibilité pour générer dynamiquement des classes depuis des schémas créés à la volée.

## 🤝 Contribution

Les contributions sont les bienvenues ! Pour ajouter une fonctionnalité :

1. Fork le projet
2. Créez une branche (`git checkout -b feature/nouvelle-fonctionnalite`)
3. Committez vos changements (`git commit -m 'Ajout nouvelle fonctionnalité'`)
4. Pushez vers la branche (`git push origin feature/nouvelle-fonctionnalite`)
5. Ouvrez une Pull Request

### Guidelines

- Suivre les principes SOLID
- Ajouter des tests unitaires
- Documenter les nouvelles fonctionnalités
- Utiliser JUnit 5 pour les tests

## 📜 License

Ce projet est fourni tel quel pour utilisation éducative et professionnelle.

## 👨‍💻 Auteur

Développé avec l'architecture SOLID et les meilleures pratiques Java.

---

**Version** : 1.0-SNAPSHOT
**Java** : 21+
**Build Tool** : Maven 3.6+
