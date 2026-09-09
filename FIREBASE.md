# Firebase — état et ce qu'il reste à faire

Projet Firebase : **`rappel-requerent`** (numéro `1047370915264`).

> **Où sont les fichiers ?** Le code de l'application est embarqué, encodé en
> base64, dans `.github/workflows/main.yml` (variable `PROJECT_B64`). Les
> chemins ci-dessous désignent des fichiers **à l'intérieur de cette archive**.

## ✅ Fait — côté application

`app/google-services.json` contient la vraie configuration du projet
`rappel-requerent`, pour le package `com.yeastar.queuecaller`. L'application
peut donc obtenir un jeton push et le transmettre au serveur 3CA.

*(Ce fichier n'est pas un secret : il est embarqué dans chaque APK distribuée.
La clé `AIzaSy…` qu'il contient est une clé cliente Android, restreinte à ce
package. Il est normal de la versionner.)*

## ⏳ Reste à faire — côté serveur 3CA_COMMERCIAL

Sans cette étape, l'application **peut appeler** le centre d'appel mais **ne peut
pas être rappelée** : le bouton « Rappeler » affichera un message invitant
l'opérateur à composer le numéro du requérant.

1. Console Firebase → ⚙️ **Paramètres du projet** → onglet **Comptes de service**
   → **Générer une nouvelle clé privée**. Un fichier JSON est téléchargé.

   ⚠️ **Celui-là est un vrai secret** : il permet d'envoyer des notifications au
   nom du projet. Il ne doit jamais être commité ni partagé.

2. Le déposer sur le serveur CTI, hors du dépôt :

   ```
   /var/www/3CA_COMMERCIAL/storage/app/firebase/service-account.json
   ```

   (le dossier existe déjà et est ignoré par git)

3. Vérifier `/var/www/3CA_COMMERCIAL/.env` :

   ```
   FCM_PROJECT_ID=rappel-requerent
   FCM_CREDENTIALS=/var/www/3CA_COMMERCIAL/storage/app/firebase/service-account.json
   ```

4. Vider le cache : `php artisan config:clear`

## Vérifier

```bash
cd /var/www/3CA_COMMERCIAL
php artisan tinker --execute="echo app(App\Services\MobileApp\FcmService::class)->isConfigured() ? 'FCM OK' : 'FCM non configure';"
```
