# CUPLAJ — Benutzerhandbuch

[English](GUIDE.md) · [Română](GUIDE.ro.md) · [Français](GUIDE.fr.md) · [Italiano](GUIDE.it.md) · [Español](GUIDE.es.md) · [Português](GUIDE.pt.md) · **Deutsch** · [Magyar](GUIDE.hu.md)

_Version: 1.1.0 · Letzte Aktualisierung: 2026-11-24_

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

---

[← README](../README.de.md)
