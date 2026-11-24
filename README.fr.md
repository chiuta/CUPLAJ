# CUPLAJ

[English](README.md) · [Română](README.ro.md) · **Français** · [Italiano](README.it.md) · [Español](README.es.md) · [Português](README.pt.md) · [Deutsch](README.de.md) · [Magyar](README.hu.md)

<p align="center"><img src="docs/logo.svg" width="160" alt="CUPLAJ"></p>

> **Téléphonie à ligne partagée pour le théâtre immersif**

_Version : 1.0.0 · Dernière mise à jour : 2026-11-24_

CUPLAJ transforme une flotte de téléphones Android ordinaires en central téléphonique privé pour le théâtre immersif. Organisateurs et acteurs parlent aux spectateurs (et entre eux) via le Wi‑Fi local du lieu — sans cloud, sans compte, sans publicité. Sa fonction phare est le *cuplaj* : la **ligne partagée** des téléphones fixes des années 1980 jusqu'à environ 2006, où deux abonnés se partageaient une même ligne, sonnaient avec des cadences différentes et pouvaient s'entendre l'un l'autre.

## Fonctionnalités

- **Trois époques de téléphone** — 1985 cadran rotatif (impulsions audibles), 1995 clavier (tonalités DTMF), 2003 mobile (SMS, 160 caractères). La régie peut changer d'époque en direct.
- **Ligne partagée (cuplaj)** — deux téléphones se partagent une ligne ; un appel à l'un fait sonner les deux avec la cadence de l'abonné appelé ; le voisin qui décroche le premier prend l'appel ; décrocher pendant la conversation du voisin permet d'écouter, voire d'intervenir.
- **Aucune infrastructure** — un téléphone (la régie) est le hub. Les autres le rejoignent par Wi‑Fi ou par son point d'accès ; les hubs sont détectés automatiquement ; code de salle et PIN facultatifs.
- **Voix** — vrais appels (8 kHz), annonce push‑to‑talk depuis la régie, canal coulisses pour l'équipe.
- **Indices** — texte plein écran, vibration, sonnerie, flash, haut‑parleur, changement d'époque, nouveau contact, voix parlée (synthèse locale), effets sonores, écran noir, appel fantôme.
- **Scénario** — liste d'indices avec GO et PLAY (minuté), réorganisation, import/export JSON.
- **Outils de régie** — numéros, groupes, couplage/découplage, paires automatiques, écoute discrète, muet, oubli, niveaux de batterie, journal, état conservé après redémarrage, limitation de débit.
- **8 langues et accessibilité** — anglais, roumain, français, italien, espagnol, portugais, allemand, hongrois ; taille de texte réglable ; réponse depuis l'écran verrouillé ; fonctionne en arrière‑plan.

## Démarrage rapide

1. Récupérez l'APK dans l'onglet *Actions* (artefact `CUPLAJ-apk`) ou la page *Releases* et installez‑le sur chaque téléphone.
2. Sur le téléphone de la régie, activez le point d'accès (ou mettez tous les téléphones sur le même Wi‑Fi).
3. Sur le téléphone de la régie, choisissez **Régie** → *Démarrer le hub*. Notez le PIN des acteurs et, si besoin, un code de salle.
4. Sur les autres, choisissez **Spectateur** ou **Acteur**, sélectionnez le hub dans la liste (ou saisissez son adresse) et connectez‑vous.
5. Chaque téléphone reçoit un numéro (régie 100, acteurs 201…, spectateurs 6 chiffres). Décrochez et appelez.

## Les trois époques

- **1985 · cadran** — un vrai cadran qu'on tourne ; les impulsions s'entendent ; deux longues sonneries.
- **1995 · touches** — tonalités DTMF, carnet de numéros, messages de bipeur.
- **2003 · mobile** — noms dans le répertoire, identification de l'appelant, SMS limités à 160 caractères.

## Comment fonctionne la ligne partagée

<p align="center"><img src="docs/partyline.svg" width="620" alt="party line"></p>

1. Deux téléphones sont *couplés* par la régie (ou automatiquement par paires). Ils partagent la ligne N, positions 1 et 2.
2. Un appel à l'abonné 1 fait sonner les deux ; l'abonné 1 entend la cadence de son numéro (une longue sonnerie), l'abonné 2 celle du sien (deux courtes).
3. Celui qui décroche prend l'appel ; l'autre téléphone cesse de sonner.
4. Tant que la ligne est utilisée, le voisin qui décroche entend la conversation et les interlocuteurs entendent un léger clic.
5. Un auditeur peut *intervenir* et parler ; un appel vers un téléphone couplé dont la ligne est occupée reçoit la tonalité d'occupation.
6. La régie peut surveiller n'importe quelle ligne discrètement (sans clic).

## Documentation

- [Guide d'utilisation](docs/GUIDE.fr.md)
- [Protocole](docs/PROTOCOL.md) *(English)*
- [Audit adversarial](docs/AUDIT.md) *(English)*
- [Journal des modifications](CHANGELOG.md) *(English)*
- [Site de présentation](https://chiuta.github.io/CUPLAJ/)

## État réel

La logique du hub (appels, ligne partagée, permissions, limites, persistance) est couverte par ~55 vérifications automatiques exécutées sur une JVM. L'interface Android et le chemin audio ont passé le contrôle de types face au SDK Android 14 mais **n'ont pas encore été testés sur de vrais appareils** ; l'APK est construit par GitHub Actions. Testez avec quelques téléphones avant un spectacle. Le trafic du réseau local n'est pas chiffré (le PIN et le code de salle écartent les curieux de bonne foi, ce n'est pas de la cryptographie).

## Compilation

Nécessite JDK 17. `./gradlew assembleRelease` produit `app/build/outputs/apk/release/app-release.apk` (signé avec la clé de débogage, installable directement). Tests unitaires : `./gradlew test`. Le workflow de `.github/workflows` fait les deux, avec des outils Node ≥ 24 sur Ubuntu 26.04.

## Confidentialité

Aucune statistique, aucun serveur, aucun compte. Le son et les messages ne circulent que dans votre réseau local, entre les téléphones et le hub. Le hub conserve numéros, groupes et couplages sur le téléphone de la régie pour qu'un redémarrage ne les perde pas.

## Soutenir le projet

CUPLAJ est gratuit et trade‑free. S'il aide votre spectacle, vous pouvez offrir un café à l'auteur : <https://buymeacoffee.com/echo.of.the.strings>.

## Licence

MIT © Alexandru‑Ionuț Chiuță (Alexio).
