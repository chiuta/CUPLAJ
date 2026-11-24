# CUPLAJ

[English](README.md) · [Română](README.ro.md) · [Français](README.fr.md) · **Italiano** · [Español](README.es.md) · [Português](README.pt.md) · [Deutsch](README.de.md) · [Magyar](README.hu.md)

<p align="center"><img src="docs/logo.svg" width="160" alt="CUPLAJ"></p>

> **Telefonia a linea condivisa per il teatro immersivo**

_Versione: 1.2.0 · Ultimo aggiornamento: 2026-11-24_

CUPLAJ trasforma una flotta di normali telefoni Android in una centralina telefonica privata per il teatro immersivo. Organizzatori e attori parlano con gli spettatori (e tra loro) tramite il Wi‑Fi locale del luogo — senza cloud, senza account, senza pubblicità. La sua funzione distintiva è il *cuplaj*: la **linea condivisa** dei telefoni fissi dagli anni '80 fino al 2006 circa, in cui due abbonati dividevano la stessa linea, squillavano con cadenze diverse e potevano ascoltarsi a vicenda.

## Funzioni

- **Tre epoche di telefono** — 1985 disco rotante (impulsi udibili), 1995 tastiera (toni DTMF), 2003 cellulare (SMS, 160 caratteri). La regia può cambiare epoca in diretta.
- **Linea condivisa (cuplaj)** — due telefoni dividono una linea; una chiamata a uno fa squillare entrambi con la cadenza dell'abbonato chiamato; il vicino che risponde per primo prende la chiamata; alzando la cornetta durante la conversazione del vicino si ascolta, e si può persino intervenire.
- **Nessuna infrastruttura** — un telefono (la regia) è l'hub. Gli altri entrano via Wi‑Fi o tramite il suo hotspot; gli hub si trovano da soli; codice sala e PIN facoltativi.
- **Voce** — vere chiamate (8 kHz), annuncio push‑to‑talk dalla regia, canale backstage per la squadra.
- **Segnali** — testo a schermo intero, vibrazione, squillo, flash, altoparlante, cambio di epoca, nuovo contatto, voce parlata (sintesi locale), effetti sonori, schermo nero, chiamata fantasma.
- **Copione** — elenco di segnali con GO e PLAY (a tempo), riordino, import/export JSON.
- **Strumenti di regia** — numeri, gruppi, accoppiamento/disaccoppiamento, coppie automatiche, ascolto discreto, muto, dimentica, livelli batteria, registro eventi, stato che sopravvive al riavvio, limitazione del flusso.
- **8 lingue e accessibilità** — inglese, rumeno, francese, italiano, spagnolo, portoghese, tedesco, ungherese; dimensione del testo regolabile; risposta dalla schermata di blocco; funziona in background.
- **Internet e dati mobili (1.1)** — connessione con `host` o `host:porta`; chiave di rete AES‑128‑GCM opzionale; compressione G.711 automatica fuori dalla rete locale; keepalive.
- **Chiamate dirette P2P (1.1)** — l'audio va da telefono a telefono via UDP con hole punching, con ripiego sul relay dell'hub; la regia può disattivarlo, limitarlo alla rete locale (predefinito) o aprirlo ovunque.
- **Gateway VoIP / SIP (1.1)** — il telefono della regia si registra presso un provider SIP o un PBX; la squadra compone `0`+numero per uscire, le chiamate SIP in arrivo squillano a un numero interno.
- **Rete mobile (1.1)** — gateway SMS tramite la SIM del telefono della regia (invio e ricezione), cue *chiamata reale* che apre il tastierino, indicatore Wi‑Fi / mobile.
- **Più modi per collegarsi (1.2)** — Bluetooth (senza router, circa 7 telefoni), Wi‑Fi Direct (il telefono della regia crea una propria rete Wi‑Fi) e scoperta mDNS, oltre a Wi‑Fi, hotspot, tethering USB, Ethernet e dati mobili.

## Avvio rapido

1. Scarica l'APK dalla scheda *Actions* (artefatto `CUPLAJ-apk`) o dalla pagina *Releases* e installalo su ogni telefono.
2. Sul telefono della regia attiva l'hotspot (oppure collega tutti i telefoni allo stesso Wi‑Fi).
3. Sul telefono della regia scegli **Regia** → *Avvia l'hub*. Annota il PIN per gli attori e, se vuoi, un codice sala.
4. Sugli altri scegli **Spettatore** o **Attore**, seleziona l'hub dall'elenco (o scrivi il suo indirizzo) e connettiti.
5. Ogni telefono riceve un numero (regia 100, attori 201…, spettatori 6 cifre). Alza la cornetta e chiama.

## Le tre epoche

- **1985 · disco** — un vero disco da far girare; gli impulsi si sentono; due squilli lunghi.
- **1995 · tasti** — toni DTMF, rubrica di numeri, messaggi del cercapersone.
- **2003 · cellulare** — nomi in rubrica, identificazione del chiamante, SMS limitati a 160 caratteri.

## Come funziona la linea condivisa

<p align="center"><img src="docs/partyline.svg" width="620" alt="party line"></p>

1. Due telefoni sono *accoppiati* dalla regia (o automaticamente a coppie). Condividono la linea N, posizioni 1 e 2.
2. Una chiamata all'abbonato 1 fa squillare entrambi; l'abbonato 1 sente la cadenza del proprio numero (uno squillo lungo), l'abbonato 2 la sua (due brevi).
3. Chi risponde prende la chiamata; l'altro telefono smette di squillare.
4. Finché la linea è in uso, il vicino che alza la cornetta sente la conversazione e gli interlocutori sentono un leggero clic.
5. Chi ascolta può *intervenire* e parlare; una chiamata a un telefono accoppiato la cui linea è occupata riceve il segnale di occupato.
6. La regia può monitorare qualsiasi linea in silenzio (senza clic).

## Documentazione

- [Guida all'uso](docs/GUIDE.it.md)
- [Protocollo](docs/PROTOCOL.md) *(English)*
- [Audit avversariale](docs/AUDIT.md) *(English)*
- [Registro delle modifiche](CHANGELOG.md) *(English)*
- [Sito di presentazione](https://chiuta.github.io/CUPLAJ/)

## Stato reale

La logica dell'hub (chiamate, linea condivisa, permessi, limiti, persistenza) è coperta da ~55 verifiche automatiche eseguite su una JVM. L'interfaccia Android e il percorso audio hanno superato il controllo dei tipi rispetto all'SDK Android 14 ma **non sono ancora stati provati su dispositivi reali**; l'APK è costruito da GitHub Actions. Prova con qualche telefono prima dello spettacolo. Il traffico sulla rete locale non è cifrato (PIN e codice sala tengono fuori gli estranei in buona fede, non sono crittografia). La rete della versione 1.1 (cifratura, P2P, SIP, SMS) è coperta da test dell'hub contro un server SIP simulato, ma **non** è stata provata su operatori, NAT o SIM reali. I collegamenti Bluetooth, Wi‑Fi Direct e mDNS (1.2) usano lo stesso livello di trasporto, testato su JVM con canali simulati, ma **non** sono stati provati su dispositivi reali; il comportamento varia tra i produttori.

## Compilazione

Richiede JDK 17. `./gradlew assembleRelease` produce `app/build/outputs/apk/release/app-release.apk` (firmato con la chiave di debug, installabile direttamente). Test unitari: `./gradlew test`. Il workflow in `.github/workflows` fa entrambe le cose, con strumenti Node ≥ 24 su Ubuntu 26.04.

## Privacy

Nessuna statistica, nessun server, nessun account. Audio e messaggi viaggiano solo nella vostra rete locale, tra i telefoni e l'hub. L'hub conserva numeri, gruppi e accoppiamenti sul telefono della regia, così un riavvio non li perde.

## Sostieni il progetto

CUPLAJ è gratuito e trade‑free. Se aiuta il tuo spettacolo, puoi offrire un caffè all'autore: <https://buymeacoffee.com/echo.of.the.strings>.

## Licenza

MIT © Alexandru‑Ionuț Chiuță (Alexio).
