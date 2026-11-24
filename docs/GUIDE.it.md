# CUPLAJ — Guida all'uso

[English](GUIDE.md) · [Română](GUIDE.ro.md) · [Français](GUIDE.fr.md) · **Italiano** · [Español](GUIDE.es.md) · [Português](GUIDE.pt.md) · [Deutsch](GUIDE.de.md) · [Magyar](GUIDE.hu.md)

_Versione: 1.1.0 · Ultimo aggiornamento: 2026-11-24_

## 1. Prima dello spettacolo

- Usa un'unica rete Wi‑Fi per tutti i telefoni, o l'hotspot del telefono della regia. Disattiva l'*isolamento dei client* sui router.
- Installa l'APK, concedi i permessi per microfono e notifiche e, nel menu **Aa**, scegli *Mantieni l'app attiva* perché il sistema non la metta a dormire.
- Carica tutti i telefoni; la regia mostra i livelli di batteria e avvisa sotto il 20 %.

## 2. Ruoli e numeri

- **Regia** — l'hub; vede tutto, invia segnali, accoppia i telefoni. Numero 100.
- **Attore** — richiede il PIN; può parlare alla squadra e chiamare chiunque. Numeri da 201.
- **Spettatore** — riceve chiamate, messaggi e segnali; può parlare solo alla squadra. Numeri a sei cifre.
- La regia può cambiare qualsiasi numero e assegnare gruppi (per esempio *balconata*, *cantina*).

## 3. Usare il telefono

- **1985** — alza la cornetta (pulsante), attendi il tono, gira il disco; il numero parte da solo dopo una breve pausa.
- **1995** — alza, poi usa la tastiera; la rubrica elenca i numeri aggiunti dalla regia.
- **2003** — scrivi il numero e premi *Chiama*; i messaggi sono limitati a 160 caratteri.
- Un telefono che squilla si può prendere dalla notifica, anche a schermo bloccato.

## 4. La linea condivisa in pratica

- Seleziona due dispositivi nella scheda Regia e premi *Accoppia 2*, oppure *Coppie auto* per accoppiare tutti i telefoni liberi.
- Usa *Ascolta* sulla regia per monitorare una linea senza essere sentito.
- Le opzioni permettono di disattivare l'ascolto del vicino, il clic o i messaggi tra telefoni.

## 5. Segnali

- Scegli un bersaglio (tutti, spettatori, attori, dispositivi selezionati, un gruppo, un numero), un tipo e un testo, poi *Invia il segnale*.
- Esempi: `Mamma;204455` aggiunge un contatto; `1;È quasi mezzanotte` rende lo schermo nero con un testo discreto; `Dr. Voss;204455;Venite in cantina` fa partire una chiamata fantasma che parla quando si risponde.

## 6. Copione

- Costruisci lo spettacolo in passi con bersaglio, tipo, testo e pausa. **GO** invia il passo successivo; **PLAY** esegue il resto automaticamente a partire dal passo selezionato.
- Esporta il copione come JSON e importalo su un altro telefono di regia.

## 7. Risoluzione dei problemi

- *Nessun hub trovato* — verifica la stessa rete, disattiva l'isolamento dei client o scrivi l'indirizzo dell'hub.
- *PIN / codice sala errato* — la regia li mostra nella schermata iniziale.
- *Porta occupata* — chiudi ogni altra app che usa la porta 8470.
- *Chiamate mute* — controlla il permesso del microfono e l'interruttore dell'altoparlante nella scheda Voce.

## 8. Internet, VoIP e rete mobile (1.1)

- **Chiave di rete** — nella schermata iniziale attiva *Collegamento cifrato*; la regia mostra una chiave (xxxx‑xxxx‑xxxx‑xxxx). Inseriscila nel campo *Chiave di rete* su ogni altro telefono. Senza la chiave giusta l'hub rifiuta la connessione. Usala ogni volta che il traffico esce dalla rete locale.
- **Da internet** — inoltra TCP 8470 e UDP 8472 sul router verso il telefono della regia e indica l'indirizzo pubblico (`host` o `host:porta`). Con i dati mobili non serve altro.
- **P2P** — nella scheda Regia l'opzione *Chiamate dirette* alterna spente / solo rete locale / ovunque. *Ovunque* permette a due telefoni di conoscere l'IP pubblico reciproco; la regia non può monitorare, silenziare o intervenire finché la chiamata è diretta (torna al relay automaticamente se serve).
- **Linea SIP** — Impostazioni → *Linea VoIP / SIP…*: server, utente, password, dominio opzionale e numero interno per le chiamate in arrivo (predefinito 100). Riavvia l'hub per applicare. La squadra compone `0` + numero esterno; gli spettatori non possono chiamare fuori. Ogni chiamata è limitata a 15 minuti. SIP e RTP non sono cifrati.
- **SMS** — Impostazioni → *Gateway SMS* (servono SIM e permessi SMS). La squadra sceglie *SMS a un numero reale* nella scheda Messaggi; gli SMS ricevuti compaiono come messaggi da *SMS*. Limite: 20 al minuto. Si applicano le tariffe dell'operatore.
- **Chiamata reale** — il cue *chiamata reale* apre il tastierino con il numero. Android non consente a un'app di portare una chiamata cellulare nella linea condivisa: l'audio GSM non può essere instradato; per questo usa la linea SIP.

---

[← README](../README.it.md)
