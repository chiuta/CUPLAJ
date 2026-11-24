# CUPLAJ

[English](README.md) · [Română](README.ro.md) · [Français](README.fr.md) · [Italiano](README.it.md) · [Español](README.es.md) · [Português](README.pt.md) · **Deutsch** · [Magyar](README.hu.md)

<p align="center"><img src="docs/logo.svg" width="160" alt="CUPLAJ"></p>

> **Gemeinschaftsanschluss-Telefonie für immersives Theater**

_Version: 1.4.0 · Letzte Aktualisierung: 2026-11-24_

CUPLAJ verwandelt eine Flotte gewöhnlicher Android-Telefone in eine private Telefonanlage für immersives Theater. Organisatoren und Schauspieler sprechen über das lokale WLAN des Veranstaltungsorts mit den Zuschauern (und untereinander) — ohne Cloud, ohne Konten, ohne Werbung. Das Kernmerkmal ist der *Cuplaj*: der **Gemeinschaftsanschluss** der Festnetztelefone von den 1980er-Jahren bis etwa 2006, bei dem sich zwei Teilnehmer eine Leitung teilten, mit unterschiedlichem Rhythmus klingelten und einander mithören konnten.

## Funktionen

- **Drei Telefon-Epochen** — 1985 Wählscheibe (hörbare Impulse), 1995 Tastatur (DTMF-Töne), 2003 Handy (SMS, 160 Zeichen). Die Regie kann die Epoche live wechseln.
- **Gemeinschaftsanschluss (Cuplaj)** — zwei Telefone teilen eine Leitung; ein Anruf bei einem lässt beide mit dem Rhythmus des gerufenen Teilnehmers klingeln; wer zuerst abhebt, nimmt das Gespräch an; wer während des Nachbargesprächs abhebt, hört mit und kann sogar eingreifen.
- **Keine Infrastruktur** — ein Telefon (die Regie) ist der Hub. Die anderen verbinden sich per WLAN oder über dessen Hotspot; Hubs werden automatisch gefunden; Raumcode und PIN optional.
- **Stimme** — echte Gespräche (8 kHz), Push-to-Talk-Durchsagen von der Regie, Backstage-Kanal für das Team.
- **Signale** — Vollbildtext, Vibration, Klingeln, Blitz, Lautsprecher, Epochenwechsel, neuer Kontakt, gesprochene Stimme (lokale Sprachsynthese), Klangeffekte, schwarzer Bildschirm, Geisteranruf.
- **Ablauf** — Signalliste mit GO und PLAY (zeitgesteuert), Umsortieren, JSON-Import/-Export.
- **Regie-Werkzeuge** — Nummern, Gruppen, Koppeln/Entkoppeln, automatische Paare, stilles Mithören, Stummschalten, Vergessen, Akkustände, Ereignisprotokoll, Zustand übersteht Neustarts, Ratenbegrenzung.
- **8 Sprachen und Barrierefreiheit** — Englisch, Rumänisch, Französisch, Italienisch, Spanisch, Portugiesisch, Deutsch, Ungarisch; einstellbare Textgröße; Annehmen vom Sperrbildschirm; läuft im Hintergrund.
- **Internet und Mobilfunk (1.1)** — Verbindung mit `Host` oder `Host:Port`; optionaler AES‑128‑GCM‑Netzwerkschlüssel; automatische G.711‑Kompression außerhalb des lokalen Netzes; Keepalive.
- **Direkte P2P‑Gespräche (1.1)** — Audio läuft per UDP mit Hole‑Punching direkt von Telefon zu Telefon, mit Rückfall auf das Hub‑Relay; die Regie kann es abschalten, auf das lokale Netz begrenzen (Standard) oder überall erlauben.
- **VoIP‑/SIP‑Gateway (1.1)** — das Regie‑Telefon registriert sich bei einem SIP‑Anbieter oder einer Telefonanlage; das Team wählt `0`+Nummer nach außen, eingehende SIP‑Anrufe klingeln an einer internen Nummer.
- **Mobilfunknetz (1.1)** — SMS‑Gateway über die SIM des Regie‑Telefons (Senden und Empfangen), Cue *echter Anruf*, der die Wähl‑App öffnet, und WLAN‑/Mobil‑Anzeige.
- **Weitere Verbindungsarten (1.2)** — Bluetooth (ohne Router, etwa 7 Telefone), Wi‑Fi Direct (das Regie‑Telefon baut ein eigenes WLAN auf) und mDNS‑Erkennung, zusätzlich zu WLAN, Hotspot, USB‑Tethering, Ethernet und Mobilfunk.
- **Plug and Play (1.3)** — das Regie‑Telefon öffnet alle passenden Verbindungen selbst; die anderen tippen auf *Automatisch finden und beitreten* oder auf einen Einladungslink. Neu: Wi‑Fi Aware.
- **Gehärtet und ohne Netz (1.4)** — Audit mit feindlichem Publikum samt Korrekturen, und CUPLAJ‑AIR: drei eigene Protokolle, die Cues und Einladungen ganz ohne Netz übertragen (Taschenlampe → Kamera, Lautsprecher → Mikrofon, Vibration → Beschleunigungssensor).

## Schnellstart

1. Den APK im Tab *Actions* (Artefakt `CUPLAJ-apk`) oder auf der Seite *Releases* holen und auf jedem Telefon installieren.
2. Auf dem Regie-Telefon den Hotspot einschalten (oder alle Telefone in dasselbe WLAN bringen).
3. Auf dem Regie-Telefon **Regie** → *Hub starten* wählen. PIN für Schauspieler und optional einen Raumcode notieren.
4. Auf den anderen **Zuschauer** oder **Schauspieler** wählen, den Hub in der Liste auswählen (oder seine Adresse eingeben) und verbinden.
5. Jedes Telefon erhält eine Nummer (Regie 100, Schauspieler 201…, Zuschauer 6 Ziffern). Hörer abheben und anrufen.

## Die drei Epochen

- **1985 · Wählscheibe** — eine echte Scheibe zum Drehen; die Impulse sind hörbar; zwei lange Klingeltöne.
- **1995 · Tasten** — DTMF-Töne, Notizbuch mit Nummern, Pager-Nachrichten.
- **2003 · Handy** — Namen im Adressbuch, Anruferkennung, SMS auf 160 Zeichen begrenzt.

## So funktioniert der Gemeinschaftsanschluss

<p align="center"><img src="docs/partyline.svg" width="620" alt="party line"></p>

1. Zwei Telefone werden von der Regie *gekoppelt* (oder automatisch paarweise). Sie teilen Leitung N, Plätze 1 und 2.
2. Ein Anruf bei Teilnehmer 1 lässt beide klingeln; Teilnehmer 1 hört den Rhythmus seiner Nummer (ein langer Ton), Teilnehmer 2 den seinen (zwei kurze).
3. Wer annimmt, bekommt das Gespräch; das andere Telefon hört auf zu klingeln.
4. Solange die Leitung belegt ist, hört der Nachbar, der abhebt, das Gespräch mit, und die Gesprächspartner hören ein leises Klicken.
5. Ein Mithörer kann *eingreifen* und sprechen; ein Anruf zu einem gekoppelten Telefon mit belegter Leitung erhält den Besetztton.
6. Die Regie kann jede Leitung still überwachen (ohne Klicken).

## Dokumentation

- [Benutzerhandbuch](docs/GUIDE.de.md)
- [Protokoll](docs/PROTOCOL.md) *(English)*
- [Adversarielles Audit](docs/AUDIT.md) *(English)*
- [Änderungsprotokoll](CHANGELOG.md) *(English)*
- [Präsentationsseite](https://chiuta.github.io/CUPLAJ/)

## Ehrlicher Stand

Die Hub-Logik (Gespräche, Gemeinschaftsanschluss, Berechtigungen, Limits, Persistenz) wird von ~55 automatischen Prüfungen auf einer JVM abgedeckt. Die Android-Oberfläche und der Audiopfad haben die Typprüfung gegen das Android-14-SDK bestanden, wurden aber **noch nicht auf echten Geräten getestet**; der APK wird von GitHub Actions gebaut. Vor einer Vorstellung mit ein paar Telefonen testen. Der Verkehr im lokalen Netz ist nicht verschlüsselt (PIN und Raumcode halten gutgläubige Fremde fern, sie sind keine Kryptografie). Die Netzfunktionen der Version 1.1 (Verschlüsselung, P2P, SIP, SMS) sind durch Hub‑Tests gegen einen simulierten SIP‑Server abgedeckt, aber **nicht** an echten Anbietern, NATs oder SIM‑Karten erprobt. Die Verbindungen über Bluetooth, Wi‑Fi Direct und mDNS (1.2) nutzen dieselbe Transportschicht, auf der JVM mit simulierten Kanälen getestet, wurden aber **nicht** an echten Geräten erprobt; das Verhalten unterscheidet sich je nach Hersteller. Plug and Play (Auto‑Beitritt, Einladungen, Wi‑Fi Aware, 1.3) ist **nicht** an Geräten getestet; das Einladungsformat ist durch JVM‑Tests abgedeckt. Die Korrekturen des 1.4‑Audits und die AIR‑Codecs sind durch JVM‑Tests abgedeckt (simuliertes Rauschen, Taktdrift, falscher Schlüssel, Störungen); der Code für Taschenlampe, Kamera, Mikrofon, Lautsprecher, Motor und Beschleunigungssensor ist **nicht** auf echten Telefonen getestet, und echte Reichweite und Geschwindigkeit werden abweichen.

## Bauen

Benötigt JDK 17. `./gradlew assembleRelease` erzeugt `app/build/outputs/apk/release/app-release.apk` (mit dem Debug-Schlüssel signiert, direkt installierbar). Unit-Tests: `./gradlew test`. Der Workflow in `.github/workflows` erledigt beides, mit Node-≥-24-Werkzeugen auf Ubuntu 26.04.

## Datenschutz

Keine Analyse, keine Server, keine Konten. Ton und Nachrichten laufen nur in eurem lokalen Netz, zwischen den Telefonen und dem Hub. Der Hub speichert Nummern, Gruppen und Kopplungen auf dem Regie-Telefon, damit ein Neustart sie nicht verliert.

## Projekt unterstützen

CUPLAJ ist kostenlos und trade-free. Wenn es deiner Vorstellung hilft, kannst du dem Autor einen Kaffee spendieren: <https://buymeacoffee.com/echo.of.the.strings>.

## Lizenz

MIT © Alexandru‑Ionuț Chiuță (Alexio).
