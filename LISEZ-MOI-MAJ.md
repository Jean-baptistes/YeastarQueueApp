# Mise à jour de l'application — 2026-09-09

## Ce qui a changé

`.github/workflows/main.yml` est le **seul fichier modifié** : son bloc
`PROJECT_B64` contient désormais la nouvelle version de l'application
(le mécanisme base64 est conservé, comme demandé).

L'application ne balaie plus une plage d'extensions et ne contient plus de mot
de passe SIP. Elle demande une ligne au serveur 3CA, qui la lui prête et retient
qui l'utilise — c'est ce qui permet à l'opérateur de savoir qui appelle et de
rappeler la bonne personne.

Nouveautés : écran d'inscription, identifiant unique `MOB-XXXXXX`, journal des
appels, et **réception des rappels** du centre d'appel (Firebase).

## Pour publier un nouvel APK

```bash
cd /home/cti/YeastarQueueApp-maj
git add .github/workflows/main.yml
git commit -m "App mobile : baux d'extension, identification et rappels"
git push
```

GitHub Actions recompile et met à jour la Release `latest`
(`AppelFileYeastar.apk`).

## Avant que les rappels fonctionnent

Lire `FIREBASE.md` (embarqué dans le projet) : il faut créer le projet Firebase
et remplacer `app/google-services.json`. Le fichier actuellement embarqué est un
**fichier de remplacement** : il permet au build de passer, les appels sortants
fonctionnent, mais l'application ne peut pas être rappelée.

## Source lisible

Le code source décodé est disponible dans `/home/cti/YeastarQueueApp-source-lisible`.
Après modification, réencoder :

```bash
cd /home/cti && zip -r project.zip YeastarQueueApp-source-lisible  # (renommer le dossier en YeastarQueueApp)
base64 -w0 project.zip | fold -w120 | sed 's/^/            /' > b64.txt
# puis remplacer le bloc sous « PROJECT_B64: | » dans .github/workflows/main.yml
```
