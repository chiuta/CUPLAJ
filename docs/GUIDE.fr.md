# CUPLAJ — Guide d'utilisation

[English](GUIDE.md) · [Română](GUIDE.ro.md) · **Français** · [Italiano](GUIDE.it.md) · [Español](GUIDE.es.md) · [Português](GUIDE.pt.md) · [Deutsch](GUIDE.de.md) · [Magyar](GUIDE.hu.md)

_Version : 1.1.0 · Dernière mise à jour : 2026-11-24_

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

---

[← README](../README.fr.md)
