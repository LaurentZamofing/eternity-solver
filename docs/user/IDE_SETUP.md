# Configuration IDE pour Eternity Solver

## Problème: `package org.slf4j does not exist`

Ce problème survient quand l'IDE ne trouve pas les dépendances SLF4J.

## Solutions

### Solution 1: IntelliJ IDEA (Recommandé)

#### Option A: Ajouter les JARs comme bibliothèque
1. Dans la vue **Project**, clic droit sur le dossier `lib/`
2. Sélectionner **"Add as Library..."**
3. Choisir **"Project Library"**
4. Cliquer **OK**

#### Option B: Ajouter manuellement
1. **File → Project Structure** (Cmd+;)
2. **Project Settings → Libraries**
3. Cliquer **+ → Java**
4. Sélectionner tous les JARs dans `lib/`:
   - `slf4j-api-2.0.9.jar`
   - `logback-classic-1.4.11.jar`
   - `logback-core-1.4.11.jar`
5. Cliquer **OK**

#### Option C: Recharger Maven
1. Ouvrir **Maven** tool window (View → Tool Windows → Maven)
2. Cliquer l'icône **Reload** (flèches circulaires)

### Solution 2: Eclipse

1. Clic droit sur le projet → **Build Path → Configure Build Path**
2. Onglet **Libraries**
3. Cliquer **Add External JARs...**
4. Sélectionner tous les JARs dans `lib/`
5. Cliquer **OK**

### Solution 3: VS Code

1. Installer l'extension **Java Extension Pack**
2. Créer/modifier `.vscode/settings.json`:
```json
{
  "java.project.referencedLibraries": [
    "lib/**/*.jar"
  ]
}
```
3. Recharger la fenêtre (Cmd+Shift+P → "Reload Window")

### Solution 4: Ligne de Commande

Si vous préférez compiler en ligne de commande:

```bash
# Utiliser le script de compilation
./compile.sh

# Ou compiler manuellement
javac -d bin -sourcepath src -cp "lib/*" $(find src -name "*.java")

# Exécuter
java -cp "bin:lib/*" MainCLI --help
```

## Vérification

Après configuration, votre IDE devrait:
- ✅ Reconnaître les imports SLF4J
- ✅ Offrir l'auto-complétion pour Logger
- ✅ Ne plus afficher d'erreurs de compilation

## Test Rapide

Ouvrir `EternitySolver.java` et vérifier que ces lignes ne montrent pas d'erreur:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger logger = LoggerFactory.getLogger(EternitySolver.class);
```

## JARs Nécessaires

Le projet utilise:
- **SLF4J API 2.0.9**: Interface de logging
- **Logback Classic 1.4.11**: Implémentation
- **Logback Core 1.4.11**: Core Logback
- **JUnit 5.10.1**: Tests (déjà configuré)

Tous les JARs sont dans `lib/`.

## Support

Si le problème persiste:
1. Fermer l'IDE
2. Supprimer les caches IDE (`.idea/`, `.vscode/`, etc.)
3. Rouvrir le projet
4. Réappliquer la configuration
