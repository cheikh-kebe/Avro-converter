# JSON to Avro Schema Converter

Un convertisseur CLI Java qui génère automatiquement des schémas Avro (.avsc) à partir de fichiers JSON avec inférence intelligente des types.

## 🎯 Fonctionnalités

- ✅ **Inférence automatique des types** : Détection intelligente des types primitifs et complexes
- ✅ **Types primitifs** : string, boolean
- ✅ **Types complexes** :
  - **UUID** : Détection par regex avec logical type
  - **ENUM** : Détection heuristique (UPPER_CASE avec underscores)
  - **Arrays** : Analyse complète de tous les éléments
  - **Records imbriqués** : Support récursif
- ✅ **Gestion des nulls** : Génération automatique d'union types `["null", "type"]` avec `default: null`
- ✅ **Noms capitalisés** : Types complexes avec noms en PascalCase
- ✅ **Architecture SOLID** : Code maintenable et extensible
- ✅ **Fat JAR** : Exécutable autonome sans dépendances externes

## 📋 Prérequis

- **Java** 21 ou supérieur
- **Maven** 3.6+

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

### Méthode 1 : Fat JAR (Recommandé)

```bash
java -jar target/json-to-avro-converter.jar <input.json> <output.avsc>
```

**Exemple :**
```bash
java -jar target/json-to-avro-converter.jar data.json schema.avsc
```

### Méthode 2 : Via Maven

```bash
mvn exec:java -Dexec.mainClass="com.shanks.App" -Dexec.args="<input.json> <output.avsc>"
```

**Exemple :**
```bash
mvn exec:java -Dexec.mainClass="com.shanks.App" -Dexec.args="data.json schema.avsc"
```

## 📚 Exemples

### Exemple 1 : Types Primitifs et Complexes

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

### Exemple 2 : Objets Imbriqués

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
com.shanks/
├── App.java                          # Point d'entrée CLI
├── cli/
│   ├── CliArguments.java             # Parsing et validation des arguments
│   └── ConverterCli.java             # Orchestration CLI
├── converter/
│   ├── JsonToAvroConverter.java      # Orchestrateur principal
│   ├── TypeInferenceEngine.java      # Moteur d'inférence de types
│   ├── SchemaGenerator.java          # Générateur de schémas Avro
│   └── interfaces/
│       └── TypeDetector.java         # Interface pour détecteurs (SOLID)
├── model/
│   ├── JsonType.java                 # Enum des types JSON
│   ├── AvroTypeInfo.java             # Métadonnées de types Avro
│   └── InferredSchema.java           # Schéma inféré intermédiaire
└── util/
    ├── UuidDetector.java             # Détecteur UUID (implements TypeDetector)
    └── EnumDetector.java             # Détecteur ENUM (implements TypeDetector)
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

Le projet contient **17 tests unitaires** couvrant tous les composants.

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

| Dépendance | Version | Usage |
|------------|---------|-------|
| Apache Avro | 1.11.3 | Génération de schémas Avro |
| Jackson Databind | 2.16.1 | Parsing JSON |
| Jackson Core | 2.16.1 | Support Jackson |
| JUnit Jupiter | 5.10.0 | Tests unitaires |
| AssertJ | 3.24.2 | Assertions fluides |
| Maven Shade Plugin | 3.5.1 | Création du Fat JAR |

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
