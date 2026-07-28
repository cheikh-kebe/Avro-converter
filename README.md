# OpenAPI/JSON to Avro Converter + Java Code Generator

Outil Java qui génère des schémas **Apache Avro** à partir d'une spec **OpenAPI/Swagger** ou de données **JSON**, puis génère automatiquement les classes Java correspondantes.

## Sommaire

- [OpenAPI/JSON to Avro Converter + Java Code Generator](#openapijson-to-avro-converter--java-code-generator)
  - [Sommaire](#sommaire)
  - [En bref (pour tout le monde)](#en-bref-pour-tout-le-monde)
  - [Fonctionnalités clés](#fonctionnalités-clés)
  - [🚀 Quick Start](#-quick-start)
    - [Prérequis](#prérequis)
    - [Installation](#installation)
    - [Utilisation](#utilisation)
  - [📖 Documentation Détaillée](#-documentation-détaillée)
    - [OpenAPI/Swagger → Avro](#openapiswagger--avro)
    - [✅ Validation Avro \& erreurs lisibles](#-validation-avro--erreurs-lisibles)
    - [JSON → Avro](#json--avro)
    - [📬 Enveloppe `notif` (`.webhook.avsc`)](#-enveloppe-notif-webhookavsc)
    - [Avro Schema → JSON + Binaire](#avro-schema--json--binaire)
    - [Avro → Java (Maven Plugin)](#avro--java-maven-plugin)
  - [🏗️ Architecture](#️-architecture)
  - [🧪 Tests](#-tests)
  - [🔧 Dépendances](#-dépendances)
  - [📝 Exemples Rapides](#-exemples-rapides)
  - [🐛 Troubleshooting](#-troubleshooting)
  - [📄 License](#-license)

<a id="en-bref"></a>
## En bref (pour tout le monde)

**Le problème :** quand une équipe échange des données via Kafka (ou tout système basé sur des schémas), le schéma Avro doit rester parfaitement synchronisé avec le contrat d'API (OpenAPI) ou avec la structure réelle des données. Écrire et maintenir ces schémas à la main est source d'erreurs et de dérive entre la doc d'API et ce qui circule réellement sur le bus de messages.

**Ce que fait l'outil :** il lit la spec OpenAPI (ou un exemple JSON) et génère automatiquement le schéma Avro correspondant — types, enums, imbrications, patterns de validation — sans intervention manuelle. Il produit aussi les classes Java prêtes à l'emploi et un fichier JSON d'exemple pour tester rapidement.

**Pour qui :**
- **Dev backend / data engineer** : génère les schémas `.avsc` et les classes Java depuis la CLI ou au build Maven, sans écrire le schéma à la main.
- **BA / PO** : permet de vérifier qu'un schéma Avro proposé correspond bien au contrat d'API validé (les types, champs obligatoires et enums viennent directement de la spec OpenAPI — pas d'interprétation).

**Ce que ça ne fait pas :** ce n'est pas un serveur, pas un client Kafka — c'est un outil de conversion de schémas, exécuté en local ou en CI/CD, dont la sortie (fichiers `.avsc` / classes Java) est ensuite utilisée par vos applications.

<a id="fonctionnalites-cles"></a>
## Fonctionnalités clés

- 🔄 **OpenAPI/Swagger → Avro** : conversion de specs API en schémas Avro
- 🔄 **JSON → Avro** : inférence de schéma depuis des données JSON existantes
- ⚡ **Avro → Java** : génération automatique de classes Java (Maven plugin)
- 📦 **Avro → JSON → Binaire** : génération de JSON exemple et encodage en trame binaire Avro
- 📄 **Minification** : génération automatique d'une version one-line (`.min.avsc`) pour chaque schéma
- 📬 **Enveloppe `notif`** : génération automatique d'un fichier consolidé (`.webhook.avsc`) qui embarque le schéma dans une enveloppe `notif` (header + payload) — l'enveloppe elle-même est un template JSON interchangeable (`--envelope <nom>`), pas une structure figée
- ✅ **Validation Avro stricte** : noms de records/champs/enums et symboles d'enum invalides détectés à la conversion, avec un message lisible (pas une stacktrace) — identique en mode standard et en mode registry, voir [section dédiée](#-validation-avro--erreurs-lisibles)

<a id="quick-start"></a>
## 🚀 Quick Start

<a id="prerequis"></a>
### Prérequis
- Java 21+
- Maven 3.6+

<a id="installation"></a>
### Installation

```bash
# Build
mvn clean package

# Génère le Fat JAR: target/json-to-avro-converter.jar
```

<a id="utilisation"></a>
### Utilisation

```bash
# JSON → Avro Schema
java -jar target/json-to-avro-converter.jar data.json schema.avsc

# OpenAPI → Avro Schema (mode registry pour IBM/Confluent Schema Registry)
java -jar target/json-to-avro-converter.jar api.yaml output.avsc User --registry

# OpenAPI → Avro Schema depuis le requestBody d'une opération
java -jar target/json-to-avro-converter.jar api.yaml output.avsc --from-request-body /users POST

# Avro → Java (automatique)
mvn compile  # Les classes sont générées dans target/generated-sources/avro/

# Avro Schema → JSON exemple
java -jar target/json-to-avro-converter.jar generate src/main/avro/User.avsc User.json User

# JSON → Trame binaire Avro
java -jar target/json-to-avro-converter.jar encode src/main/avro/User.avsc User.json User.avro User

# Génération JSON + encodage binaire en une commande
java -jar target/json-to-avro-converter.jar encode src/main/avro/User.avsc --generate User.avro User
```

Chaque commande de conversion (JSON→Avro et OpenAPI→Avro, quel que soit le mode) produit systématiquement **3 fichiers** à partir d'un seul chemin de sortie `<nom>.avsc` :

| Fichier | Contenu |
|---|---|
| `<nom>.avsc` | Schéma Avro formaté (résultat brut de la conversion) |
| `<nom>.min.avsc` | Copie minifiée en une seule ligne du même schéma |
| `<nom>.webhook.avsc` | Le schéma consolidé dans un template d'enveloppe `notif` (voir [section dédiée](#-enveloppe-notif-webhookavsc)) |

<a id="documentation-detaillee"></a>
## 📖 Documentation Détaillée

[Diagramme d'architecture](./docs/diagrams/converter-architecture.drawio.png)

<a id="openapi-vers-avro"></a>
### OpenAPI/Swagger → Avro

**Fonctionnalités clés:**
- Support OpenAPI 3.0.x, 3.1.x, Swagger 2.0 (YAML/JSON)
- Conversion directe des types et enums
- Extraction automatique des patterns regex
- Résolution des `$ref` (y compris références directes et $ref multiples sur le même schéma)
- **Conversion depuis un schéma nommé** (`components/schemas`) ou **depuis le `requestBody`** d'une opération (`--from-request-body`)
- **Mode registry** (`--registry`) : schéma unique auto-contenu compatible IBM/Confluent Schema Registry
- **Mode doc** (`--doc`) : inclut les champs `doc` dans le schéma Avro, extraits des `description` OpenAPI
- **Périmètre fonctionnel** (`--functional-perimeter <nom>`) : ajoute un suffixe au namespace par défaut (`com.shanks.generated.<nom>`)
- **Enveloppe `notif`** (`--envelope <nom>`) : choisit le template d'enveloppe (`src/main/resources/envelopes/<nom>.json`) utilisé pour le `.webhook.avsc` — `default` si omis
- Génération automatique d'un fichier `.min.avsc` (JSON one-line) à côté du `.avsc`
- Génération automatique d'un fichier `.webhook.avsc` consolidant le schéma dans un template d'enveloppe `notif` (voir [section dédiée](#-enveloppe-notif-webhookavsc))

**Exemples:**

```bash
# Mode standard (fichiers séparés, types inline)
java -jar target/json-to-avro-converter.jar api.yaml output-dir/

# Schéma nommé depuis components/schemas
java -jar target/json-to-avro-converter.jar api.yaml User.avsc User

# Mode registry (schéma unique auto-contenu pour Schema Registry)
java -jar target/json-to-avro-converter.jar api.yaml User.avsc User --registry

# Avec champs doc (descriptions OpenAPI → doc Avro)
java -jar target/json-to-avro-converter.jar api.yaml User.avsc User --doc

# Registry + doc combinés
java -jar target/json-to-avro-converter.jar api.yaml User.avsc User --registry --doc

# Depuis le requestBody d'une opération (path + méthode HTTP)
java -jar target/json-to-avro-converter.jar api.yaml CreateUser.avsc --from-request-body /users POST

# Idem, combinable avec --registry et --doc
java -jar target/json-to-avro-converter.jar api.yaml CreateUser.avsc --from-request-body /users POST --registry --doc

# Avec un périmètre fonctionnel personnalisé (namespace: com.shanks.generated.users)
java -jar target/json-to-avro-converter.jar api.yaml CreateUser.avsc --functional-perimeter users --from-request-body /users POST

# Avec un template d'enveloppe notif différent (voir section dédiée)
java -jar target/json-to-avro-converter.jar api.yaml User.avsc User --envelope minimal
```

**Périmètre fonctionnel (`--functional-perimeter <nom>`):**

Ajoute `<nom>` en suffixe du namespace par défaut (`com.shanks.generated` → `com.shanks.generated.<nom>`), utile pour isoler les schémas de plusieurs domaines métier générés depuis la même spec OpenAPI. Compatible avec tous les autres modes (`--registry`, `--doc`, `--from-request-body`, schéma nommé). Placé n'importe où dans les arguments après `<output.avsc>`.

**Conversion depuis le `requestBody` (`--from-request-body <path> <méthode>`):**

Plutôt que de cibler un schéma nommé dans `components/schemas`, ce mode va chercher directement le schéma JSON déclaré dans le `requestBody` d'une opération donnée (`paths.<path>.<méthode>.requestBody.content['application/json'].schema`). Utile quand le payload d'entrée d'un endpoint n'est pas (ou pas entièrement) un schéma nommé.

- `<path>` : la clé du path OpenAPI telle qu'écrite dans le spec, y compris les accolades (ex: `/orders/{orderId}/cancel`) — à mettre entre guillemets si le shell interprète les `{}`.
- `<méthode>` : `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `HEAD`, `OPTIONS` ou `TRACE` (insensible à la casse).
- Le nom du record Avro généré est dérivé automatiquement :
  - depuis l'`operationId` de l'opération s'il est présent (ex: `createUser` → `CreateUser`) ;
  - sinon depuis le path + la méthode (ex: `/orders/{orderId}/cancel` + `POST` → `OrdersOrderIdCancelPost`).
- Si le schéma du `requestBody` est un `$ref`, il est résolu comme n'importe quel autre `$ref` (imbrication, enums, patterns compris).
- Erreurs explicites si le path, la méthode ou le `requestBody` n'existent pas dans le spec.

```bash
# Exemple avec fallback de nom (pas d'operationId)
java -jar target/json-to-avro-converter.jar api.yaml Cancel.avsc --from-request-body "/orders/{orderId}/cancel" POST
```

**Mapping des types (OpenAPI → Avro) :**

| Type OpenAPI | Format | Type Avro |
|---|---|---|
| `string` | (aucun) | `string` |
| `string` | `uuid` | `string` + `logicalType: uuid` |
| `string` | `date` / `date-time` | `string` |
| `boolean` | — | `boolean` |
| `object` | — | `record` |
| `array` | — | `array` |
| `integer` | tous formats (`int32`, `int64`) | `string` |
| `number` | tous formats (`float`, `double`) | `string` |
| valeurs d'enum | — | `enum` |

> ⚠️ Les types numériques et date/heure sont **volontairement mappés en `string`**, pour éviter toute perte de précision et simplifier la compatibilité entre systèmes. Ce n'est pas un bug : c'est un choix de conception assumé.

<a id="validation-avro"></a>
### ✅ Validation Avro & erreurs lisibles

Les noms de records, de champs, d'enums et les symboles d'enum doivent respecter la règle de nommage Avro (`[A-Za-z_][A-Za-z0-9_]*` — commencer par une lettre ou un underscore). Cette validation est effectuée **avant** l'écriture des fichiers de sortie, et de façon **strictement identique en mode standard et en mode registry** — les deux modes construisent le schéma via le même composant interne (`AvroSchemaBuilder`), donc un schéma invalide échoue de la même manière quel que soit le mode utilisé.

Concrètement, une valeur d'enum comme `"1RED"` (spec OpenAPI) fait échouer la conversion avec un message explicite plutôt qu'une stacktrace Java :

```
Error: Invalid Avro enum symbol

  Value:   "1RED"
  Where:   enum com.shanks.generated.creditcard.CardType
  Reason:  Avro names must start with a letter or underscore, and contain only
           letters, digits, and underscores (rule: [A-Za-z_][A-Za-z0-9_]*)

  Fix: rename this value in your OpenAPI/JSON source.
```

- Cette validation couvre : nom de record, nom de champ, nom d'enum, symbole d'enum.
- Pour toute autre erreur inattendue (bug, fichier illisible, etc.), la stacktrace Java complète est masquée par défaut. Ajoutez `--stacktrace` pour l'afficher :
  ```bash
  java -jar target/json-to-avro-converter.jar api.yaml User.avsc User --registry --stacktrace
  ```
- Intérêt pour le mode registry : les erreurs qu'un vrai schema registry (IBM/Confluent) rejetterait à la publication sont désormais détectées ici, à la conversion.

<a id="json-vers-avro"></a>
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
# → Génère schema.avsc (formaté) + schema.min.avsc (one-line) + schema.webhook.avsc (enveloppe notif)
```

<a id="enveloppe-notif"></a>
### 📬 Enveloppe `notif` (`.webhook.avsc`)

Chaque conversion (JSON→Avro ou OpenAPI→Avro, tous modes confondus) génère systématiquement, en plus de `<nom>.avsc` et `<nom>.min.avsc`, un troisième fichier `<nom>.webhook.avsc` qui consolide le schéma généré dans une enveloppe `notif`.

**L'enveloppe est un template JSON, pas une structure figée dans le code.** Elle est chargée depuis `src/main/resources/envelopes/<nom>.json` (`<nom>` choisi via `--envelope <nom>`, `default` si omis). Le code qui fait l'injection (`NotifWrapperGenerator`) ne connaît rien de la structure de ce template : il cherche récursivement, n'importe où dans l'arbre, un champ nommé `payload` et y injecte le schéma généré. Ajouter une nouvelle version d'enveloppe = ajouter un fichier `.json`, sans toucher au code Java.

**Template `default` :**

```
Notif (record racine)
├── header : Header (record)
│   ├── technical : Technical (record, vide pour l'instant)
│   └── functional : Functional (record, vide pour l'instant)
└── payload : <le schéma généré, injecté ici>
```

**Template `minimal`** (exemple d'une structure différente, sans `header`) :

```
Notif (record racine)
└── payload : <le schéma généré, injecté ici>
```

- Dans un template, le jeton littéral `${namespace}` est remplacé par le namespace du schéma généré (donc suit `--functional-perimeter` automatiquement) avant que le JSON ne soit parsé.
- Le fichier `.webhook.avsc` est toujours généré — pas de flag pour le désactiver, seulement pour changer quel template l'enveloppe.
- Fonctionne à l'identique en Mode Standard et Mode Registry.
- Une enveloppe inconnue (`--envelope` sans template correspondant) ou un template sans champ `payload` fait échouer la conversion avec un message explicite, avant l'écriture de tout fichier.

```bash
java -jar target/json-to-avro-converter.jar api.yaml User.avsc User
# → Génère User.avsc, User.min.avsc, User.webhook.avsc (enveloppe "default")

java -jar target/json-to-avro-converter.jar api.yaml User.avsc User --envelope minimal
# → Même chose, mais User.webhook.avsc utilise le template "minimal" (payload à la racine)
```

<a id="avro-vers-json-binaire"></a>
### Avro Schema → JSON + Binaire

**Deux sous-commandes** pour générer du JSON et des trames binaires Avro à partir de schémas :

**`generate`** — Génère un JSON exemple (format Avro JSON encoding) à partir d'un schéma `.avsc` :
```bash
# Génère output.json avec des valeurs par défaut cohérentes
java -jar target/json-to-avro-converter.jar generate src/main/avro/User.avsc output.json User
```

**`encode`** — Encode du JSON en fichier binaire `.avro` (container format avec header + schema embarqué) :
```bash
# Depuis un fichier JSON existant
java -jar target/json-to-avro-converter.jar encode src/main/avro/User.avsc User.json User.avro User

# Auto-génération + encodage en une commande
java -jar target/json-to-avro-converter.jar encode src/main/avro/User.avsc --generate User.avro User
```

**Types supportés pour la génération JSON :**
- `string` → `"example_string"`, UUID → UUID aléatoire
- `int`/`long`/`float`/`double` → `0`
- `boolean` → `false`
- `enum` → premier symbole
- `array` → `[]`
- `record` → objet récursif
- `timestamp-millis` → timestamp courant
- unions `["null", T]` → valeur non-null wrappée (format Avro JSON encoding)

<a id="avro-vers-java"></a>
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
    {"name": "age", "type": ["null", "string"], "default": null}
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
- Support des logical types (UUID → `java.util.UUID`)

<a id="architecture"></a>
## 🏗️ Architecture

```
src/main/java/com/shanks/
├── cli/              # CLI et parsing arguments
├── converter/        # Convertisseurs (JSON, OpenAPI) + orchestration + écriture des 3 fichiers
├── serializer/       # Génération JSON exemple + encodage binaire Avro (sous-commandes generate/encode)
├── parser/           # Parser OpenAPI
├── mapper/           # Mapping types (OpenAPI → Avro)
├── model/            # Modèles de données
└── util/             # Détecteurs (UUID, ENUM)

src/main/avro/        # Schémas Avro versionnés
target/generated-sources/avro/  # Classes Java générées
```

**Principes:** SOLID, injection de dépendances, séparation des responsabilités.

**Stratégie de namespace :** chaque type nommé (record ou enum) reçoit un namespace hiérarchique qui encode sa position dans l'arbre du schéma (`parentNamespace.parentRecordName`), ce qui garantit des noms uniques même quand un même nom de type (ex: `Address`) apparaît à plusieurs endroits du schéma — sans quoi `mvn compile` échouerait avec une erreur `can't redefine`.

<a id="tests"></a>
## 🧪 Tests

```bash
mvn test  # tests unitaires (JUnit 5 + AssertJ)
```

<a id="dependances"></a>
## 🔧 Dépendances

- Apache Avro 1.11.3
- Jackson 2.16.1 (JSON)
- Swagger Parser 2.1.22 (OpenAPI)
- avro-maven-plugin 1.11.3

<a id="exemples-rapides"></a>
## 📝 Exemples Rapides

**Mode Registry vs Standard (OpenAPI):**

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
# Mode registry (1 fichier auto-contenu, types imbriqués inline puis référencés par nom qualifié)
java -jar target/json-to-avro-converter.jar test-openapi.yaml CreditCard.avsc CreditCard --registry
# → Enum défini inline à la première occurrence, référencé par nom qualifié ensuite

# Mode standard (types inline dans le record, dédupliqués automatiquement par l'API Avro)
java -jar target/json-to-avro-converter.jar test-openapi.yaml CreditCard.avsc CreditCard
# → Enum inline dans le record

# Avec doc (descriptions OpenAPI incluses comme champs "doc" dans le schéma Avro)
java -jar target/json-to-avro-converter.jar test-openapi.yaml CreditCard.avsc CreditCard --doc
# → Ajoute "doc": "..." sur les records, fields et enums ayant une description
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

<a id="troubleshooting"></a>
## 🐛 Troubleshooting

**NoClassDefFoundError:** Utilisez le Fat JAR `json-to-avro-converter.jar`, pas `demo-1.0-SNAPSHOT.jar`

**Classes non générées:** Exécutez `mvn clean generate-sources`

**IDE ne voit pas les classes:** Recharger le projet Maven (IntelliJ: Maven → Reload)

**Erreur `can't redefine` à la compilation :** deux fichiers `.avsc` définissent un type avec le même nom complet (namespace + nom). Vérifiez que chaque schéma généré utilise bien un namespace distinct (voir [Stratégie de namespace](#️-architecture)), ou passez `--functional-perimeter` pour isoler les domaines.

**`Error: Invalid Avro enum symbol` / `Invalid Avro record name` / `Invalid Avro field name` à la conversion :** un nom ou une valeur d'enum de votre spec OpenAPI (ou de vos données JSON) ne respecte pas la règle de nommage Avro (`[A-Za-z_][A-Za-z0-9_]*` — doit commencer par une lettre ou un underscore, ex: `"1RED"` est invalide). Renommez la valeur fautive à la source ; voir [Validation Avro & erreurs lisibles](#-validation-avro--erreurs-lisibles). Pour voir la stacktrace Java complète en cas d'erreur inattendue, ajoutez `--stacktrace`.

<a id="license"></a>
## 📄 License

Usage éducatif et professionnel.

---

**Version:** 1.0-SNAPSHOT | **Java:** 21+ | **Maven:** 3.6+
