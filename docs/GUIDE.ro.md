# CUPLAJ — Ghid de utilizare

[English](GUIDE.md) · **Română** · [Français](GUIDE.fr.md) · [Italiano](GUIDE.it.md) · [Español](GUIDE.es.md) · [Português](GUIDE.pt.md) · [Deutsch](GUIDE.de.md) · [Magyar](GUIDE.hu.md)

_Versiune: 1.5.0 · Ultima actualizare: 2026-11-24_

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

## 9. Conectare fără router (1.2)

- **Același hub, orice legătură** — apelurile, cuplajul, cue‑urile, cifrarea și PIN‑ul funcționează la fel pe orice legătură; se schimbă doar drumul telefonului către hub.
- **Hotspot, tethering USB, Ethernet** — orice legătură care dă telefoanelor o rețea IP merge fără nicio configurare; folosește adresa sau lasă hubul să fie găsit.
- **Bluetooth** — pe telefonul Regiei pornește hubul cu *Legătură Bluetooth: DA* și apasă *Fă telefonul vizibil prin Bluetooth*. Pe celelalte apasă *Caută prin Bluetooth* și alege telefonul Regiei (🔵). Raza e mică și încap circa 7 telefoane pe un hub; vocea se comprimă automat. Asocierea nu e obligatorie; accesul rămâne protejat de PIN, codul camerei și cheia de rețea.
- **Wi‑Fi Direct** — pe telefonul Regiei pornește hubul cu *Grup Wi‑Fi Direct: DA*. Pe celelalte apasă *Caută Wi‑Fi Direct* și alege telefonul Regiei (📡); telefonul Regiei poate cere să accepți fiecare telefon. După alăturare, hubul apare în listă. De obicei telefoanele nu pot rămâne pe Wi‑Fi‑ul obișnuit cât sunt într‑un grup Wi‑Fi Direct, iar unele modele nu pot face ambele.
- **mDNS** — hubul se anunță și prin mDNS/DNS‑SD (🌐), ceea ce îl face vizibil în rețele care blochează difuzarea obișnuită.
- **Nu sunt incluse** — Wi‑Fi Aware (puține telefoane îl au) și Nearby Connections de la Google (cere Google Play Services, pe care această aplicație trade‑free îl evită). Bluetooth Low Energy e prea lent pentru voce.

## 10. Plug and play (1.3)

- **Telefonul Regiei** — alege *Regie*, apoi *Pornește hubul*. Cu *Plug & play* pornit (implicit) CUPLAJ deschide Wi‑Fi / hotspot, mDNS, Bluetooth și Wi‑Fi Aware și creează un grup Wi‑Fi Direct când nu există rețea Wi‑Fi. Acceptă o singură cerere de permisiuni. Setările arată legăturile active.
- **Celelalte telefoane, cel mai simplu** — Setări → *Trimite invitație pentru spectatori* (sau *actori*) pe telefonul Regiei; trimite linkul prin orice aplicație. Cine îl atinge, cu CUPLAJ instalat, este configurat (adresă, cheie, PIN, cameră) și conectat.
- **Celelalte telefoane, fără link** — alege un rol și apasă *Găsește și alătură‑mă automat*. CUPLAJ caută pe toate legăturile deodată (Wi‑Fi, mDNS, Bluetooth, Wi‑Fi Direct, Wi‑Fi Aware) și se conectează la cel mai bun hub găsit: întâi rețeaua locală, la urmă Bluetooth.
- **Wi‑Fi Aware** — telefoanele care îl au se găsesc fără router și fără configurare. Multe telefoane nu îl au; nimic nu se strică dacă al tău nu îl are.
- **Siguranță** — o invitație poate crea doar spectator sau actor, niciodată telefon de Regie. Linkurile conțin cheia și PIN‑ul; trimite‑le doar celor invitați.

## 11. Întărire + AIR (1.4)

- **Hub mai sigur** — PIN aleatoriu de 6 cifre, cheie nouă la fiecare conexiune, limite pentru conexiuni neautentificate și mărimea mesajelor, blocare după PIN‑uri greșite repetate și confirmare înainte de aplicarea oricărui link de invitație.
- **LUMEN (lumină)** — telefonul Regiei clipește cu lanterna; alt telefon privește cu camera din spate. Camera slab luminată, 1–3 m, lanterna îndreptată spre cameră. Lent (câteva secunde), dar trece prin orice zgomot.
- **SONUS (sunet)** — difuzorul redă un cod de tonuri, aproape ultrasonic (inaudibil pentru majoritatea adulților) sau audibil; celălalt telefon ascultă cu microfonul. Duce replici și o invitație la hub (adresă și PIN).
- **TACTUS (vibrații)** — motorul de vibrații trimite pulsuri scurte și lungi; alt telefon așezat pe aceeași suprafață le simte cu accelerometrul. Merge în liniște și în întuneric, duce replici scurte.
- **Folosire** — Setări → *Legături prin aer*: pornește receptoarele dorite (cameră / microfon / accelerometru; telefonul cere permisiunea), apoi pe telefonul Regiei apasă un buton de trimitere. Pachetele au o etichetă scurtă de autentificare din cheia de rețea și nu pot fi reluate.

## 12. Control spectacol (1.5)

- **Același moment** — începe textul replicii cu `@3.5;` ca să pornească peste 3,5 s pe toate telefoanele deodată. Telefoanele își sincronizează ceasul cu hubul la conectare și în fiecare minut; o replică întârziată cu peste 10 s se aruncă, nu pornește depășită.
- **PANICĂ** — replica *PANICĂ: oprește tot* oprește sunerie, vibrații, voce, efecte sonore, ecrane negre și colorate, apeluri false și replicile programate. Merge și prin AIR.
- **Culoare** — `#RRGGBB;secunde;pulsuri pe secundă` umple ecranul. Pulsurile sunt plafonate la 2 pe secundă pentru spectatorii fotosensibili; apăsarea lungă pe ecran îl închide.
- **Sondaj** — `întrebare;opțiunea 1;opțiunea 2;…` întreabă spectatorii și actorii; fiecare telefon are un vot (se poate schimba). *Încheie sondajul* arată rezultatul telefonului Regiei. Sondajele pot fi pași în scenariu.
- **Livrare** — după fiecare replică, Regia vede *trimisă la N din M telefoane conectate*. **PTT hardware** — Setări → *Buton PTT hardware* face din butonul căștilor și volum + un push‑to‑talk cât timp aplicația e în față.

---

[← README](../README.ro.md)
