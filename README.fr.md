# CUPLAJ

[English](README.md) · [Română](README.ro.md) · **Français** · [Italiano](README.it.md) · [Español](README.es.md) · [Português](README.pt.md) · [Deutsch](README.de.md) · [Magyar](README.hu.md)

<p align="center"><img src="docs/logo.svg" width="160" alt="CUPLAJ"></p>

> **Téléphonie à ligne partagée pour le théâtre immersif**

_Version : 1.8.0 · Dernière mise à jour : 2026-11-24_

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
- **Internet et données mobiles (1.1)** — connexion par `hôte` ou `hôte:port` ; clé réseau AES‑128‑GCM facultative ; compression G.711 automatique hors réseau local ; keepalive.
- **Appels directs P2P (1.1)** — l'audio circule de téléphone à téléphone en UDP avec traversée de NAT, avec repli sur le relais du hub ; la régie peut le désactiver, le limiter au réseau local (défaut) ou l'ouvrir partout.
- **Passerelle VoIP / SIP (1.1)** — le téléphone de la régie s'enregistre chez un opérateur SIP ou un PBX ; l'équipe compose `0`+numéro pour sortir, les appels SIP entrants sonnent à un numéro interne.
- **Réseau mobile (1.1)** — passerelle SMS via la SIM du téléphone de la régie (envoi et réception), cue *appel réel* qui ouvre le numéroteur, indicateur Wi‑Fi / mobile.
- **Davantage de modes de connexion (1.2)** — Bluetooth (sans routeur, environ 7 téléphones), Wi‑Fi Direct (le téléphone de la régie crée son propre réseau Wi‑Fi) et découverte mDNS, en plus du Wi‑Fi, du point d'accès, du partage USB, de l'Ethernet et des données mobiles.
- **Plug and play (1.3)** — le téléphone de la régie ouvre seul toutes les liaisons utiles ; les autres touchent *Trouver et rejoindre automatiquement* ou un lien d'invitation. Ajoute Wi‑Fi Aware.
- **Durci et sans réseau (1.4)** — audit face à un public hostile avec corrections, et CUPLAJ‑AIR : trois protocoles originaux qui transportent indices et invitations sans aucun réseau (lampe → caméra, haut‑parleur → micro, vibration → accéléromètre).
- **Régie de spectacle (1.5)** — indices déclenchés au même instant, indice PANIQUE, couleur plein écran, sondages du public, confirmation de livraison et touche PTT matérielle. Comparé à Unity Intercom, Zello, Augmented Theatre, QLab/SOLOstudio et Livecube dans `docs/COMPARISON.md`.
- **Tout ce que la comparaison montrait et qui tient dans une appli Android hors ligne (1.6)** — priorité PA (ducking), OSC en entrée et sortie pour pupitres lumière et son, et indices image, vidéo et clip audio envoyés via le hub.
- **Spectacle intelligent (1.7)** — zones qui suivent le public par le son, règles (« quand quelqu'un entre en zone 3, envoyez‑lui… »), compteur de public, effets de voix, sous‑titres, texte multilingue et personnalisé, ramification de l'histoire.
- **Outils de scène (1.8)** — lumières Art‑Net/DMX, MIDI, vague de signaux, téléphone arabe, secret fragmenté, magnétophone fantôme, signal de sécurité (lumière de salle), Morse par vibrations, rapport de fin de spectacle.

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

La logique du hub (appels, ligne partagée, permissions, limites, persistance) est couverte par ~55 vérifications automatiques exécutées sur une JVM. L'interface Android et le chemin audio ont passé le contrôle de types face au SDK Android 14 mais **n'ont pas encore été testés sur de vrais appareils** ; l'APK est construit par GitHub Actions. Testez avec quelques téléphones avant un spectacle. Le trafic du réseau local n'est pas chiffré (le PIN et le code de salle écartent les curieux de bonne foi, ce n'est pas de la cryptographie). Le réseau de la version 1.1 (chiffrement, P2P, SIP, SMS) est couvert par des tests du hub contre un serveur SIP simulé, mais **n'a pas** été essayé sur de vrais opérateurs, NAT ou cartes SIM. Les liaisons Bluetooth, Wi‑Fi Direct et mDNS (1.2) partagent la même couche de transport, testée sur JVM avec des canaux simulés, mais **n'ont pas** été essayées sur de vrais appareils ; le comportement varie selon les fabricants. Le plug and play (connexion auto, invitations, Wi‑Fi Aware, 1.3) **n'a pas** été testé sur appareils ; le format des invitations est couvert par des tests JVM. Les corrections de l'audit 1.4 et les codecs AIR sont couverts par des tests JVM (bruit simulé, dérive d'horloge, mauvaise clé, interférence) ; le code de la lampe, caméra, micro, haut‑parleur, moteur et accéléromètre **n'est pas** testé sur de vrais téléphones, et la portée et la vitesse réelles différeront. Les fonctions 1.5 sont couvertes par des tests JVM (estimation d'horloge, délais, règles du sondage, limites photosensibles) ; les surimpressions d'écran, le bouton du casque et la précision réelle de l'horloge **ne sont pas** testés sur appareils. Les fonctions 1.6 sont couvertes par des tests JVM (courbe de ducking, fuzzing et inondation OSC, intégrité des transferts et sûreté des chemins, relais du hub) ; la lecture des images, vidéos et clips, le sélecteur de fichiers et les boîtes de réglages **ne sont pas** testés sur appareils. Zones, règles, compteur, effets de voix et langues sont couverts par des tests JVM (moteur de règles, règles du hub, agrégation du compteur, filtres de voix mesurés en fréquence) ; les vraies balises sonores en intérieur, les mesures micro et accéléromètre et la qualité de voix sur téléphones **ne sont pas** testées sur appareils. Une balise enregistrée peut être rejouée une fois par numéro de séquence : les zones servent à l'ambiance, pas à la sécurité. Paquets Art‑Net, fondus DMX, messages MIDI, motifs Morse, rapport et logique du hub (vague, téléphone arabe, secret, rapport) sont couverts par des tests JVM ; le magnétophone fantôme, les vraies consoles et appareils MIDI et tout ce qui touche de vrais téléphones ne sont pas testés.

## Compilation

Nécessite JDK 17. `./gradlew assembleRelease` produit `app/build/outputs/apk/release/app-release.apk` (signé avec la clé de débogage, installable directement). Tests unitaires : `./gradlew test`. Le workflow de `.github/workflows` fait les deux, avec des outils Node ≥ 24 sur Ubuntu 26.04.

## Confidentialité

Aucune statistique, aucun serveur, aucun compte. Le son et les messages ne circulent que dans votre réseau local, entre les téléphones et le hub. Le hub conserve numéros, groupes et couplages sur le téléphone de la régie pour qu'un redémarrage ne les perde pas.

## Soutenir le projet

CUPLAJ est gratuit et trade‑free. S'il aide votre spectacle, vous pouvez offrir un café à l'auteur : <https://buymeacoffee.com/echo.of.the.strings>.

## Licence

MIT © Alexandru‑Ionuț Chiuță (Alexio).
