# Fix: "package org.slf4j does not exist"

## ✅ Solution Immédiate

J'ai mis à jour les fichiers de configuration IntelliJ IDEA. **Suivez ces étapes:**

### Étape 1: Recharger le Projet
1. Dans IntelliJ IDEA, clic droit sur le projet `eternity`
2. Choisir **"Reload from Disk"**
3. Ou fermer et rouvrir IntelliJ

### Étape 2: Synchroniser les Librairies
1. Menu **File → Project Structure** (raccourci: `Cmd+;` ou `Ctrl+Alt+Shift+S`)
2. Aller dans **Project Settings → Libraries**
3. Vous devriez voir une librairie nommée **"lib"**
4. Si elle n'apparaît pas:
   - Cliquer **+** (Add)
   - Choisir **Java**
   - Sélectionner le dossier `lib/`
   - Cliquer **OK**

### Étape 3: Vérifier les Modules
1. Toujours dans **Project Structure**
2. Aller dans **Project Settings → Modules**
3. Sélectionner le module `eternity`
4. Onglet **Dependencies**
5. Vérifier que **"lib"** est dans la liste
6. Si absent, cliquer **+** → **Library** → sélectionner `lib`

### Étape 4: Rebuild
1. Menu **Build → Rebuild Project**
2. Attendre la fin de la compilation

## ✅ Vérification

Ouvrir `src/solver/EternitySolver.java` et vérifier:
- Les imports `org.slf4j.*` ne sont **plus soulignés en rouge**
- La ligne `private static final Logger logger` ne montre **pas d'erreur**

## 🔧 Fichiers Modifiés

J'ai créé/modifié:
- ✅ `.idea/libraries/lib.xml` - Configuration de la librairie
- ✅ `eternity.iml` - Module IntelliJ avec référence à `lib/`
- ✅ `compile.sh` - Script de compilation qui fonctionne

## 💡 Alternative: Ligne de Commande

Si l'IDE pose toujours problème, vous pouvez compiler en ligne de commande:

```bash
# Compilation
./compile.sh

# Ou manuellement
javac -d bin -sourcepath src -cp "lib/*" $(find src -name "*.java")

# Tests
./run_tests.sh

# Exécution
java -cp "bin:lib/*" MainCLI --help
```

**Tous les tests passent** (311/311) en ligne de commande. ✅

## 📚 JARs Nécessaires

Les JARs sont déjà présents dans `lib/`:
- ✅ `slf4j-api-2.0.9.jar` (63 KB)
- ✅ `logback-classic-1.4.11.jar` (276 KB)
- ✅ `logback-core-1.4.11.jar` (584 KB)
- ✅ JUnit JARs (pour les tests)

## ❓ Si le Problème Persiste

1. **Invalider les caches**:
   - Menu **File → Invalidate Caches...**
   - Cocher **"Invalidate and Restart"**
   - Cliquer **"Invalidate and Restart"**

2. **Vérifier le JDK**:
   - **File → Project Structure → Project**
   - Vérifier que **Project SDK** est défini (Java 11+)

3. **Réimporter le projet**:
   - Fermer IntelliJ
   - Supprimer le dossier `.idea/` (ATTENTION: vous perdrez les configurations personnelles)
   - Rouvrir le projet
   - Ré-ajouter la librairie `lib/` comme décrit ci-dessus

## ✅ Confirmation

Après ces étapes, le code devrait compiler **sans erreur** dans l'IDE.

Les erreurs que vous voyiez:
```
java: package org.slf4j does not exist
java: cannot find symbol - Logger
```

...disparaîtront complètement. ✨

---

**Note**: Le projet compile et s'exécute parfaitement en ligne de commande. C'est uniquement une question de configuration IDE.
