# CUPLAJ — Ghid de utilizare

[English](GUIDE.md) · **Română** · [Français](GUIDE.fr.md) · [Italiano](GUIDE.it.md) · [Español](GUIDE.es.md) · [Português](GUIDE.pt.md) · [Deutsch](GUIDE.de.md) · [Magyar](GUIDE.hu.md)

_Versiune: 1.1.0 · Ultima actualizare: 2026-11-24_

## 1. Înainte de spectacol

- Folosește o singură rețea Wi‑Fi pentru toate telefoanele sau hotspotul telefonului Regiei. Dezactivează *izolarea clienților* pe routere.
- Instalează APK-ul, acordă permisiunile pentru microfon și notificări, iar din meniul **Aa** al aplicației alege *Păstrează aplicația activă*, ca sistemul să n-o adoarmă.
- Încarcă toate telefoanele; regia vede nivelul bateriei și avertizează sub 20 %.

## 2. Roluri și numere

- **Regie** — hubul; vede tot, trimite indicii, cuplează telefoane. Numărul 100.
- **Actor** — are nevoie de PIN; poate vorbi către echipă și poate suna pe oricine. Numere de la 201.
- **Spectator** — primește apeluri, mesaje și indicii; poate vorbi doar către echipă. Numere de șase cifre.
- Regia poate schimba orice număr și poate atribui grupuri (de exemplu *balcon*, *pivniță*).

## 3. Folosirea telefonului

- **1985** — ridică receptorul (butonul), așteaptă tonul, rotește discul; numărul se formează singur după o scurtă pauză.
- **1995** — ridică, apoi folosește tastele; carnetul arată numerele adăugate de regie.
- **2003** — scrie numărul și apasă *Apel*; mesajele au cel mult 160 de caractere.
- Un telefon care sună poate fi preluat din notificare, chiar și pe ecranul blocat.

## 4. Cuplajul în practică

- Selectează două dispozitive în fila Regie și apasă *Cuplează 2*, sau apasă *Perechi automate* ca să împerechezi toate telefoanele libere.
- Folosește *Ascultă* pe regie ca să monitorizezi o linie fără să fii auzit.
- Opțiunile permit oprirea ascultării de către vecin, a clicului sau a mesajelor între telefoane.

## 5. Indicii

- Alege ținta (toți, spectatori, actori, dispozitive selectate, un grup, un număr), un fel de indiciu și un text, apoi *Trimite indiciul*.
- Exemple: `Mama;204455` adaugă un contact; `1;E aproape miezul nopții` face ecranul negru cu un text discret; `Dr. Voss;204455;Vino în pivniță` face un apel fantomă care vorbește când e preluat.

## 6. Scenariu

- Construiește spectacolul din pași cu țintă, fel, text și pauză. **GO** trimite pasul următor; **PLAY** rulează restul automat, începând de la pasul selectat.
- Exportă scenariul ca JSON și importă-l pe alt telefon de regie.

## 7. Depanare

- *Niciun hub găsit* — verifică aceeași rețea, dezactivează izolarea clienților sau scrie adresa hubului.
- *PIN / cod de cameră greșit* — regia le arată pe ecranul de pornire.
- *Port ocupat* — închide orice altă aplicație care folosește portul 8470.
- *Apeluri mute* — verifică permisiunea pentru microfon și comutatorul difuzorului din fila Voce.

## 8. Internet, VoIP și rețea mobilă (1.1)

- **Cheia de rețea** — pe ecranul de start pornește *Legătură cifrată*; Regia afișează o cheie (xxxx‑xxxx‑xxxx‑xxxx). Scrie‑o în câmpul *Cheie de rețea* pe fiecare alt telefon. Fără cheia corectă hubul refuză conexiunea. Folosește‑o oricând traficul iese din rețeaua locală.
- **Din internet** — redirecționează TCP 8470 și UDP 8472 în router către telefonul Regiei și dă adresa publică (`host` sau `host:port`) în câmpul de adresă. Pe date mobile nu mai trebuie nimic.
- **P2P** — în fila Regie, opțiunea *Apeluri directe* alternează oprite / doar rețea locală / oriunde. *Oriunde* lasă două telefoane să‑și afle IP‑ul public; Regia nu poate monitoriza, amuți sau intra într‑un apel cât timp e direct (revine automat pe releu când are nevoie).
- **Linia SIP** — Setări → *Linie VoIP / SIP…*: server, utilizator, parolă, domeniu opțional și numărul intern pentru apeluri primite (implicit 100). Repornește hubul pentru aplicare. Echipa formează `0` + număr extern; spectatorii nu pot suna în afară. Fiecare apel e limitat la 15 minute. SIP și RTP nu sunt cifrate.
- **SMS** — Setări → *Poartă SMS* (necesită SIM și permisiunile SMS). Echipa alege *SMS către un număr real* în fila Mesaje; SMS‑urile primite apar echipei ca mesaje de la *SMS*. Limită: 20 pe minut. Se aplică tarifele operatorului.
- **Apel real** — cue‑ul *apel real* deschide formatorul telefonului cu numărul. Android nu permite unei aplicații să ducă un apel celular în cuplaj, deci sunetul apelurilor GSM nu poate fi legat; pentru asta folosește linia SIP.

---

[← README](../README.ro.md)
