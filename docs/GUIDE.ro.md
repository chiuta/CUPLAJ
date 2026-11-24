# CUPLAJ — Ghid de utilizare

[English](GUIDE.md) · **Română** · [Français](GUIDE.fr.md) · [Italiano](GUIDE.it.md) · [Español](GUIDE.es.md) · [Português](GUIDE.pt.md) · [Deutsch](GUIDE.de.md) · [Magyar](GUIDE.hu.md)

_Versiune: 1.8.0 · Ultima actualizare: 2026-11-24_

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

## 13. Media, OSC, prioritate (1.6)

- **Prioritate PA** — cât vorbește telefonul Regiei (sau un actor) către toți, restul sunetelor de pe telefoane coboară la 30 % și revin lin. Setări → *Prioritate PA* o oprește.
- **Replici media** — pe telefonul Regiei, fila Replici → *Trimite un fișier…* alege o imagine, un video sau un audio (până la 8 MB) și îl trimite pe bucăți telefoanelor alese; fiecare telefon verifică mărimea și suma de control și ignoră un fișier deteriorat. Apoi folosește tipurile *Imagine pe ecran*, *Video pe ecran* sau *Clip audio* (lasă textul gol pentru ultimul fișier, sau `cod;secunde`). Apăsarea lungă închide imaginea sau videoul. Telefoanele care intră mai târziu nu au fișierul: trimite‑l din nou.
- **OSC ieșire** — Setări → *OSC*: fiecare replică se trimite și ca `/cuplaj/cue <către> <tip> <text>` prin UDP către un pupitru de lumini sau sunet, QLab sau Companion.
- **OSC intrare** — pupitrul poate trimite `/cuplaj/<PIN hub>/go`, `/panic`, `/stop` sau `/cue <către> <tip> <text>` telefonului Regiei (port implicit 8473). PIN‑ul din adresă e singura protecție: ține rețeaua spectacolului privată. Se acceptă cel mult 20 de mesaje pe secundă.
- **Nu sunt incluse** — iOS (doar Android), program feed și mai multe canale decât grupurile existente.

## 14. Spectacol inteligent (1.7)

- **Zone** — pune un telefon într‑o încăpere (sau în buzunarul unui personaj), Setări → *Legături prin aer* → *Semnal de zonă* și apasă până arată numărul zonei 1–9: emite din 7 în 7 s un cod aproape ultrasonic, inaudibil pentru cei mai mulți. Telefoanele publicului cu *Ascult: SONUS* pornit raportează ce zonă aud; telefonul Regiei arată 📍 lângă nume. 30 s de tăcere înseamnă că au plecat.
- **Reguli** — telefonul Regiei, Setări → *Reguli*, câte una pe linie: `zone 3 once > self | text | Bun venit, {name}`, `pollwin 1 > all | color | #00ff00;3`, `meter noise 70 > all | blackout | 1;`, `pollwin 2 > all | goto | Finalul B`. *self* este persoana care a declanșat regula; `goto` sare în scenariu la un pas după nume sau număr, deci un sondaj poate alege povestea.
- **Contor de public** — în fila Replici: *Măsoară zgomotul* sau *mișcarea* 5 s pe telefoanele alese. Fiecare telefon trimite doar un număr 0–100, niciodată sunet. Vezi media, vârful și câte au răspuns, iar regulile pot reacționa.
- **Efecte de voce** — alege telefoane în lista Regiei, *Efect de voce…*: robot, radio vechi, ecou, grav, ascuțit, lo‑fi. Efectul se aplică pe telefonul respectiv înainte de trimitere, deci toți cei care îl aud primesc aceeași voce.
- **Subtitrări și limbi** — tipul *Subtitrare* arată subtitrări mari; orice replică text poate purta `en=Hello|ro=Salut|fr=Bonjour` și fiecare telefon o folosește pe a lui (apoi engleza, apoi prima). `{name}` și `{number}` devin numele și numărul telefonului care primește.

## 15. Instrumente de scenă (1.8)

- **Lumini și muzică** — cue‑urile *dmx* (`0;1=255,2-4=128;2000` = universul 0, canalul 1 la maxim, canalele 2–4 la jumătate, tranziție de 2 s) și *midi* (`note 60 127`, `cc 7 100`, `pc 5`, `mmc play`) rulează pe telefonul Regiei: Setări → *Ieșire Art‑Net / DMX* trimite Art‑Net (UDP 6454) către un nod sau pupitru din aceeași rețea Wi‑Fi; MIDI merge la primul dispozitiv MIDI USB/Bluetooth găsit.
- **Val** — începe textul unui cue cu `~150;` (după un eventual `@3;`) și hubul îl trimite telefoanelor pe rând, în ordinea numerelor, la 150 ms: un val prin sală.
- **Telefonul fără fir** — *Instrumente → Telefonul fără fir*: scrii un mesaj și numerele telefoanelor în ordine; fiecare vede ce a transmis cel dinainte, îl poate schimba și îl transmite mai departe. Regia primește originalul și toate deformările.
- **Secret fragmentat** — o frază e împărțită cuvânt cu cuvânt pe telefoane; cine spune primul fraza întreagă (Setări → *Spune fraza secretă*) o rezolvă; Regia află cine, iar declanșatorul de regulă `solved` poate porni un cue (de ex. se deschide o ușă).
- **Casetofon‑fantomă** — înregistrezi până la 60 s din microfonul unui telefon pe hub și le redai mai târziu oricărui grup, ca o voce de nicăieri (sursa rezervată 253).
- **Lumini de sală** — cue *house*: oprește tot (ca PANICA), vibrează, arată un ecran alb și un mesaj calm. **Morse** — cue *morse* vibrează un text în cod Morse.
- **Raport** — *Instrumente → Raport*: un fișier Markdown cu totaluri, apeluri/mesaje/intrări în zone/baterie pe telefon și cronologia completă, partajat cu orice aplicație.
- **Limite și siguranță (1.8)** — un val nu durează mai mult de 20 s (pasul se scurtează în săli mari; cel mult 4 valuri simultan); un telefon din telefonul fără fir care nu răspunde în 90 s e sărit; un secret fragmentat cere o frază de cel puțin 2 cuvinte și 4 litere latine sau cifre (verificarea ignoră majusculele, diacriticele și punctuația); o singură redare de casetofon la un moment dat; Morse e tăiat la 45 s; Art‑Net nu trimite nimic până la primul cue *dmx* (deci nu poate stinge luminile doar fiindcă l‑ai pornit) și ține până la 8 universuri; *dmx* și *midi* merg doar pe telefonul Regiei (cue‑uri și scenarii de acolo), nu ca acțiuni de regulă.

## 16. Referință cue‑uri (toate tipurile)

- **Prefixe** — `@3.5;` declanșează cue‑ul după 3,5 s pe toate telefoanele deodată; `~150;` (după `@…;`) îl trimite telefoanelor pe rând, la 150 ms; `en=Hello|ro=Salut` alege limba fiecărui telefon; `{name}` și `{number}` se înlocuiesc pe fiecare telefon.
- **`text`** — Text pe ecran: Textul afișat pe tot ecranul
- **`vibrate`** — Vibrație: Durata în ms (ex. 800)
- **`ring`** — Sunet de apel: (nu e nevoie de text)
- **`flash`** — Bliț: Durata în ms (ex. 1500)
- **`speaker`** — Difuzor: 1 = pornit, 0 = oprit
- **`contact`** — Adaugă contact: Nume;număr (ex. Mama;204455)
- **`era`** — Schimbă epoca: 1985, 1995 sau 2003
- **`say`** — Voce vorbită: Textul de rostit
- **`sound`** — Efect sonor: bell, siren, knock, static (clopot, sirenă, ciocănit, statică)
- **`blackout`** — Ecran negru: 1;text sau 0
- **`fakecall`** — Apel fantomă: Nume;număr;cuvinte rostite
- **`realcall`** — Apel real (deschide formatorul): Numărul de format, ex. +40721000111
- **`panic`** — PANICĂ: oprește tot: (fără text)
- **`color`** — Culoare pe tot ecranul: #RRGGBB;secunde;pulsuri pe secundă (max 2)
- **`poll`** — Sondaj (votul publicului): întrebare;opțiunea 1;opțiunea 2;… (2–6 opțiuni)
- **`pollend`** — Încheie sondajul și arată rezultatul: (fără text)
- **`image`** — Imagine pe ecran: cod fișier;secunde (trimite întâi fișierul; gol = ultimul)
- **`video`** — Video pe ecran: cod fișier;secunde (trimite întâi fișierul; gol = ultimul)
- **`clip`** — Clip audio: cod fișier;secunde (trimite întâi fișierul; gol = ultimul)
- **`caption`** — Subtitrare pe ecran: text, sau en=Hello|ro=Salut|fr=Bonjour (fiecare telefon arată limba lui; {name} = numele telefonului)
- **`dmx`** — Lumini: Art‑Net/DMX (telefonul Regiei): univers;canal=valoare,interval=valoare;fade ms — ex. 0;1=255,2-4=128;2000
- **`midi`** — Ieșire MIDI (telefonul Regiei): note 60 127 [can] · off 60 [can] · cc 7 100 [can] · pc 5 [can] · mmc play|stop|record
- **`morse`** — Cod Morse prin vibrații: textul de transmis (litere și cifre; ~ 60 de caractere)
- **`house`** — Lumini de sală (siguranță: oprire + ecran alb): (opțional) mesajul afișat pe ecranul alb
- **Declanșatoare și acțiuni de reguli** — O regulă pe linie: ‹când› > ‹cine› | ‹tip replică› | ‹text›. Când: `zone 3 [once]`, `pollwin 0` (opțiunea câștigătoare, de la 0), `meter noise|motion 70` (media ≥ 70). Cine: all, staff, spectators, actors, self (cel care a declanșat), num:/id:/grp:. Tipul `goto` sare la un pas din scenariu după nume sau număr. Textul poate folosi {name}. Și `solved` (un secret fragmentat a fost ghicit).

## 17. Ecrane și butoane (exact ca în aplicație)

- **Voce** — `ANUNȚ la toate telefoanele` · `DOAR CU ECHIPA`
- **Regie** — `Număr` · `Grup` · `Cuplează 2` · `Decuplează` · `Perechi automate` · `Ascultă` · `Mut` · `Uită` · `Jurnal` · `Sună` · `Epoca …` · `Vecin ascultă: …` · `Clic la ridicare: …` · `Mesaje între telefoane: …` · `Apeluri directe (P2P): …` · `Trimite indiciul` · `Măsoară zgomotul (5 s)` · `Măsoară mișcarea (5 s)` · `Efect de voce pentru telefoanele alese…` · `Trimite un fișier (imagine, video, audio) telefoanelor alese…` · `Instrumente: lanț, secret, casetofon, raport`
- **Scenariu** — `Adaugă pasul` · `GO ▶` · `⏮ Început` · `Șterge` · `Exportă` · `Importă`
- **Setări (meniul Aa)** — `MĂRIMEA TEXTULUI` · `Păstrează aplicația activă (optimizare baterie)` · `🔍 Găsește și alătură‑mă automat` · `Caută prin Bluetooth` · `Caută Wi‑Fi Direct` · `Fă telefonul vizibil prin Bluetooth (5 min)` · `Trimite invitație pentru spectatori` · `Trimite invitație pentru actori` · `Linie VoIP / SIP…` · `Legături prin aer: lumină, sunet, vibrații` · `Semnal de zonă (acest telefon, difuzor): …` · `OSC: pupitre de lumini / sunet, QLab…` · `Ieșire Art‑Net / DMX (lumini)` · `Reguli: zonă, sondaj, public → replică` · `Prioritate PA: restul sunetelor coboară cât vorbește Regia` · `Buton PTT hardware: pornit (buton căști, volum +)` · `Spune fraza secretă` · `Resetează memoria hubului (numere, cuplaje)` · `Despre CUPLAJ` · `Ieși`
- Setările și opțiunile Regiei arată și starea (pornit/oprit).

---

[← README](../README.ro.md)
