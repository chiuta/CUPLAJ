# CUPLAJ — Guide d'utilisation

[English](GUIDE.md) · [Română](GUIDE.ro.md) · **Français** · [Italiano](GUIDE.it.md) · [Español](GUIDE.es.md) · [Português](GUIDE.pt.md) · [Deutsch](GUIDE.de.md) · [Magyar](GUIDE.hu.md)

_Version : 1.8.0 · Dernière mise à jour : 2026-11-24_

## 1. Avant le spectacle

- Utilisez un seul réseau Wi‑Fi pour tous les téléphones, ou le point d'accès du téléphone de la régie. Désactivez l'*isolation des clients* sur les routeurs.
- Installez l'APK, accordez les permissions microphone et notifications, et dans le menu **Aa** choisissez *Garder l'app active* pour que le système ne la mette pas en veille.
- Chargez tous les téléphones ; la régie affiche les niveaux de batterie et alerte sous 20 %.

## 2. Rôles et numéros

- **Régie** — le hub ; voit tout, envoie des indices, couple les téléphones. Numéro 100.
- **Acteur** — a besoin du PIN ; peut parler à l'équipe et appeler n'importe qui. Numéros à partir de 201.
- **Spectateur** — reçoit appels, messages et indices ; ne peut parler qu'à l'équipe. Numéros à six chiffres.
- La régie peut changer tout numéro et attribuer des groupes (par exemple *balcon*, *cave*).

## 3. Utiliser le téléphone

- **1985** — décrochez (bouton), attendez la tonalité, tournez le cadran ; le numéro se compose seul après une courte pause.
- **1995** — décrochez puis utilisez le clavier ; le carnet liste les numéros ajoutés par la régie.
- **2003** — saisissez le numéro et appuyez sur *Appeler* ; les messages sont limités à 160 caractères.
- Un téléphone qui sonne peut être décroché depuis la notification, même sur l'écran verrouillé.

## 4. La ligne partagée en pratique

- Sélectionnez deux appareils dans l'onglet Régie et appuyez sur *Coupler 2*, ou sur *Paires auto* pour apparier tous les téléphones libres.
- Utilisez *Écouter* sur la régie pour surveiller une ligne sans être entendu.
- Des options permettent de couper l'écoute du voisin, le clic ou les messages entre téléphones.

## 5. Indices

- Choisissez une cible (tous, spectateurs, acteurs, appareils sélectionnés, un groupe, un numéro), un type et un texte, puis *Envoyer l'indice*.
- Exemples : `Maman;204455` ajoute un contact ; `1;Il est presque minuit` met l'écran en noir avec un texte discret ; `Dr Voss;204455;Venez à la cave` déclenche un appel fantôme qui parle quand on décroche.

## 6. Scénario

- Construisez le spectacle en étapes avec cible, type, texte et délai. **GO** envoie l'étape suivante ; **PLAY** déroule la suite automatiquement à partir de l'étape sélectionnée.
- Exportez le scénario en JSON et importez‑le sur un autre téléphone de régie.

## 7. Dépannage

- *Aucun hub trouvé* — vérifiez le même réseau, désactivez l'isolation des clients ou saisissez l'adresse du hub.
- *PIN / code de salle incorrect* — la régie les affiche sur son écran de démarrage.
- *Port occupé* — fermez toute autre application utilisant le port 8470.
- *Appels muets* — vérifiez la permission du microphone et le bouton haut‑parleur de l'onglet Voix.

## 8. Internet, VoIP et réseau mobile (1.1)

- **Clé réseau** — sur l'écran de départ, activez *Liaison chiffrée* ; la régie affiche une clé (xxxx‑xxxx‑xxxx‑xxxx). Saisissez‑la dans le champ *Clé réseau* de chaque autre téléphone. Sans la bonne clé, le hub refuse la connexion. À utiliser dès que le trafic quitte le réseau local.
- **Depuis Internet** — redirigez TCP 8470 et UDP 8472 sur le routeur vers le téléphone de la régie et indiquez l'adresse publique (`hôte` ou `hôte:port`). En données mobiles, rien d'autre n'est nécessaire.
- **P2P** — dans l'onglet Régie, l'option *Appels directs* alterne désactivés / réseau local seulement / partout. *Partout* permet à deux téléphones de connaître leur IP publique mutuelle ; la régie ne peut ni écouter, ni couper, ni s'insérer tant que l'appel est direct (retour automatique au relais si nécessaire).
- **Ligne SIP** — Réglages → *Ligne VoIP / SIP…* : serveur, utilisateur, mot de passe, domaine facultatif et numéro interne pour les appels entrants (100 par défaut). Redémarrez le hub pour appliquer. L'équipe compose `0` + numéro externe ; les spectateurs ne peuvent pas appeler à l'extérieur. Chaque appel est limité à 15 minutes. SIP et RTP ne sont pas chiffrés.
- **SMS** — Réglages → *Passerelle SMS* (SIM et permissions SMS requises). L'équipe choisit *SMS vers un vrai numéro* dans l'onglet Messages ; les SMS reçus apparaissent comme messages de *SMS*. Limite : 20 par minute. Les tarifs de l'opérateur s'appliquent.
- **Appel réel** — le cue *appel réel* ouvre le numéroteur avec le numéro. Android interdit à une application d'injecter un appel cellulaire dans la ligne partagée : l'audio GSM ne peut pas être relayé ; utilisez la ligne SIP pour cela.

## 9. Se connecter sans routeur (1.2)

- **Même hub, n'importe quelle liaison** — appels, ligne partagée, cues, chiffrement et PIN fonctionnent de la même façon ; seul le chemin du téléphone vers le hub change.
- **Point d'accès, partage USB, Ethernet** — toute liaison qui donne un réseau IP aux téléphones fonctionne sans réglage ; utilisez l'adresse ou laissez le hub être trouvé.
- **Bluetooth** — sur le téléphone de la régie, démarrez le hub avec *Liaison Bluetooth : OUI* et appuyez sur *Rendre visible par Bluetooth*. Sur les autres, appuyez sur *Chercher par Bluetooth* et choisissez le téléphone de la régie (🔵). La portée est courte et environ 7 téléphones tiennent sur un hub ; la voix est compressée automatiquement. L'appairage n'est pas obligatoire ; l'accès reste protégé par le PIN, le code de salle et la clé réseau.
- **Wi‑Fi Direct** — sur le téléphone de la régie, démarrez le hub avec *Groupe Wi‑Fi Direct : OUI*. Sur les autres, appuyez sur *Chercher Wi‑Fi Direct* et choisissez le téléphone de la régie (📡) ; il peut demander d'accepter chaque téléphone. Après la jonction, le hub apparaît dans la liste. Les téléphones ne peuvent généralement pas rester sur leur Wi‑Fi habituel dans un groupe Wi‑Fi Direct, et certains modèles ne gèrent pas les deux à la fois.
- **mDNS** — le hub s'annonce aussi par mDNS/DNS‑SD (🌐), ce qui le rend visible sur les réseaux qui bloquent la diffusion habituelle.
- **Non inclus** — Wi‑Fi Aware (peu de téléphones) et Nearby Connections de Google (nécessite Google Play Services, que cette application trade‑free évite). Le Bluetooth Low Energy est trop lent pour la voix.

## 10. Plug and play (1.3)

- **Téléphone de la régie** — choisissez *Régie*, puis *Démarrer le hub*. Avec *Plug & play* activé (par défaut), CUPLAJ ouvre Wi‑Fi / point d'accès, mDNS, Bluetooth et Wi‑Fi Aware, et crée un groupe Wi‑Fi Direct s'il n'y a pas de réseau Wi‑Fi. Acceptez la demande de permissions unique. Les réglages montrent les liaisons actives.
- **Autres téléphones, le plus simple** — Réglages → *Partager l'invitation pour spectateurs* (ou *acteurs*) sur le téléphone de la régie ; envoyez le lien par n'importe quelle appli. Quiconque le touche avec CUPLAJ installé est configuré (adresse, clé, PIN, salle) et connecté.
- **Autres téléphones, sans lien** — choisissez un rôle et touchez *Trouver et rejoindre automatiquement*. CUPLAJ cherche sur toutes les liaisons à la fois (Wi‑Fi, mDNS, Bluetooth, Wi‑Fi Direct, Wi‑Fi Aware) et se connecte au meilleur hub : réseau local d'abord, Bluetooth en dernier.
- **Wi‑Fi Aware** — les téléphones compatibles se trouvent sans routeur ni réglage. Beaucoup ne le gèrent pas ; rien ne casse si le vôtre non.
- **Sécurité** — une invitation ne peut créer qu'un spectateur ou un acteur, jamais un téléphone de régie. Les liens contiennent la clé et le PIN ; ne les envoyez qu'aux personnes invitées.

## 11. Durcissement + AIR (1.4)

- **Hub plus sûr** — PIN aléatoire à 6 chiffres, clé nouvelle à chaque connexion, limites sur les connexions non authentifiées et la taille des messages, blocage après des PIN erronés répétés, et confirmation avant d'appliquer un lien d'invitation.
- **LUMEN (lumière)** — le téléphone de régie fait clignoter sa lampe ; un autre téléphone observe avec sa caméra arrière. Pièce tamisée, 1–3 m, lampe dirigée vers la caméra. Lent (quelques secondes) mais robuste.
- **SONUS (son)** — le haut‑parleur joue un code de tons, quasi ultrasonore (inaudible pour la plupart des adultes) ou audible ; l'autre téléphone écoute avec son micro. Transporte des indices et une invitation au hub (adresse et PIN).
- **TACTUS (vibration)** — le moteur de vibration envoie des impulsions courtes et longues ; un autre téléphone posé sur la même surface les sent avec son accéléromètre. Fonctionne dans le silence et le noir, pour de courts indices.
- **Utilisation** — Réglages → *Liaisons par l'air* : activez les écoutes voulues (caméra / micro / accéléromètre ; le téléphone demande la permission), puis sur le téléphone de régie appuyez sur un bouton d'envoi. Les paquets portent une courte étiquette d'authentification dérivée de la clé réseau et ne peuvent pas être rejoués.

## 12. Régie de spectacle (1.5)

- **Même instant** — commencez le texte par `@3.5;` pour le déclencher dans 3,5 s sur tous les téléphones à la fois. Les téléphones synchronisent leur horloge avec le hub à la connexion puis chaque minute ; un indice en retard de plus de 10 s est abandonné.
- **PANIQUE** — l'indice *PANIQUE : tout arrêter* coupe sonnerie, vibration, voix, effets sonores, écrans noirs et colorés, faux appels et indices programmés. Fonctionne aussi par AIR.
- **Couleur** — `#RRGGBB;secondes;pulsations par seconde` remplit l'écran. Les pulsations sont plafonnées à 2 par seconde pour les spectateurs photosensibles ; un appui long ferme l'écran.
- **Sondage** — `question;option 1;option 2;…` interroge spectateurs et acteurs ; un vote par téléphone (modifiable). *Clore le sondage* montre le résultat à la régie. Les sondages peuvent être des étapes du scénario.
- **Livraison** — après chaque indice, la régie voit *envoyé à N téléphones sur M en ligne*. **PTT matériel** — Réglages → *Touche PTT matérielle* transforme le bouton du casque et volume + en push‑to‑talk quand l'appli est au premier plan.

## 13. Médias, OSC, priorité (1.6)

- **Priorité PA** — quand le téléphone de régie (ou un acteur) parle à tous, tous les autres sons descendent à 30 % puis reviennent en douceur. Réglages → *Priorité PA* la désactive.
- **Indices média** — sur le téléphone de régie, onglet Indices → *Envoyer un fichier…* choisit une image, une vidéo ou un audio (jusqu'à 8 Mo) et l'envoie par morceaux aux téléphones choisis ; chacun vérifie la taille et la somme de contrôle et ignore un fichier abîmé. Utilisez ensuite *Image à l'écran*, *Vidéo à l'écran* ou *Clip audio* (texte vide = dernier fichier, ou `code;secondes`). Un appui long ferme l'image ou la vidéo. Les téléphones arrivés plus tard n'ont pas le fichier : renvoyez‑le.
- **OSC sortie** — Réglages → *OSC* : chaque indice est aussi envoyé en `/cuplaj/cue <vers> <type> <texte>` par UDP à un pupitre lumière ou son, QLab ou Companion.
- **OSC entrée** — le pupitre peut envoyer `/cuplaj/<PIN du hub>/go`, `/panic`, `/stop` ou `/cue <vers> <type> <texte>` à la régie (port 8473 par défaut). Le PIN dans l'adresse est la seule protection : gardez le réseau du spectacle privé. 20 messages par seconde au plus.
- **Non inclus** — iOS (Android seulement), flux programme et plus de canaux que les groupes existants.

## 14. Spectacle intelligent (1.7)

- **Zones** — posez un téléphone dans une pièce (ou dans la poche d'un personnage), Réglages → *Liaisons par l'air* → *Balise de zone* et touchez jusqu'au numéro 1–9 : il émet toutes les 7 s un code quasi ultrasonore, inaudible pour la plupart. Les téléphones du public avec *Écouter : SONUS* signalent la zone entendue ; la régie voit 📍 près du nom. 30 s de silence = ils sont partis.
- **Règles** — téléphone de régie, Réglages → *Règles*, une par ligne : `zone 3 once > self | text | Bienvenue, {name}`, `pollwin 1 > all | color | #00ff00;3`, `meter noise 70 > all | blackout | 1;`, `pollwin 2 > all | goto | Fin B`. *self* est la personne qui a déclenché la règle ; `goto` saute à une étape du scénario par nom ou numéro : un sondage peut donc choisir l'histoire.
- **Compteur de public** — onglet Indices : *Mesurer le bruit* ou *le mouvement* 5 s sur les téléphones choisis. Chacun n'envoie qu'un nombre 0–100, jamais de son. Vous voyez moyenne, pic et nombre de réponses ; les règles peuvent réagir.
- **Effets de voix** — choisissez des téléphones dans la liste de la régie, *Effet de voix…* : robot, vieille radio, écho, grave, aigu, lo‑fi. L'effet est appliqué sur ce téléphone avant l'envoi : tous les auditeurs entendent la même voix.
- **Sous‑titres et langues** — l'indice *Sous‑titre* affiche de grands sous‑titres ; tout indice texte peut porter `en=Hello|ro=Salut|fr=Bonjour` et chaque téléphone prend sa langue (puis l'anglais, puis le premier). `{name}` et `{number}` deviennent le nom et le numéro du téléphone récepteur.

## 15. Outils de scène (1.8)

- **Lumières et musique** — les signaux *dmx* (`0;1=255,2-4=128;2000`) et *midi* (`note 60 127`, `cc 7 100`, `pc 5`, `mmc play`) s'exécutent sur le téléphone de la régie : Réglages → *Sortie Art‑Net / DMX* envoie de l'Art‑Net (UDP 6454) vers un nœud ou une console sur le même Wi‑Fi ; le MIDI va au premier appareil MIDI USB/Bluetooth trouvé.
- **Vague** — commencez le texte d'un signal par `~150;` : le hub l'envoie aux téléphones l'un après l'autre, dans l'ordre des numéros, à 150 ms d'intervalle.
- **Téléphone arabe** — *Outils → Téléphone arabe* : message et numéros dans l'ordre ; chacun voit ce que le précédent a transmis, peut le changer et le passe. La régie reçoit l'original et toutes les déformations.
- **Secret fragmenté** — une phrase est répartie mot par mot ; qui dit la phrase entière en premier (Réglages → *Dire la phrase secrète*) la résout ; le déclencheur de règle `solved` peut lancer un signal.
- **Magnétophone fantôme** — enregistre jusqu'à 60 s du micro d'un téléphone sur le hub et le rejoue plus tard à un groupe, comme une voix venue de nulle part (source réservée 253).
- **Lumière de salle** — signal *house* : arrête tout (comme PANIQUE), vibre, écran blanc et message calme. **Morse** — signal *morse* : vibre un texte en Morse.
- **Rapport** — *Outils → Rapport* : fichier Markdown avec totaux, appels/messages/entrées de zone/batterie par téléphone et chronologie complète.
- **Limites et sécurité (1.8)** — une vague ne dure jamais plus de 20 s (le pas se réduit dans les grandes salles ; 4 vagues à la fois au plus) ; un téléphone qui ne répond pas en 90 s dans le téléphone arabe est sauté ; un secret fragmenté exige une phrase d'au moins 2 mots et 4 lettres latines ou chiffres (la vérification ignore casse, accents et ponctuation) ; une seule lecture de magnétophone à la fois ; le Morse est coupé à 45 s ; l'Art‑Net n'envoie rien avant le premier signal *dmx* (il ne peut donc pas éteindre un plateau en étant activé) et gère 8 univers au plus ; *dmx* et *midi* ne fonctionnent que sur le téléphone de la régie (signaux et scénarios), pas comme actions de règle.

## 16. Référence des signaux (tous les types)

- **Préfixes** — `@3.5;` déclenche le signal après 3,5 s sur tous les téléphones à la fois ; `~150;` (après `@…;`) l'envoie aux téléphones l'un après l'autre, à 150 ms ; `en=Hello|ro=Salut` choisit la langue de chaque téléphone ; `{name}` et `{number}` sont remplacés sur chaque téléphone.
- **`text`** — Texte à l'écran: Texte affiché en plein écran
- **`vibrate`** — Vibration: Durée en ms (ex. 800)
- **`ring`** — Sonnerie: (aucun texte nécessaire)
- **`flash`** — Flash: Durée en ms (ex. 1500)
- **`speaker`** — Haut-parleur: 1 = activé, 0 = désactivé
- **`contact`** — Ajouter un contact: Nom;numéro (ex. Maman;204455)
- **`era`** — Changer d'époque: 1985, 1995 ou 2003
- **`say`** — Voix parlée: Texte à prononcer
- **`sound`** — Effet sonore: bell, siren, knock, static (cloche, sirène, coups, parasites)
- **`blackout`** — Écran noir: 1;texte ou 0
- **`fakecall`** — Appel fantôme: Nom;numéro;paroles prononcées
- **`realcall`** — Appel réel (ouvre le numéroteur): Numéro à composer, ex. +40721000111
- **`panic`** — PANIQUE : tout arrêter: (sans texte)
- **`color`** — Couleur plein écran: #RRGGBB;secondes;pulsations par seconde (max 2)
- **`poll`** — Sondage (vote du public): question;option 1;option 2;… (2–6 options)
- **`pollend`** — Clore le sondage et afficher le résultat: (sans texte)
- **`image`** — Image à l'écran: code du fichier;secondes (envoyez d'abord le fichier ; vide = dernier)
- **`video`** — Vidéo à l'écran: code du fichier;secondes (envoyez d'abord le fichier ; vide = dernier)
- **`clip`** — Clip audio: code du fichier;secondes (envoyez d'abord le fichier ; vide = dernier)
- **`caption`** — Sous‑titre à l'écran: texte, ou en=Hello|ro=Salut|fr=Bonjour (chaque téléphone affiche sa langue ; {name} = nom du téléphone)
- **`dmx`** — Lumières : Art‑Net/DMX (téléphone de la régie): univers;canal=valeur,plage=valeur;fondu ms — ex. 0;1=255,2-4=128;2000
- **`midi`** — Sortie MIDI (téléphone de la régie): note 60 127 [canal] · off 60 [canal] · cc 7 100 [canal] · pc 5 [canal] · mmc play|stop|record
- **`morse`** — Code Morse par vibrations: texte à transmettre (lettres et chiffres ; ~ 60 caractères)
- **`house`** — Lumière de salle (sécurité : arrêt + écran blanc): (facultatif) message affiché sur l’écran blanc
- **Déclencheurs et actions de règles** — Une règle par ligne : ‹quand› > ‹qui› | ‹type d'indice› | ‹texte›. Quand : `zone 3 [once]`, `pollwin 0` (option gagnante, dès 0), `meter noise|motion 70` (moyenne ≥ 70). Qui : all, staff, spectators, actors, self (celui qui a déclenché), num:/id:/grp:. Le type `goto` saute à une étape du scénario par nom ou numéro. Le texte peut utiliser {name}. Aussi `solved` (un secret fragmenté a été deviné).

---

[← README](../README.fr.md)
