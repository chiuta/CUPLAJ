# CUPLAJ

[English](README.md) · **Română** · [Français](README.fr.md) · [Italiano](README.it.md) · [Español](README.es.md) · [Português](README.pt.md) · [Deutsch](README.de.md) · [Magyar](README.hu.md)

<p align="center"><img src="docs/logo.svg" width="160" alt="CUPLAJ"></p>

> **Telefonie cu cuplaj pentru teatru imersiv**

_Versiune: 1.10.0 · Ultima actualizare: 2026-11-26_

CUPLAJ transformă o flotă de telefoane Android obișnuite într-o centrală telefonică privată pentru teatrul imersiv. Organizatorii și actorii vorbesc cu spectatorii (și între ei) prin Wi‑Fi-ul local al locației — fără cloud, fără conturi, fără reclame. Funcția lui de bază este *cuplajul*: **linia comună** a telefoanelor fixe din anii '80 până pe la 2006, în care doi abonați împărțeau aceeași linie, sunau cu cadențe diferite și se puteau auzi unul pe altul.

**[Descarcă APK-ul](https://github.com/chiuta/CUPLAJ/releases/download/v1.10.0/CUPLAJ-1.10.0.apk) · [Descarcă pentru iPhone/iPad (.ipa)](https://github.com/chiuta/CUPLAJ/releases/download/v1.10.0/CUPLAJ-1.10.0-ios-unsigned.ipa) · [Toate versiunile](https://github.com/chiuta/CUPLAJ/releases)**

## Funcții

- **Șase epoci de telefon** — 1985 disc rotativ, 1995 taste, 2003 mobil (SMS 160), 2025 smartphone, 2045 dispozitive purtabile (apeluri șoptite, scuturi ca să răspunzi), 2080 minte comună (fără tastatură). Regia schimbă epoca în direct sau trimite o *călătorie în timp* care le parcurge pe toate.
- **Linie cuplată** — două telefoane împart o linie; un apel către unul sună pe ambele cu cadența abonatului chemat; vecinul care ridică primul preia apelul; dacă ridici receptorul în timpul convorbirii vecinului, asculți și poți chiar interveni.
- **Fără infrastructură** — un telefon (Regia) este hubul. Ceilalți intră prin Wi‑Fi sau prin hotspotul hubului; hubul se găsește automat; cod de cameră și PIN opționale.
- **Voce** — apeluri reale (8 kHz), anunț apasă‑și‑vorbește de la regie, canal backstage pentru echipă.
- **Indicii** — text pe tot ecranul, vibrație, sunet de apel, bliț, difuzor, schimbare de epocă, contact nou, voce vorbită (sinteză locală), efecte sonore, ecran negru, apel fantomă.
- **Scenariu** — listă de indicii cu GO și PLAY (cronometrat), reordonare, import/export JSON.
- **Unelte de regie** — numere, grupuri, cuplare/decuplare, perechi automate, ascultare discretă, mut, uitare, nivel baterie, jurnal de evenimente, stare păstrată la repornire, limitare de flux.
- **8 limbi și accesibilitate** — engleză, română, franceză, italiană, spaniolă, portugheză, germană, maghiară; mărime text reglabilă; răspuns de pe ecranul blocat; rulează în fundal.
- **Internet și date mobile (1.1)** — conectare cu `host` sau `host:port`; cheie de rețea opțională AES‑128‑GCM; compresie G.711 automată în afara rețelei locale; keepalive.
- **Apeluri directe P2P (1.1)** — sunetul merge de la telefon la telefon prin UDP cu hole punching, cu revenire la releul hubului; Regia le poate opri, limita la rețeaua locală (implicit) sau permite oriunde.
- **Poartă VoIP / SIP (1.1)** — telefonul Regiei se înregistrează la un furnizor SIP sau o centrală; echipa formează `0`+număr pentru exterior, apelurile SIP primite sună la un număr intern.
- **Rețea mobilă (1.1)** — poartă SMS prin SIM‑ul telefonului Regiei (trimitere și primire), cue de *apel real* care deschide formatorul și indicator Wi‑Fi / mobil.
- **Mai multe moduri de conectare (1.2)** — Bluetooth (fără router, circa 7 telefoane), Wi‑Fi Direct (telefonul Regiei își face propria rețea Wi‑Fi) și descoperire mDNS, pe lângă Wi‑Fi, hotspot, tethering USB, Ethernet și date mobile.
- **Plug and play (1.3)** — telefonul Regiei deschide singur toate legăturile potrivite; celelalte telefoane apasă *Găsește și alătură‑mă automat* sau ating un link de invitație. Adaugă Wi‑Fi Aware.
- **Întărit și fără rețea (1.4)** — audit cu public ostil, cu remedieri, și CUPLAJ‑AIR: trei protocoale originale care duc replici și invitații fără nicio rețea (lanternă → cameră, difuzor → microfon, vibrație → accelerometru).
- **Control spectacol (1.5)** — replici care pornesc la același moment, replică PANICĂ, culoare pe tot ecranul, sondaje cu publicul, confirmare de livrare și buton PTT hardware. Comparat cu Unity Intercom, Zello, Augmented Theatre, QLab/SOLOstudio și Livecube în `docs/COMPARISON.md`.
- **Tot ce din comparație încape într‑o aplicație doar Android, fără internet (1.6)** — prioritate PA (ducking), OSC intrare și ieșire pentru pupitre de lumini și sunet, și replici cu imagine, video și clip audio trimise prin hub.
- **Spectacol inteligent (1.7)** — zone care urmăresc publicul prin sunet, reguli („când intră cineva în zona 3, trimite‑i…”), contor de public, efecte de voce, subtitrări, text multilingv și personalizat, ramificarea poveștii.
- **Instrumente de scenă (1.8)** — lumini Art‑Net/DMX, MIDI, val de cue‑uri, telefonul fără fir, secret fragmentat, casetofon‑fantomă, cue de siguranță (lumini de sală), Morse prin vibrații, raport după spectacol.
- **iPhone și iPad** — client complet și gazdă (Regie) în Swift, același protocol ca pe Android, deci se amestecă liber în același spectacol; se construiește din sursă cu Xcode (`ios/`).

## Pornire rapidă

1. Ia APK-ul din fila *Actions* (artefactul `CUPLAJ-apk`) sau de pe pagina *Releases* și instalează-l pe toate telefoanele.
2. Pe telefonul Regiei pornește hotspotul (sau pune toate telefoanele în același Wi‑Fi).
3. Pe telefonul Regiei alege **Regie** → *Pornește hubul*. Notează PIN-ul pentru actori și, opțional, un cod de cameră.
4. Pe celelalte alege **Spectator** sau **Actor**, selectează hubul din listă (sau scrie adresa lui) și conectează-te.
5. Fiecare telefon primește un număr (regia 100, actorii 201…, spectatorii 6 cifre). Ridică receptorul și sună.

> Android poate avertiza pentru aplicațiile instalate din afara Google Play (Play Protect): apasă „Mai multe detalii”, apoi „Instalează oricum”. Actualizările merg doar între versiuni semnate cu aceeași cheie.

### Pornire rapidă pe iPhone și iPad

1. Descarcă `CUPLAJ-<versiune>-ios-unsigned.ipa` de pe pagina *Releases*. Nu este semnat, deci iOS nu îl deschide direct.
2. Instalează-l cu **AltStore** sau **Sideloadly** folosind Apple ID-ul tău (un ID gratuit ține 7 zile și permite 3 aplicații; reinstalezi când expiră) sau construiește-l singur: deschide `ios/` în Xcode (`xcodegen generate`), alege echipa și rulează.
3. Pe iOS 16 și mai nou activează *Setări → Confidențialitate și securitate → Mod dezvoltator* și repornește. Dacă iOS spune că dezvoltatorul nu e de încredere, acordă încredere la *Setări → General → VPN și gestionarea dispozitivului*.
4. La prima pornire permite **Rețea locală** (ca să găsești hubul), **Microfon** (apeluri) și **Notificări** (alerte de apel). Permite camera doar pentru cue-urile de lumină.
5. Intră în același Wi‑Fi cu telefonul de regie sau în hotspotul lui. Și un iPhone poate fi regia: pornește *Hotspot personal*, alege **Regie** → *Pornește hubul* și lasă-i pe ceilalți să se conecteze la acel hotspot.
6. Alege hubul din listă (sau scrie adresa lui) și conectează-te. Ține aplicația deschisă, cu ecranul pornit (*Ecran și luminozitate → Blocare automată → Niciodată* pe durata spectacolului); telefonul de regie să rămână la încărcător.
7. Indisponibile pe iOS: legătura Bluetooth, Wi‑Fi Direct și gateway-ul silențios de SMS și apeluri. Aplicația pentru iPhone nu a fost încă încercată pe telefoane reale.

## Cele șase epoci

- **1985 · disc** — un disc rotund pe care îl rotești de-adevăratelea; impulsurile se aud; două sunete lungi de apel.
- **1995 · taste** — tonuri DTMF, carnet cu numere, mesaje de biper.
- **2003 · mobil** — nume în agendă, identificarea apelantului, SMS limitat la 160 de caractere.
- **2025 · smartphone** — oamenii pe primul loc: atingi un contact recent ca să suni; aspect modern, închis la culoare; chat până la 500 de caractere; un sunet blând de marimba.
- **2045 · dispozitive purtabile** — fără tastatură, suni alegând un nume; soneria e un ticăit abia auzit; scuturi telefonul sau atingi ecranul ca să răspunzi sau să închizi; mesaje șoptite până la 200 de caractere.
- **2080 · minte comună** — fără telefon: te conectezi la o minte din rază; un ton care crește lent în loc de sonerie; gânduri de cel mult 60 de caractere.

## Cum funcționează cuplajul

<p align="center"><img src="docs/partyline.svg" width="620" alt="party line"></p>

1. Două telefoane sunt *cuplate* de regie (sau automat, în perechi). Ele împart linia N, pozițiile 1 și 2.
2. Un apel către abonatul 1 sună pe ambele; abonatul 1 aude cadența numărului său (un sunet lung), abonatul 2 pe a sa (două sunete scurte).
3. Cine răspunde preia apelul; celălalt telefon încetează să sune.
4. Cât timp linia e folosită, vecinul care ridică receptorul aude convorbirea, iar cei care vorbesc aud un mic clic.
5. Un ascultător poate *interveni* și vorbi; un apel către un telefon cuplat a cărui linie e ocupată primește ton de ocupat.
6. Regia poate monitoriza orice linie discret (fără clic).

## Documentație

- [Ghid de utilizare](docs/GUIDE.ro.md)
- [Protocol](docs/PROTOCOL.md) *(English)*
- [Audit adversarial](docs/AUDIT.md) *(English)*
- [Istoric modificări](CHANGELOG.md) *(English)*
- [Recenzie onestă și comparație](docs/REVIEW.md) *(English)*
- [Site de prezentare](https://chiuta.github.io/CUPLAJ/)

## Stadiul real

Logica hubului (apeluri, cuplaj, permisiuni, limite, persistență) este acoperită de peste 250 de verificări automate care rulează pe JVM. Interfața Android și calea audio au trecut verificarea de tipuri față de SDK-ul Android 14, dar **nu au fost încă testate pe telefoane reale**; APK-ul este construit de GitHub Actions. Încearcă cu câteva telefoane înainte de spectacol. Traficul din rețeaua locală nu este criptat (PIN-ul și codul de cameră țin la distanță străinii de bună-credință, nu sunt criptografie). Rețelistica din versiunea 1.1 (cifrare, P2P, SIP, SMS) e acoperită de teste de hub contra unui server SIP simulat, dar **nu** a fost încercată pe operatori, NAT‑uri sau SIM‑uri reale. Legăturile Bluetooth, Wi‑Fi Direct și mDNS (1.2) folosesc același strat de transport, testat pe JVM cu canale simulate, dar **nu** au fost încercate pe dispozitive reale; comportamentul diferă între producători. Plug and play (conectare automată, invitații, Wi‑Fi Aware, 1.3) **nu** a fost testat pe dispozitive; formatul invitațiilor e acoperit de teste JVM. Remedierile din auditul 1.4 și codecurile AIR sunt acoperite de teste JVM (zgomot simulat, derivă de ceas, cheie greșită, interferență); codul pentru lanternă, cameră, microfon, difuzor, motor și accelerometru **nu** este testat pe telefoane reale, iar raza și viteza reale vor diferi. Funcțiile 1.5 sunt acoperite de teste JVM (estimarea ceasului, întârzieri, regulile sondajului, limitele fotosensibile); suprapunerile de ecran, butonul căștilor și acuratețea reală a ceasului pe telefoane **nu** sunt testate pe dispozitive. Funcțiile 1.6 sunt acoperite de teste JVM (curba de ducking, fuzzing și inundare OSC, integritatea transferului și siguranța căilor, releul hubului); redarea imaginilor, a videoului și a clipurilor, selectorul de fișiere și dialogurile din setări **nu** sunt testate pe dispozitive. Zonele, regulile, contorul, efectele de voce și limbile sunt acoperite de teste JVM (motorul de reguli, regulile hubului, agregarea contorului, filtrele de voce măsurate pe frecvență); semnalele sonore reale în interior, măsurătorile cu microfonul și accelerometrul și calitatea vocii pe telefoane **nu** sunt testate pe dispozitive. Un semnal înregistrat poate fi redat o dată pe număr de secvență, deci zonele sunt pentru atmosferă, nu pentru securitate. Pachetele Art‑Net, tranzițiile DMX, mesajele MIDI, modelele Morse, raportul și logica hubului pentru val, telefonul fără fir, secret și raport sunt acoperite de teste JVM; casetofonul‑fantomă, pupitrele de lumini și dispozitivele MIDI reale și tot ce ține de telefoane reale nu sunt testate. **iOS:** nucleul Swift este testat pe macOS în CI, iar aplicația se construiește pentru simulator, dar **nu a fost încercată pe iPhone‑uri reale sau cu un furnizor SIP real**.

## Compilare

Necesită JDK 17. `./gradlew assembleRelease` produce `app/build/outputs/apk/release/app-release.apk` (semnat cu cheia de depanare, instalabil direct). Teste unitare: `./gradlew test`. Fluxul din `.github/workflows` face ambele, cu unelte Node ≥ 24 pe Ubuntu 26.04.

## Confidențialitate

Fără statistici, fără servere, fără conturi. Sunetul și mesajele circulă doar în rețeaua locală, între telefoane și hub. Hubul ține numerele, grupurile și cuplajele pe telefonul Regiei, ca o repornire să nu le piardă.

## Sprijină proiectul

CUPLAJ este gratuit și trade‑free. Dacă ajută spectacolul tău, îi poți oferi autorului o cafea: <https://buymeacoffee.com/echo.of.the.strings>.

## Licență

MIT © Alexandru‑Ionuț Chiuță (Alexio).
