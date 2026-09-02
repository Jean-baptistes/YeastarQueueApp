# Obtenir l'APK sans rien installer sur votre PC (GitHub Actions)

Ce guide vous fait compiler l'application « en ligne » grâce à GitHub, puis
installer l'APK sur votre téléphone Android. **Aucun logiciel à installer sur
votre ordinateur.** Comptez ~10 minutes la première fois.

---

## Étape 1 — Créer un compte GitHub (gratuit)
1. Allez sur <https://github.com> et cliquez **Sign up**.
2. Suivez l'inscription (email + mot de passe). C'est gratuit.

## Étape 2 — Créer un dépôt (repository)
1. En haut à droite, cliquez le **+** puis **New repository**.
2. *Repository name* : par ex. `appel-file-yeastar`.
3. Laissez **Public** (ou Private, peu importe).
4. Cliquez **Create repository**.

## Étape 3 — Déposer le projet dans le dépôt
> Vous avez le dossier du projet (décompressez le fichier `YeastarQueueApp.zip`
> reçu dans la conversation). Il faut envoyer **le contenu** de ce dossier.

1. Sur la page du dépôt vide, cliquez le lien **« uploading an existing file »**
   (ou bouton **Add file → Upload files**).
2. Ouvrez le dossier `YeastarQueueApp` sur votre PC, **sélectionnez tout**
   (Ctrl+A) et **glissez-déposez** les fichiers/dossiers dans la page GitHub.
   - ⚠️ Important : déposez bien le **contenu** (les dossiers `app`, `gradle`,
     `.github`, et les fichiers `settings.gradle.kts`, `gradlew`, etc.),
     pas seulement le dossier parent.
   - Si le dossier `.github` n'apparaît pas dans votre explorateur, activez
     l'affichage des fichiers cachés, il est indispensable (il contient le
     script de compilation).
3. En bas, cliquez **Commit changes**.

## Étape 4 — Laisser GitHub compiler l'APK
1. Cliquez l'onglet **Actions** en haut du dépôt.
2. Vous verrez un job **« Build APK »** qui tourne (rond orange). Attendez
   qu'il devienne **vert** (≈ 3–6 minutes).
   - S'il ne démarre pas seul : cliquez **Build APK** dans la liste de gauche,
     puis **Run workflow** à droite.

## Étape 5 — Télécharger l'APK
Deux possibilités :

**A. Via la Release (le plus simple pour le téléphone)**
1. Retournez à la page d'accueil du dépôt (onglet **Code**).
2. À droite, section **Releases**, cliquez **Dernière version APK** (tag `latest`).
3. Sous **Assets**, téléchargez **`AppelFileYeastar.apk`**.
   → Ouvrez ce lien **directement depuis le navigateur de votre téléphone**
     pour récupérer l'APK sur l'appareil.

**B. Via l'artefact du build**
1. Onglet **Actions** → cliquez le build vert.
2. En bas, section **Artifacts** → **AppelFileYeastar-APK** (fichier .zip
   contenant l'APK ; à décompresser).

## Étape 6 — Installer l'APK sur Android
1. Sur le téléphone, ouvrez le fichier **AppelFileYeastar.apk** téléchargé.
2. Android demandera d'autoriser l'installation d'applications de source inconnue :
   **Paramètres → Autoriser pour cette source** (navigateur ou gestionnaire de
   fichiers), puis revenez et confirmez **Installer**.
3. Ouvrez l'application **« Appel File Yeastar »**.

## Étape 7 — Configurer et appeler
1. Icône **engrenage** (Paramètres) :
   - **Adresse du PBX** : IP/domaine de votre Yeastar (ex. `192.168.1.10`).
   - **Port / Transport** : `5060` + UDP pour un test local ; `5061` + TLS en production.
   - **Numéro de la file** : `6400`.
   - **Ajouter une extension** : numéro + mot de passe SIP (défini dans le PBX).
2. Revenez à l'écran principal, **sélectionnez l'extension** (pastille verte = prête).
3. Appuyez sur le **bouton vert** pour appeler la file **6400**. 🎉

---

## Mettre à jour l'application plus tard
Modifiez un fichier dans le dépôt (ou re-déposez une nouvelle version) →
GitHub recompile automatiquement → un nouvel APK apparaît dans la Release `latest`.

## En cas d'échec du build
- Onglet **Actions** → ouvrez le build rouge → lisez l'étape en erreur.
- Cause la plus fréquente : le dossier `.github` n'a pas été envoyé, ou la
  version du SDK Linphone. Envoyez-moi le message d'erreur, je corrige.
