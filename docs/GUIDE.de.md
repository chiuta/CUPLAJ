# CUPLAJ — Benutzerhandbuch

[English](GUIDE.md) · [Română](GUIDE.ro.md) · [Français](GUIDE.fr.md) · [Italiano](GUIDE.it.md) · [Español](GUIDE.es.md) · [Português](GUIDE.pt.md) · **Deutsch** · [Magyar](GUIDE.hu.md)

_Version: 1.10.0 · Letzte Aktualisierung: 2026-11-26_

## 1. Vor der Vorstellung

- Ein einziges WLAN für alle Telefone verwenden, oder den Hotspot des Regie-Telefons. *Client-Isolation* an Routern abschalten.
- APK installieren, Mikrofon- und Benachrichtigungsrechte erteilen und im Menü **Aa** *App aktiv halten* wählen, damit das System sie nicht schlafen legt.
- Alle Telefone laden; die Regie zeigt Akkustände und warnt unter 20 %.

## 2. Rollen und Nummern

- **Regie** — der Hub; sieht alles, sendet Signale, koppelt Telefone. Nummer 100.
- **Schauspieler** — braucht die PIN; kann zum Team sprechen und jeden anrufen. Nummern ab 201.
- **Zuschauer** — empfängt Anrufe, Nachrichten und Signale; kann nur zum Team sprechen. Sechsstellige Nummern.
- Die Regie kann jede Nummer ändern und Gruppen vergeben (zum Beispiel *Balkon*, *Keller*).

## 3. Das Telefon benutzen

- **1985** — Hörer abheben (Taste), Freizeichen abwarten, Scheibe drehen; die Nummer wird nach kurzer Pause von selbst gewählt.
- **1995** — abheben, dann die Tastatur benutzen; das Notizbuch listet die von der Regie hinzugefügten Nummern.
- **2003** — Nummer eingeben und *Anrufen* drücken; Nachrichten sind auf 160 Zeichen begrenzt.
- Ein klingelndes Telefon kann über die Benachrichtigung angenommen werden, auch bei gesperrtem Bildschirm.

## 4. Der Gemeinschaftsanschluss in der Praxis

- Im Regie-Tab zwei Geräte auswählen und *2 koppeln* drücken, oder *Auto-Paare*, um alle freien Telefone zu paaren.
- Mit *Mithören* in der Regie eine Leitung überwachen, ohne gehört zu werden.
- Optionen erlauben, das Mithören des Nachbarn, das Klicken oder Nachrichten zwischen Telefonen abzuschalten.

## 5. Signale

- Ziel wählen (alle, Zuschauer, Schauspieler, ausgewählte Geräte, eine Gruppe, eine Nummer), Art und Text eingeben, dann *Signal senden*.
- Beispiele: `Mutter;204455` fügt einen Kontakt hinzu; `1;Es ist fast Mitternacht` macht den Bildschirm schwarz mit blassem Text; `Dr. Voss;204455;Kommt in den Keller` löst einen Geisteranruf aus, der beim Annehmen spricht.

## 6. Ablauf

- Die Vorstellung aus Schritten mit Ziel, Art, Text und Pause bauen. **GO** sendet den nächsten Schritt; **PLAY** führt den Rest automatisch ab dem gewählten Schritt aus.
- Den Ablauf als JSON exportieren und auf einem anderen Regie-Telefon importieren.

## 7. Fehlerbehebung

- *Kein Hub gefunden* — dasselbe Netz prüfen, Client-Isolation abschalten oder die Hub-Adresse eingeben.
- *Falsche PIN / Raumcode* — die Regie zeigt beides auf ihrem Startbildschirm.
- *Port belegt* — jede andere App schließen, die Port 8470 nutzt.
- *Stumme Gespräche* — Mikrofonberechtigung und Lautsprecherschalter im Stimme-Tab prüfen.

## 8. Internet, VoIP und Mobilfunk (1.1)

- **Netzwerkschlüssel** — auf dem Startbildschirm *Verschlüsselte Verbindung* einschalten; die Regie zeigt einen Schlüssel (xxxx‑xxxx‑xxxx‑xxxx). Gib ihn auf jedem anderen Telefon im Feld *Netzwerkschlüssel* ein. Ohne den richtigen Schlüssel lehnt der Hub die Verbindung ab. Immer verwenden, wenn der Verkehr das lokale Netz verlässt.
- **Aus dem Internet** — am Router TCP 8470 und UDP 8472 auf das Regie‑Telefon weiterleiten und die öffentliche Adresse (`Host` oder `Host:Port`) angeben. Mit Mobilfunk ist nichts weiter nötig.
- **P2P** — im Reiter Regie schaltet die Option *Direktgespräche* zwischen aus / nur lokales Netz / überall um. *Überall* lässt zwei Telefone die öffentliche IP des anderen erfahren; die Regie kann ein direktes Gespräch weder mithören, stummschalten noch unterbrechen (bei Bedarf wechselt es automatisch zurück auf das Relay).
- **SIP‑Leitung** — Einstellungen → *VoIP‑/SIP‑Leitung…*: Server, Benutzer, Passwort, optionale Domain und interne Nummer für eingehende Anrufe (Standard 100). Zum Anwenden den Hub neu starten. Das Team wählt `0` + externe Nummer; Zuschauer können nicht nach außen telefonieren. Jeder Anruf ist auf 15 Minuten begrenzt. SIP und RTP sind nicht verschlüsselt.
- **SMS** — Einstellungen → *SMS‑Gateway* (SIM und SMS‑Berechtigungen nötig). Das Team wählt im Reiter Nachrichten *SMS an eine echte Nummer*; eingehende SMS erscheinen als Nachrichten von *SMS*. Limit: 20 pro Minute. Es gelten die Tarife des Anbieters.
- **Echter Anruf** — der Cue *echter Anruf* öffnet die Wähl‑App mit der Nummer. Android erlaubt einer App nicht, ein Mobilfunkgespräch in die Gemeinschaftsleitung zu leiten: GSM‑Audio lässt sich nicht verbinden; dafür die SIP‑Leitung nutzen.

## 9. Verbinden ohne Router (1.2)

- **Derselbe Hub, jede Verbindung** — Anrufe, Gemeinschaftsleitung, Cues, Verschlüsselung und PIN funktionieren gleich; nur der Weg des Telefons zum Hub ändert sich.
- **Hotspot, USB‑Tethering, Ethernet** — jede Verbindung, die den Telefonen ein IP‑Netz gibt, funktioniert ohne Einrichtung; Adresse eingeben oder den Hub finden lassen.
- **Bluetooth** — am Regie‑Telefon den Hub mit *Bluetooth‑Verbindung: AN* starten und *Per Bluetooth sichtbar machen* drücken. An den anderen *Per Bluetooth suchen* drücken und das Regie‑Telefon wählen (🔵). Die Reichweite ist kurz und etwa 7 Telefone passen an einen Hub; die Stimme wird automatisch komprimiert. Koppeln ist nicht nötig; der Zugang bleibt durch PIN, Raumcode und Netzwerkschlüssel geschützt.
- **Wi‑Fi Direct** — am Regie‑Telefon den Hub mit *Wi‑Fi‑Direct‑Gruppe: AN* starten. An den anderen *Wi‑Fi Direct suchen* drücken und das Regie‑Telefon wählen (📡); es fragt eventuell bei jedem Telefon nach Bestätigung. Nach dem Beitritt erscheint der Hub in der Liste. Meist können Telefone in einer Wi‑Fi‑Direct‑Gruppe nicht im gewohnten WLAN bleiben, und manche Modelle können nicht beides.
- **mDNS** — der Hub meldet sich zusätzlich per mDNS/DNS‑SD (🌐) und ist so auch in Netzen auffindbar, die das übliche Broadcast sperren.
- **Nicht enthalten** — Wi‑Fi Aware (wenige Telefone) und Googles Nearby Connections (braucht Google Play Services, die diese Trade‑free‑App meidet). Bluetooth Low Energy ist für Sprache zu langsam.

## 10. Plug and Play (1.3)

- **Regie‑Telefon** — *Regie* wählen, dann *Hub starten*. Bei aktivem *Plug & Play* (Standard) öffnet CUPLAJ WLAN / Hotspot, mDNS, Bluetooth und Wi‑Fi Aware und erstellt eine Wi‑Fi‑Direct‑Gruppe, wenn kein WLAN vorhanden ist. Die einzige Berechtigungsabfrage bestätigen. Die Einstellungen zeigen die aktiven Verbindungen.
- **Andere Telefone, am einfachsten** — Einstellungen → *Einladung für Zuschauer teilen* (oder *Schauspieler*) am Regie‑Telefon; den Link über eine beliebige App senden. Wer ihn mit installiertem CUPLAJ antippt, ist konfiguriert (Adresse, Schlüssel, PIN, Raum) und verbunden.
- **Andere Telefone, ohne Link** — Rolle wählen und *Automatisch finden und beitreten* tippen. CUPLAJ sucht auf allen Verbindungen gleichzeitig (WLAN, mDNS, Bluetooth, Wi‑Fi Direct, Wi‑Fi Aware) und verbindet sich mit dem besten Hub: erst das lokale Netz, zuletzt Bluetooth.
- **Wi‑Fi Aware** — unterstützende Telefone finden sich ohne Router und ohne Einrichtung. Viele unterstützen es nicht; nichts geht kaputt, wenn Ihres es nicht kann.
- **Sicherheit** — eine Einladung kann nur Zuschauer oder Schauspieler anlegen, nie ein Regie‑Telefon. Links enthalten Schlüssel und PIN; nur an Eingeladene senden.

## 11. Härtung + AIR (1.4)

- **Sichererer Hub** — zufällige 6‑stellige PIN, frischer Schlüssel pro Verbindung, Grenzen für nicht authentifizierte Verbindungen und Nachrichtengröße, Sperre nach wiederholt falscher PIN und Bestätigung, bevor ein Einladungslink angewendet wird.
- **LUMEN (Licht)** — das Regie‑Telefon blinkt mit der Taschenlampe; ein anderes Telefon beobachtet mit der rückseitigen Kamera. Raum abdunkeln, 1–3 m, Lampe auf die Kamera richten. Langsam (einige Sekunden), aber robust.
- **SONUS (Schall)** — der Lautsprecher spielt einen Tonkode, nahe Ultraschall (für die meisten Erwachsenen unhörbar) oder hörbar; das andere Telefon hört mit dem Mikrofon. Überträgt Cues und eine Hub‑Einladung (Adresse und PIN).
- **TACTUS (Vibration)** — der Vibrationsmotor sendet kurze und lange Impulse; ein anderes Telefon auf derselben Fläche spürt sie mit dem Beschleunigungssensor. Funktioniert in Stille und Dunkelheit für kurze Cues.
- **Benutzung** — Einstellungen → *Luftverbindungen*: gewünschte Empfänger einschalten (Kamera / Mikrofon / Beschleunigungssensor; das Telefon fragt nach der Berechtigung), dann am Regie‑Telefon eine Sende‑Taste drücken. Pakete tragen ein kurzes Authentifizierungs‑Tag aus dem Netzwerkschlüssel und lassen sich nicht wiedereinspielen.

## 12. Show‑Steuerung (1.5)

- **Derselbe Augenblick** — Text mit `@3.5;` beginnen, dann löst der Cue in 3,5 s auf allen Telefonen gleichzeitig aus. Die Telefone gleichen ihre Uhr beim Verbinden und jede Minute mit dem Hub ab; ein Cue mit mehr als 10 s Verspätung wird verworfen.
- **PANIK** — der Cue *PANIK: alles stoppen* beendet Klingeln, Vibration, Sprache, Klangeffekte, schwarze und farbige Bildschirme, falsche Anrufe und geplante Cues. Funktioniert auch über AIR.
- **Farbe** — `#RRGGBB;Sekunden;Pulse pro Sekunde` füllt den Bildschirm. Pulse sind für lichtempfindliche Zuschauer auf 2 pro Sekunde begrenzt; langer Druck schließt den Bildschirm.
- **Umfrage** — `Frage;Option 1;Option 2;…` fragt Zuschauer und Schauspieler; eine Stimme pro Telefon (änderbar). *Umfrage beenden* zeigt der Regie das Ergebnis. Umfragen können Schritte im Ablauf sein.
- **Zustellung** — nach jedem Cue zeigt die Regie *an N von M Telefonen online gesendet*. **Hardware‑PTT** — Einstellungen → *Hardware‑PTT‑Taste* macht Headset‑Taste und Lauter zu Push‑to‑Talk, solange die App vorn ist.

## 13. Medien, OSC, Vorrang (1.6)

- **PA‑Vorrang** — solange das Regie‑Telefon (oder ein Schauspieler) zu allen spricht, fallen alle anderen Töne auf 30 % und kehren sanft zurück. Einstellungen → *PA‑Vorrang* schaltet es aus.
- **Medien‑Cues** — am Regie‑Telefon: Reiter Cues → *Datei senden…* wählt Bild, Video oder Audio (bis 8 MB) und sendet es stückweise an die gewählten Telefone; jedes prüft Größe und Prüfsumme und ignoriert beschädigte Dateien. Danach *Bild auf dem Bildschirm*, *Video auf dem Bildschirm* oder *Audioclip* verwenden (leerer Text = letzte Datei, oder `Code;Sekunden`). Langer Druck schließt Bild oder Video. Später beigetretene Telefone haben die Datei nicht: erneut senden.
- **OSC aus** — Einstellungen → *OSC*: jeder Cue wird auch als `/cuplaj/cue <an> <Typ> <Text>` per UDP an ein Licht‑ oder Tonpult, QLab oder Companion gesendet.
- **OSC ein** — das Pult kann `/cuplaj/<Hub‑PIN>/go`, `/panic`, `/stop` oder `/cue <an> <Typ> <Text>` an die Regie senden (Standardport 8473). Die PIN in der Adresse ist der einzige Schutz: das Show‑Netz privat halten. Höchstens 20 Nachrichten pro Sekunde.
- **Nicht enthalten** — iOS (nur Android), Programm‑Feed und mehr Kanäle als die vorhandenen Gruppen.

## 14. Intelligente Show (1.7)

- **Zonen** — ein Telefon in einen Raum legen (oder in die Tasche einer Figur), Einstellungen → *Luftverbindungen* → *Zonen‑Bake* und antippen, bis die Zahl 1–9 erscheint: alle 7 s sendet es einen nahezu ultraschallnahen, für die meisten unhörbaren Code. Publikumstelefone mit *Empfangen: SONUS* melden die gehörte Zone; die Regie sieht 📍 neben dem Namen. 30 s Stille = gegangen.
- **Regeln** — Regie‑Telefon, Einstellungen → *Regeln*, eine pro Zeile: `zone 3 once > self | text | Willkommen, {name}`, `pollwin 1 > all | color | #00ff00;3`, `meter noise 70 > all | blackout | 1;`, `pollwin 2 > all | goto | Ende B`. *self* ist, wer die Regel ausgelöst hat; `goto` springt im Ablauf zu einem Schritt per Name oder Nummer: eine Umfrage kann die Geschichte wählen.
- **Publikumsmesser** — Reiter Cues: *Lärm* oder *Bewegung messen*, 5 s auf den gewählten Telefonen. Jedes sendet nur eine Zahl 0–100, nie Ton. Sie sehen Mittel, Spitze und wie viele geantwortet haben; Regeln können reagieren.
- **Stimmeffekte** — Telefone in der Regie‑Liste wählen, *Stimmeffekt…*: Roboter, altes Radio, Echo, tief, hoch, Lo‑Fi. Der Effekt wird auf diesem Telefon vor dem Senden angewendet: alle hören dieselbe Stimme.
- **Untertitel und Sprachen** — der Cue *Untertitel* zeigt große Untertitel; jeder Text‑Cue kann `en=Hello|ro=Salut|fr=Bonjour` tragen, jedes Telefon nimmt seine Sprache (dann Englisch, dann die erste). `{name}` und `{number}` werden zu Name und Nummer des empfangenden Telefons.

## 15. Bühnenwerkzeuge (1.8)

- **Licht und Musik** — die Cues *dmx* (`0;1=255,2-4=128;2000`) und *midi* (`note 60 127`, `cc 7 100`, `pc 5`, `mmc play`) laufen auf dem Regie‑Telefon: Einstellungen → *Art‑Net / DMX‑Ausgang* sendet Art‑Net (UDP 6454) an einen Node oder ein Pult im selben WLAN; MIDI geht an das erste gefundene USB‑/Bluetooth‑MIDI‑Gerät.
- **Welle** — Cue‑Text mit `~150;` beginnen: der Hub sendet ihn nacheinander in Nummernreihenfolge im Abstand von 150 ms an die Telefone.
- **Stille Post** — *Werkzeuge → Stille Post*: Nachricht und Nummern der Reihe nach; jeder sieht, was der Vorgänger weitergab, kann es ändern und gibt es weiter. Die Regie erhält das Original und alle Verformungen.
- **Geteiltes Geheimnis** — ein Satz wird Wort für Wort verteilt; wer zuerst den ganzen Satz sagt (Einstellungen → *Geheimen Satz sagen*), löst ihn; der Regel‑Auslöser `solved` kann einen Cue starten.
- **Geisterband** — nimmt bis zu 60 s vom Mikrofon eines Telefons auf dem Hub auf und spielt sie später einer Gruppe vor, wie eine Stimme aus dem Nichts (reservierte Quelle 253).
- **Saallicht** — Cue *house*: stoppt alles (wie PANIK), vibriert, weißer Bildschirm und ruhige Meldung. **Morse** — Cue *morse*: vibriert einen Text in Morsecode.
- **Bericht** — *Werkzeuge → Bericht*: Markdown‑Datei mit Summen, Anrufen/Nachrichten/Zonenbesuchen/Akku pro Telefon und vollständiger Zeitleiste.
- **Grenzen und Sicherheit (1.8)** — eine Welle dauert nie länger als 20 s (der Abstand schrumpft in großen Sälen; höchstens 4 Wellen gleichzeitig); ein Telefon, das bei Stille Post nicht in 90 s antwortet, wird übersprungen; ein geteiltes Geheimnis braucht einen Satz mit mindestens 2 Wörtern und 4 lateinischen Buchstaben oder Ziffern (die Prüfung ignoriert Groß‑/Kleinschreibung, Akzente und Satzzeichen); nur eine Geisterband‑Wiedergabe gleichzeitig; Morse wird bei 45 s abgeschnitten; Art‑Net sendet nichts vor dem ersten *dmx*‑Cue (es kann also nicht durch Einschalten das Licht ausknipsen) und verwaltet bis zu 8 Universen; *dmx* und *midi* funktionieren nur auf dem Regie‑Telefon (Cues und Skripte), nicht als Regelaktionen.

## 16. Cue‑Referenz (alle Arten)

- **Präfixe** — `@3.5;` löst den Cue nach 3,5 s auf allen Telefonen gleichzeitig aus; `~150;` (nach `@…;`) sendet ihn nacheinander an die Telefone, im Abstand von 150 ms; `en=Hello|ro=Salut` wählt die Sprache jedes Telefons; `{name}` und `{number}` werden auf jedem Telefon ersetzt.
- **`text`** — Text auf dem Bildschirm: Text im Vollbild
- **`vibrate`** — Vibration: Dauer in ms (z. B. 800)
- **`ring`** — Klingeln: (kein Text nötig)
- **`flash`** — Blitz: Dauer in ms (z. B. 1500)
- **`speaker`** — Lautsprecher: 1 = an, 0 = aus
- **`contact`** — Kontakt hinzufügen: Name;Nummer (z. B. Mutter;204455)
- **`era`** — Epoche wechseln: 1985, 1995, 2003, 2025, 2045 oder 2080 (oder eine Liste: 1985,2080;4000)
- **`say`** — Gesprochene Stimme: Zu sprechender Text
- **`sound`** — Klangeffekt: bell, siren, knock, static (Glocke, Sirene, Klopfen, Rauschen)
- **`blackout`** — Schwarzer Bildschirm: 1;Text oder 0
- **`fakecall`** — Geisteranruf: Name;Nummer;gesprochener Text
- **`realcall`** — Echter Anruf (öffnet die Wähl-App): Zu wählende Nummer, z. B. +40721000111
- **`panic`** — PANIK: alles stoppen: (kein Text)
- **`color`** — Vollbildfarbe: #RRGGBB;Sekunden;Pulse pro Sekunde (max. 2)
- **`poll`** — Umfrage (Publikumsabstimmung): Frage;Option 1;Option 2;… (2–6 Optionen)
- **`pollend`** — Umfrage beenden und Ergebnis zeigen: (kein Text)
- **`image`** — Bild auf dem Bildschirm: Dateicode;Sekunden (Datei zuerst senden; leer = letzte)
- **`video`** — Video auf dem Bildschirm: Dateicode;Sekunden (Datei zuerst senden; leer = letzte)
- **`clip`** — Audioclip: Dateicode;Sekunden (Datei zuerst senden; leer = letzte)
- **`caption`** — Untertitel auf dem Bildschirm: Text oder en=Hello|ro=Salut|fr=Bonjour (jedes Telefon zeigt seine Sprache; {name} = Name des Telefons)
- **`dmx`** — Licht: Art‑Net/DMX (Regie‑Telefon): Universum;Kanal=Wert,Bereich=Wert;Überblendung ms — z. B. 0;1=255,2-4=128;2000
- **`midi`** — MIDI‑Ausgang (Regie‑Telefon): note 60 127 [Kanal] · off 60 [Kanal] · cc 7 100 [Kanal] · pc 5 [Kanal] · mmc play|stop|record
- **`morse`** — Morsecode per Vibration: zu sendender Text (Buchstaben und Ziffern; ~ 60 Zeichen)
- **`house`** — Saallicht (Sicherheit: Stopp + weißer Bildschirm): (optional) Meldung auf dem weißen Bildschirm
- **Regel‑Auslöser und ‑Aktionen** — Eine Regel pro Zeile: ‹wann› > ‹wer› | ‹Cue‑Typ› | ‹Text›. Wann: `zone 3 [once]`, `pollwin 0` (Gewinneroption, ab 0), `meter noise|motion 70` (Mittel ≥ 70). Wer: all, staff, spectators, actors, self (wer ausgelöst hat), num:/id:/grp:. Typ `goto` springt zu einem Ablaufschritt per Name oder Nummer. Der Text darf {name} nutzen. Auch `solved` (ein geteiltes Geheimnis wurde erraten).

## 17. Bildschirme und Schaltflächen (genau wie in der App)

- **Stimme** — `DURCHSAGE an alle Telefone` · `NUR ZUM TEAM`
- **Regie** — `Nummer` · `Gruppe` · `2 koppeln` · `Entkoppeln` · `Auto-Paare` · `Mithören` · `Stumm` · `Vergessen` · `Protokoll` · `Anrufen` · `Epoche …` · `Nachbar hört mit: …` · `Klick beim Abheben: …` · `Nachrichten zwischen Telefonen: …` · `Direktgespräche (P2P): …` · `Signal senden` · `Lärm messen (5 s)` · `Bewegung messen (5 s)` · `Stimmeffekt für gewählte Telefone…` · `Datei (Bild, Video, Audio) an die gewählten Telefone senden…` · `Werkzeuge: Kette, Geheimnis, Rekorder, Bericht`
- **Ablauf** — `Schritt hinzufügen` · `GO ▶` · `⏮ Anfang` · `Löschen` · `Exportieren` · `Importieren`
- **Einstellungen (Menü Aa)** — `TEXTGRÖSSE` · `App aktiv halten (Akkuoptimierung)` · `🔍 Automatisch finden und beitreten` · `Per Bluetooth suchen` · `Wi‑Fi Direct suchen` · `Per Bluetooth sichtbar machen (5 Min.)` · `Einladung für Zuschauer teilen` · `Einladung für Schauspieler teilen` · `VoIP-/SIP-Leitung…` · `Luftverbindungen: Licht, Schall, Vibration` · `Zonen‑Bake (dieses Telefon, Lautsprecher): …` · `OSC: Licht‑/Tonpulte, QLab…` · `Art‑Net / DMX‑Ausgang (Licht)` · `Regeln: Zone, Umfrage, Publikum → Cue` · `PA‑Vorrang: andere Töne werden leiser, wenn die Regie spricht` · `Hardware‑PTT‑Taste: an (Headset‑Taste, Lauter)` · `Geheimen Satz sagen` · `Hub-Gedächtnis zurücksetzen (Nummern, Kopplungen)` · `Über CUPLAJ` · `Beenden`
- Einstellungen und Regie‑Optionen zeigen auch ihren Zustand (an/aus).

## 18. iPhone und iPad (iOS)

- Die iOS‑App ist ein vollständiger Client und kann auch die Vorstellung hosten (Regie). Sie spricht dasselbe Protokoll: Android‑ und iOS‑Telefone lassen sich frei mischen.
- Build: `ios/` öffnen (XcodeGen: `cd ios && xcodegen generate`), in Xcode das Signing‑Team wählen und starten. Es gibt noch keinen App‑Store‑ oder TestFlight‑Build.
- Enthalten: Gespräche und Gemeinschaftsanschluss, Cues, Ablauf, Umfragen, Regeln, Zähler, Medien, Durchsagen (PA), Art‑Net, MIDI, OSC, direktes P2P‑Audio, SIP‑Gateway, CUPLAJ‑AIR (Licht, Schall, Vibration) und Audio im Hintergrund.
- Unter iOS nicht möglich (keine öffentliche API): Bluetooth RFCOMM, Wi‑Fi Direct, stilles SMS‑ und Anruf‑Gateway. Den iPhone‑Hotspot oder ein gemeinsames WLAN nutzen; der Cue *echter Anruf* öffnet die Wähl‑App.
- App geöffnet und Bildschirm an lassen, das Regie‑Telefon am Ladegerät. Bisher liefen nur Simulator‑Builds und automatische Tests — **noch nicht auf echten iPhones ausprobiert**.

---

[← README](../README.de.md)
