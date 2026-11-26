# CUPLAJ — Guida all'uso

[English](GUIDE.md) · [Română](GUIDE.ro.md) · [Français](GUIDE.fr.md) · **Italiano** · [Español](GUIDE.es.md) · [Português](GUIDE.pt.md) · [Deutsch](GUIDE.de.md) · [Magyar](GUIDE.hu.md)

_Versione: 1.10.0 · Ultimo aggiornamento: 2026-11-26_

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

## 9. Collegarsi senza router (1.2)

- **Stesso hub, qualunque collegamento** — chiamate, linea condivisa, cue, cifratura e PIN funzionano allo stesso modo; cambia solo il percorso del telefono verso l'hub.
- **Hotspot, tethering USB, Ethernet** — ogni collegamento che dà ai telefoni una rete IP funziona senza configurazione; usa l'indirizzo o lascia che l'hub venga trovato.
- **Bluetooth** — sul telefono della regia avvia l'hub con *Collegamento Bluetooth: SÌ* e premi *Rendi visibile via Bluetooth*. Sugli altri premi *Cerca via Bluetooth* e scegli il telefono della regia (🔵). Il raggio è corto e su un hub stanno circa 7 telefoni; la voce viene compressa automaticamente. L'abbinamento non è obbligatorio; l'accesso resta protetto da PIN, codice sala e chiave di rete.
- **Wi‑Fi Direct** — sul telefono della regia avvia l'hub con *Gruppo Wi‑Fi Direct: SÌ*. Sugli altri premi *Cerca Wi‑Fi Direct* e scegli il telefono della regia (📡); può chiedere di accettare ogni telefono. Dopo l'unione l'hub compare nell'elenco. Di solito i telefoni non possono restare sul loro Wi‑Fi abituale in un gruppo Wi‑Fi Direct, e alcuni modelli non gestiscono entrambi.
- **mDNS** — l'hub si annuncia anche via mDNS/DNS‑SD (🌐), così è visibile su reti che bloccano il broadcast abituale.
- **Non inclusi** — Wi‑Fi Aware (pochi telefoni) e Nearby Connections di Google (richiede Google Play Services, che questa app trade‑free evita). Il Bluetooth Low Energy è troppo lento per la voce.

## 10. Plug and play (1.3)

- **Telefono della regia** — scegli *Regia*, poi *Avvia l'hub*. Con *Plug & play* attivo (predefinito) CUPLAJ apre Wi‑Fi / hotspot, mDNS, Bluetooth e Wi‑Fi Aware e crea un gruppo Wi‑Fi Direct se non c'è una rete Wi‑Fi. Accetta l'unica richiesta di permessi. Le impostazioni mostrano i collegamenti attivi.
- **Altri telefoni, il modo più semplice** — Impostazioni → *Condividi invito per spettatori* (o *attori*) sul telefono della regia; invia il link con qualsiasi app. Chi lo tocca, con CUPLAJ installato, viene configurato (indirizzo, chiave, PIN, sala) e collegato.
- **Altri telefoni, senza link** — scegli un ruolo e tocca *Trova e unisciti automaticamente*. CUPLAJ cerca su tutti i collegamenti insieme (Wi‑Fi, mDNS, Bluetooth, Wi‑Fi Direct, Wi‑Fi Aware) e si collega al miglior hub: prima la rete locale, per ultimo il Bluetooth.
- **Wi‑Fi Aware** — i telefoni compatibili si trovano senza router né configurazione. Molti non lo supportano; non si rompe nulla se il tuo non lo fa.
- **Sicurezza** — un invito può creare solo uno spettatore o un attore, mai un telefono della regia. I link contengono chiave e PIN; inviali solo alle persone invitate.

## 11. Rinforzo + AIR (1.4)

- **Hub più sicuro** — PIN casuale di 6 cifre, chiave nuova a ogni connessione, limiti su connessioni non autenticate e dimensione dei messaggi, blocco dopo PIN errati ripetuti e conferma prima di applicare un link di invito.
- **LUMEN (luce)** — il telefono della regia fa lampeggiare la torcia; un altro telefono guarda con la fotocamera posteriore. Stanza poco illuminata, 1–3 m, torcia puntata sulla fotocamera. Lento (qualche secondo) ma robusto.
- **SONUS (suono)** — l'altoparlante riproduce un codice di toni, quasi ultrasonico (inudibile per la maggior parte degli adulti) o udibile; l'altro telefono ascolta col microfono. Porta cue e un invito all'hub (indirizzo e PIN).
- **TACTUS (vibrazione)** — il motore di vibrazione invia impulsi brevi e lunghi; un altro telefono appoggiato sulla stessa superficie li sente con l'accelerometro. Funziona in silenzio e al buio, per cue brevi.
- **Uso** — Impostazioni → *Collegamenti via aria*: attiva gli ascolti voluti (fotocamera / microfono / accelerometro; il telefono chiede il permesso), poi sul telefono della regia premi un pulsante di invio. I pacchetti hanno una breve etichetta di autenticazione derivata dalla chiave di rete e non possono essere riprodotti.

## 12. Controllo dello spettacolo (1.5)

- **Stesso istante** — inizia il testo con `@3.5;` per farlo scattare tra 3,5 s su tutti i telefoni insieme. I telefoni sincronizzano l'orologio con l'hub alla connessione e ogni minuto; un cue in ritardo di oltre 10 s viene scartato.
- **PANICO** — il cue *PANICO: ferma tutto* ferma suoneria, vibrazione, voce, effetti sonori, schermi neri e colorati, finte chiamate e cue programmati. Funziona anche via AIR.
- **Colore** — `#RRGGBB;secondi;impulsi al secondo` riempie lo schermo. Gli impulsi sono limitati a 2 al secondo per il pubblico fotosensibile; una pressione lunga chiude lo schermo.
- **Sondaggio** — `domanda;opzione 1;opzione 2;…` interroga spettatori e attori; un voto per telefono (modificabile). *Chiudi il sondaggio* mostra il risultato alla regia. I sondaggi possono essere passi del copione.
- **Consegna** — dopo ogni cue la regia vede *inviato a N telefoni su M online*. **PTT hardware** — Impostazioni → *Tasto PTT hardware* trasforma il tasto dell'auricolare e volume + in push‑to‑talk con l'app in primo piano.

## 13. Media, OSC, priorità (1.6)

- **Priorità PA** — mentre il telefono della regia (o un attore) parla a tutti, ogni altro suono scende al 30 % e torna dolcemente. Impostazioni → *Priorità PA* la spegne.
- **Cue media** — sul telefono della regia, scheda Cue → *Invia un file…* sceglie un'immagine, un video o un audio (fino a 8 MB) e lo invia a pezzi ai telefoni scelti; ognuno verifica dimensione e checksum e ignora un file danneggiato. Poi usa *Immagine a schermo*, *Video a schermo* o *Clip audio* (testo vuoto = ultimo file, o `codice;secondi`). Una pressione lunga chiude immagine o video. I telefoni entrati dopo non hanno il file: invialo di nuovo.
- **OSC in uscita** — Impostazioni → *OSC*: ogni cue è inviato anche come `/cuplaj/cue <verso> <tipo> <testo>` via UDP a un mixer luci o audio, QLab o Companion.
- **OSC in ingresso** — il mixer può inviare `/cuplaj/<PIN hub>/go`, `/panic`, `/stop` o `/cue <verso> <tipo> <testo>` alla regia (porta 8473 predefinita). Il PIN nell'indirizzo è l'unica protezione: tieni privata la rete dello spettacolo. Al massimo 20 messaggi al secondo.
- **Non inclusi** — iOS (solo Android), program feed e più canali dei gruppi esistenti.

## 14. Spettacolo intelligente (1.7)

- **Zone** — metti un telefono in una stanza (o nella tasca di un personaggio), Impostazioni → *Collegamenti via aria* → *Segnale di zona* e tocca fino al numero 1–9: ogni 7 s emette un codice quasi ultrasonico, inudibile ai più. I telefoni del pubblico con *Ascolta: SONUS* attivo segnalano la zona sentita; la regia vede 📍 accanto al nome. 30 s di silenzio = sono usciti.
- **Regole** — telefono della regia, Impostazioni → *Regole*, una per riga: `zone 3 once > self | text | Benvenuto, {name}`, `pollwin 1 > all | color | #00ff00;3`, `meter noise 70 > all | blackout | 1;`, `pollwin 2 > all | goto | Finale B`. *self* è chi ha attivato la regola; `goto` salta a un passo del copione per nome o numero: un sondaggio può scegliere la storia.
- **Contatore del pubblico** — scheda Cue: *Misura il rumore* o *il movimento* per 5 s sui telefoni scelti. Ognuno invia solo un numero 0–100, mai audio. Vedi media, picco e quanti hanno risposto; le regole possono reagire.
- **Effetti voce** — scegli telefoni nell'elenco della regia, *Effetto voce…*: robot, vecchia radio, eco, profonda, acuta, lo‑fi. L'effetto si applica su quel telefono prima dell'invio: tutti sentono la stessa voce.
- **Sottotitoli e lingue** — il cue *Sottotitolo* mostra grandi sottotitoli; ogni cue testo può portare `en=Hello|ro=Salut|fr=Bonjour` e ogni telefono usa la sua lingua (poi inglese, poi la prima). `{name}` e `{number}` diventano nome e numero del telefono che riceve.

## 15. Strumenti di scena (1.8)

- **Luci e musica** — i cue *dmx* (`0;1=255,2-4=128;2000`) e *midi* (`note 60 127`, `cc 7 100`, `pc 5`, `mmc play`) girano sul telefono della regia: Impostazioni → *Uscita Art‑Net / DMX* invia Art‑Net (UDP 6454) a un nodo o mixer sulla stessa Wi‑Fi; il MIDI va al primo dispositivo MIDI USB/Bluetooth trovato.
- **Onda** — inizia il testo di un cue con `~150;`: l'hub lo invia ai telefoni uno dopo l'altro, in ordine di numero, a 150 ms.
- **Telefono senza fili** — *Strumenti → Telefono senza fili*: messaggio e numeri in ordine; ognuno vede ciò che ha passato il precedente, può cambiarlo e lo passa. La regia riceve l'originale e tutte le deformazioni.
- **Segreto frammentato** — una frase è divisa parola per parola; chi dice per primo la frase intera (Impostazioni → *Di' la frase segreta*) la risolve; il trigger `solved` può far partire un cue.
- **Registratore fantasma** — registra fino a 60 s dal microfono di un telefono sull'hub e lo riproduce poi a un gruppo, come una voce dal nulla (sorgente riservata 253).
- **Luci di sala** — cue *house*: ferma tutto (come PANICO), vibra, schermo bianco e messaggio calmo. **Morse** — cue *morse*: vibra un testo in Morse.
- **Rapporto** — *Strumenti → Rapporto*: file Markdown con totali, chiamate/messaggi/ingressi in zona/batteria per telefono e cronologia completa.
- **Limiti e sicurezza (1.8)** — un'onda non dura mai più di 20 s (il passo si riduce nelle sale grandi; al massimo 4 onde insieme); un telefono che non risponde in 90 s nel telefono senza fili viene saltato; un segreto frammentato richiede una frase di almeno 2 parole e 4 lettere latine o cifre (il controllo ignora maiuscole, accenti e punteggiatura); una sola riproduzione del registratore alla volta; il Morse è tagliato a 45 s; Art‑Net non invia nulla prima del primo cue *dmx* (non può spegnere le luci solo perché attivato) e gestisce fino a 8 universi; *dmx* e *midi* funzionano solo sul telefono della regia (cue e copioni), non come azioni di regola.

## 16. Riferimento dei cue (tutti i tipi)

- **Prefissi** — `@3.5;` fa partire il cue dopo 3,5 s su tutti i telefoni insieme; `~150;` (dopo `@…;`) lo invia ai telefoni uno dopo l'altro, a 150 ms; `en=Hello|ro=Salut` sceglie la lingua di ogni telefono; `{name}` e `{number}` sono sostituiti su ogni telefono.
- **`text`** — Testo a schermo: Testo mostrato a tutto schermo
- **`vibrate`** — Vibrazione: Durata in ms (es. 800)
- **`ring`** — Squillo: (nessun testo necessario)
- **`flash`** — Flash: Durata in ms (es. 1500)
- **`speaker`** — Altoparlante: 1 = acceso, 0 = spento
- **`contact`** — Aggiungi contatto: Nome;numero (es. Mamma;204455)
- **`era`** — Cambia epoca: 1985, 1995, 2003, 2025, 2045 o 2080 (o un elenco: 1985,2080;4000)
- **`say`** — Voce parlata: Testo da pronunciare
- **`sound`** — Effetto sonoro: bell, siren, knock, static (campana, sirena, bussare, disturbo)
- **`blackout`** — Schermo nero: 1;testo oppure 0
- **`fakecall`** — Chiamata fantasma: Nome;numero;parole pronunciate
- **`realcall`** — Chiamata reale (apre il tastierino): Numero da comporre, es. +40721000111
- **`panic`** — PANICO: ferma tutto: (senza testo)
- **`color`** — Colore a schermo intero: #RRGGBB;secondi;impulsi al secondo (max 2)
- **`poll`** — Sondaggio (voto del pubblico): domanda;opzione 1;opzione 2;… (2–6 opzioni)
- **`pollend`** — Chiudi il sondaggio e mostra il risultato: (senza testo)
- **`image`** — Immagine a schermo: codice file;secondi (invia prima il file; vuoto = ultimo)
- **`video`** — Video a schermo: codice file;secondi (invia prima il file; vuoto = ultimo)
- **`clip`** — Clip audio: codice file;secondi (invia prima il file; vuoto = ultimo)
- **`caption`** — Sottotitolo a schermo: testo, oppure en=Hello|ro=Salut|fr=Bonjour (ogni telefono mostra la sua lingua; {name} = nome del telefono)
- **`dmx`** — Luci: Art‑Net/DMX (telefono della regia): universo;canale=valore,intervallo=valore;dissolvenza ms — es. 0;1=255,2-4=128;2000
- **`midi`** — Uscita MIDI (telefono della regia): note 60 127 [can] · off 60 [can] · cc 7 100 [can] · pc 5 [can] · mmc play|stop|record
- **`morse`** — Codice Morse a vibrazione: testo da trasmettere (lettere e cifre; ~ 60 caratteri)
- **`house`** — Luci di sala (sicurezza: stop + schermo bianco): (facoltativo) messaggio mostrato sullo schermo bianco
- **Trigger e azioni delle regole** — Una regola per riga: ‹quando› > ‹chi› | ‹tipo di cue› | ‹testo›. Quando: `zone 3 [once]`, `pollwin 0` (opzione vincente, da 0), `meter noise|motion 70` (media ≥ 70). Chi: all, staff, spectators, actors, self (chi ha attivato), num:/id:/grp:. Il tipo `goto` salta a un passo del copione per nome o numero. Il testo può usare {name}. Anche `solved` (un segreto frammentato è stato indovinato).

## 17. Schermate e pulsanti (esattamente come nell'app)

- **Voce** — `ANNUNCIO a tutti i telefoni` · `SOLO ALLA SQUADRA`
- **Regia** — `Numero` · `Gruppo` · `Accoppia 2` · `Disaccoppia` · `Coppie auto` · `Ascolta` · `Muto` · `Dimentica` · `Registro` · `Chiama` · `Epoca …` · `Vicino in ascolto: …` · `Clic al sollevamento: …` · `Messaggi tra telefoni: …` · `Chiamate dirette (P2P): …` · `Invia il segnale` · `Misura il rumore (5 s)` · `Misura il movimento (5 s)` · `Effetto voce per i telefoni scelti…` · `Invia un file (immagine, video, audio) ai telefoni scelti…` · `Strumenti: catena, segreto, registratore, rapporto`
- **Copione** — `Aggiungi passo` · `GO ▶` · `⏮ Inizio` · `Elimina` · `Esporta` · `Importa`
- **Impostazioni (menu Aa)** — `DIMENSIONE DEL TESTO` · `Mantieni l'app attiva (ottimizzazione batteria)` · `🔍 Trova e unisciti automaticamente` · `Cerca via Bluetooth` · `Cerca Wi‑Fi Direct` · `Rendi visibile via Bluetooth (5 min)` · `Condividi invito per spettatori` · `Condividi invito per attori` · `Linea VoIP / SIP…` · `Collegamenti via aria: luce, suono, vibrazioni` · `Segnale di zona (questo telefono, altoparlante): …` · `OSC: mixer luci / audio, QLab…` · `Uscita Art‑Net / DMX (luci)` · `Regole: zona, sondaggio, pubblico → cue` · `Priorità PA: gli altri suoni si abbassano quando parla la regia` · `Tasto PTT hardware: attivo (tasto auricolare, volume +)` · `Di’ la frase segreta` · `Azzera la memoria dell'hub (numeri, accoppiamenti)` · `Informazioni su CUPLAJ` · `Esci`
- Impostazioni e opzioni della regia mostrano anche lo stato (attivo/spento).

## 18. iPhone e iPad (iOS)

- L'app iOS è un client completo e può anche ospitare lo spettacolo (regia). Parla lo stesso protocollo: telefoni Android e iOS si mescolano liberamente.
- Compilazione: apri `ios/` (XcodeGen: `cd ios && xcodegen generate`), scegli il team di firma in Xcode ed esegui. Non esiste ancora una versione App Store o TestFlight.
- Incluso: chiamate e linea condivisa, cue, copione, sondaggi, regole, contatore, media, impianto voce (PA), Art‑Net, MIDI, OSC, audio P2P diretto, gateway SIP, CUPLAJ‑AIR (luce, suono, vibrazione) e audio in background.
- Impossibile su iOS (nessuna API pubblica): Bluetooth RFCOMM, Wi‑Fi Direct, gateway silenzioso di SMS e chiamate. Usa l'hotspot dell'iPhone o un Wi‑Fi comune; il cue *chiamata reale* apre il tastierino.
- Tieni l'app aperta con lo schermo acceso e il telefono di regia in carica. Finora sono girati solo build per simulatore e test automatici — **non ancora provata su iPhone reali**.

---

[← README](../README.it.md)
