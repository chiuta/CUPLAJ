# CUPLAJ — Guide d'utilisation

[English](GUIDE.md) · [Română](GUIDE.ro.md) · **Français** · [Italiano](GUIDE.it.md) · [Español](GUIDE.es.md) · [Português](GUIDE.pt.md) · [Deutsch](GUIDE.de.md) · [Magyar](GUIDE.hu.md)

_Version : 1.0.0 · Dernière mise à jour : 2026-11-24_

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

---

[← README](../README.fr.md)
