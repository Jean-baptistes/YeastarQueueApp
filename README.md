# Appel File Yeastar — Application Android

Application Android native (Kotlin + Jetpack Compose) qui se connecte à un
**PBX Yeastar P-Series** en SIP, permet de **choisir une extension disponible**
dans un pool de postes, puis d'**appeler la file d'attente 6400** (configurable)
d'un simple bouton.

La partie téléphonie (enregistrement SIP, appel, audio) s'appuie sur le
**SDK Linphone**, une pile SIP éprouvée et open source. Les extensions Yeastar
étant des comptes SIP standard, aucune dépendance propriétaire n'est nécessaire.

---

## 1. Fonctionnalités

- Configuration du serveur PBX (adresse, port, transport UDP/TCP/TLS).
- Gestion d'un **pool d'extensions** (numéro + mot de passe SIP + libellé),
  stocké localement sur le téléphone.
- Sélection d'une extension → **enregistrement SIP** automatique auprès du PBX.
  Une extension qui s'enregistre correctement = **disponible** (pastille verte).
- Bouton **« Appeler la file 6400 »** (numéro modifiable dans les paramètres).
- Contrôles en communication : couper le micro, haut-parleur, raccrocher.
- Service de premier plan pour garder l'appel actif en arrière-plan.

---

## 2. Prérequis

- **Android Studio** (version récente : Koala 2024.1+ ou plus récent).
- Un JDK 17 (fourni par Android Studio).
- Un PBX **Yeastar P-Series** accessible depuis le téléphone (même réseau
  Wi-Fi/LAN, VPN, ou exposé publiquement selon votre configuration).
- Au moins une **extension SIP** créée dans le PBX, dont vous connaissez le
  **mot de passe d'enregistrement** (Registration Password).

---

## 3. Compilation

> 🚀 **Le plus simple pour obtenir un APK installable sans rien installer :**
> voir **[GUIDE-APK.md](GUIDE-APK.md)** — GitHub compile l'APK pour vous
> (workflow `.github/workflows/build-apk.yml`) et vous le télécharge.
>
> La méthode Android Studio ci-dessous reste possible si vous préférez.

> ⚠️ Ce projet n'a pas pu être compilé dans l'environnement où il a été généré
> (pas d'accès réseau aux dépôts Google/Maven/Linphone). Faites une première
> compilation dans Android Studio, qui téléchargera les dépendances.

1. Ouvrez le dossier `YeastarQueueApp` dans **Android Studio**
   (*File → Open*).
2. Android Studio crée automatiquement `local.properties` avec le chemin du
   SDK Android. Si besoin, créez-le manuellement :
   ```
   sdk.dir=/chemin/vers/Android/Sdk
   ```
3. Laissez Gradle se synchroniser (**Sync Now**). Il télécharge :
   - Android Gradle Plugin 8.5.2, Kotlin 1.9.24, Jetpack Compose ;
   - le **SDK Linphone** depuis `https://download.linphone.org/maven_repository`.
4. Branchez un téléphone (mode développeur + débogage USB) ou lancez un
   émulateur, puis **Run ▶**.

En ligne de commande (si le SDK Android est configuré) :
```bash
./gradlew assembleDebug      # génère app/build/outputs/apk/debug/app-debug.apk
```

### Version du SDK Linphone
Le fichier `app/build.gradle.kts` utilise `org.linphone:linphone-sdk-android:5.4.+`
(recommandation officielle). Pour figer une version précise, consultez la liste
publiée sur <https://download.linphone.org/maven_repository/org/linphone/linphone-sdk-android/>
et remplacez `5.4.+` par ex. `5.4.19`.

---

## 4. Configuration côté application

Au premier lancement :

1. Ouvrez **Paramètres** (icône engrenage en haut à droite).
2. **Serveur PBX Yeastar** :
   - *Adresse* : IP ou domaine du PBX (ex. `192.168.1.10` ou `pbx.exemple.com`).
   - *Port* : `5060` pour UDP/TCP, `5061` pour TLS.
   - *Transport* : **TLS** recommandé en production ; **UDP** pour un test rapide
     en réseau local.
   - *Numéro de la file* : `6400` par défaut.
3. **Pool d'extensions** : ajoutez un ou plusieurs postes
   (numéro d'extension + mot de passe SIP défini dans le PBX).
4. Revenez à l'écran d'appel, choisissez une extension (elle s'enregistre),
   puis appuyez sur le bouton vert pour appeler la file.

---

## 5. Configuration côté PBX Yeastar (P-Series)

Pour que l'enregistrement SIP fonctionne :

- **Extensions** : chaque extension du pool doit exister avec le type **SIP**.
  Récupérez le *Registration Password* dans *Extension → onglet Security/User*.
- **Codecs** : laissez au moins un codec audio courant activé (PCMU/PCMA/G722/Opus).
- **Registration/Transport** : autorisez le transport choisi (UDP/TCP/TLS) dans
  *PBX Settings → SIP Settings → Transport*. En TLS, assurez-vous que le
  certificat du PBX est accepté par le téléphone.
- **Sécurité** : si vous appelez depuis l'extérieur du LAN, ajoutez l'IP du
  téléphone/VPN aux règles d'accès (Auto Defense / Static Defense) pour éviter
  le blocage des tentatives d'enregistrement.
- **File d'attente 6400** : vérifiez qu'elle existe (*Call Features → Queues*)
  et que l'extension utilisée a le droit de l'appeler.

---

## 6. Notion de « disponibilité » d'une extension

Cette version considère qu'une extension est **disponible** si elle **s'enregistre
correctement** (pas déjà prise par un autre appareil avec le même compte, bons
identifiants, PBX joignable).

Pour une vraie détection de présence (poste libre / occupé / en ligne — BLF), il
faut interroger l'**API Yeastar P-Series** (Linkus/OpenAPI) : authentification par
compte API, puis récupération du *presence/extension status*. C'est une évolution
possible : le code est structuré pour l'ajouter (voir `data/` et `ui/MainViewModel`).

---

## 7. Architecture du code

```
app/src/main/java/com/yeastar/queuecaller/
├── QueueCallerApp.kt        Application : initialise la pile SIP
├── MainActivity.kt          Permissions (micro/notifs) + hôte Compose
├── data/
│   ├── Models.kt            PbxConfig, Extension, états SIP/appel
│   └── AppSettings.kt       Persistance (SharedPreferences)
├── sip/
│   ├── SipManager.kt        Cœur SIP (Linphone) : register + appel + audio
│   └── CallForegroundService.kt   Service de premier plan pendant l'appel
└── ui/
    ├── MainViewModel.kt     Lien UI ↔ persistance ↔ SIP
    ├── AppRoot.kt           Navigation + erreurs
    ├── CallScreen.kt        Sélection extension + bouton d'appel
    ├── SettingsScreen.kt    Config PBX + gestion du pool
    └── Theme.kt             Thème Material 3
```

---

## 8. Dépannage

| Symptôme | Piste |
|---|---|
| « Échec d'enregistrement » | Vérifiez adresse/port/transport, le mot de passe SIP, et que le PBX est joignable depuis le téléphone. |
| Enregistré mais l'appel échoue | Droit d'appel de la file 6400, codecs audio, NAT/SIP ALG du routeur. |
| Pas de son | Autorisation micro accordée ; testez le haut-parleur ; vérifiez les codecs. |
| TLS ne s'enregistre pas | Certificat du PBX non reconnu ; testez d'abord en TCP/UDP en réseau local. |

---

*Généré comme point de départ complet et fonctionnel. La logique SIP suit le
tutoriel officiel du SDK Linphone ; adaptez la version du SDK si nécessaire.*
