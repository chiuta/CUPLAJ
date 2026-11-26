# CUPLAJ

[English](README.md) · [Română](README.ro.md) · [Français](README.fr.md) · [Italiano](README.it.md) · [Español](README.es.md) · [Português](README.pt.md) · [Deutsch](README.de.md) · **Magyar**

<p align="center"><img src="docs/logo.svg" width="160" alt="CUPLAJ"></p>

> **Közös vonalas telefonálás immerzív színházhoz**

_Verzió: 1.10.0 · Utolsó frissítés: 2026-11-26_

A CUPLAJ hétköznapi Android-telefonok flottáját alakítja magán telefonközponttá immerzív színház számára. A szervezők és a színészek a helyszín helyi Wi‑Fi hálózatán beszélnek a nézőkkel (és egymással) — felhő, fiók és reklám nélkül. Névadó funkciója a *cuplaj*: az 1980-as évektől nagyjából 2006-ig használt vezetékes telefonok **közös vonala**, amelyen két előfizető osztozott, eltérő ritmusban csengettek, és hallhatták egymást.

**[APK letöltése](https://github.com/chiuta/CUPLAJ/releases/download/v1.10.0/CUPLAJ-1.10.0.apk) · [Letöltés iPhone/iPad-re (.ipa)](https://github.com/chiuta/CUPLAJ/releases/download/v1.10.0/CUPLAJ-1.10.0-ios-unsigned.ipa) · [Minden kiadás](https://github.com/chiuta/CUPLAJ/releases)**

## Funkciók

- **Hat telefonkorszak** — 1985 tárcsás, 1995 gombos, 2003 mobil (SMS 160), 2025 okostelefon, 2045 viselhető (suttogó hívások, rázással felvétel), 2080 közös elme (billentyűzet nélkül). A rendezői élőben vált korszakot, vagy *időutazást* indít, amely mindegyiken végigmegy.
- **Közös vonal (cuplaj)** — két telefon osztozik egy vonalon; az egyiknek szóló hívás mindkettőt csörgeti a hívott előfizető ritmusában; aki előbb felveszi, azé a hívás; ha a szomszéd beszélgetése közben felveszed a kagylót, belehallgathatsz, sőt közbe is szólhatsz.
- **Infrastruktúra nélkül** — egy telefon (a rendezés) a hub. A többiek Wi‑Fi-n vagy a hub hotspotján csatlakoznak; a hubot automatikusan megtalálják; teremkód és PIN választható.
- **Hang** — valódi hívások (8 kHz), nyomva-tartásos hangosbemondás a rendezéstől, kulissza-csatorna a stábnak.
- **Jelzések** — teljes képernyős szöveg, rezgés, csengetés, vaku, hangszóró, korszakváltás, új névjegy, beszélt hang (helyi szövegfelolvasás), hangeffektek, fekete képernyő, szellemhívás.
- **Forgatókönyv** — jelzéslista GO és PLAY (időzített) gombbal, átrendezés, JSON import/export.
- **Rendezői eszközök** — számok, csoportok, párosítás/szétválasztás, automatikus párok, csendes belehallgatás, némítás, törlés, akkumulátorszint, eseménynapló, újraindítást túlélő állapot, forgalomkorlátozás.
- **8 nyelv és hozzáférhetőség** — angol, román, francia, olasz, spanyol, portugál, német, magyar; állítható szövegméret; hívásfogadás a zárolt képernyőről; háttérben is fut.
- **Internet és mobiladat (1.1)** — csatlakozás `host` vagy `host:port` megadásával; opcionális AES‑128‑GCM hálózati kulcs; automatikus G.711 tömörítés a helyi hálózaton kívül; keepalive.
- **Közvetlen P2P hívások (1.1)** — a hang UDP‑n, lyukasztással megy telefonról telefonra, szükség esetén a hub közvetítőjére visszaesve; a rendező kikapcsolhatja, helyi hálózatra korlátozhatja (alap) vagy bárhová engedheti.
- **VoIP / SIP átjáró (1.1)** — a rendezői telefon bejelentkezik egy SIP‑szolgáltatóhoz vagy alközpontba; a stáb `0`+számot tárcsáz kifelé, a bejövő SIP‑hívások egy belső számon csengenek.
- **Mobilhálózat (1.1)** — SMS‑átjáró a rendezői telefon SIM‑jén át (küldés és fogadás), *valódi hívás* cue, amely megnyitja a tárcsázót, és Wi‑Fi / mobil jelző.
- **További csatlakozási módok (1.2)** — Bluetooth (router nélkül, kb. 7 telefon), Wi‑Fi Direct (a rendezői telefon saját Wi‑Fi hálózatot hoz létre) és mDNS‑felderítés, a Wi‑Fi, hotspot, USB‑megosztás, Ethernet és mobiladat mellett.
- **Plug and play (1.3)** — a rendezői telefon magától megnyit minden megfelelő kapcsolatot; a többiek az *Automatikus keresés és csatlakozás* gombot vagy egy meghívó linket érintenek meg. Újdonság: Wi‑Fi Aware.
- **Megerősítve és hálózat nélkül (1.4)** — ellenséges közönségre méretezett átvizsgálás javításokkal, és a CUPLAJ‑AIR: három saját protokoll, amely jelzéseket és meghívókat visz teljesen hálózat nélkül (zseblámpa → kamera, hangszóró → mikrofon, rezgés → gyorsulásmérő).
- **Előadásvezérlés (1.5)** — egyszerre induló jelzések, PÁNIK jelzés, teljes képernyős szín, közönségszavazás, kézbesítési visszajelzés és hardveres PTT gomb. Összevetés a Unity Intercom, Zello, Augmented Theatre, QLab/SOLOstudio és Livecube alkalmazásokkal: `docs/COMPARISON.md`.
- **Minden az összehasonlításból, ami belefér egy csak Androidos, offline appba (1.6)** — PA elsőbbség (ducking), OSC be- és kimenet fény- és hangpultokhoz, valamint kép-, videó- és hangklip-jelzések a hubon át.
- **Okos előadás (1.7)** — hanggal követő zónák, szabályok („ha valaki belép a 3. zónába, küldd neki…”), közönségmérő, hangeffektek, feliratok, többnyelvű és személyre szabott szöveg, a történet elágaztatása.
- **Színpadi eszközök (1.8)** — Art‑Net/DMX fények, MIDI, jelzéshullám, süket telefon, szétosztott titok, szellemmagnó, biztonsági jelzés (nézőtéri fények), Morze rezgéssel, előadás utáni jelentés.
- **iPhone és iPad** — teljes kliens és házigazda (rendezői) Swiftben, ugyanaz a protokoll, mint Androidon: egy előadáson belül szabadon keverhetők; forrásból építhető Xcode‑dal (`ios/`).

## Gyors kezdés

1. Szerezd meg az APK-t az *Actions* fülről (`CUPLAJ-apk` artefaktum) vagy a *Releases* oldalról, és telepítsd minden telefonra.
2. A rendezés telefonján kapcsold be a hotspotot (vagy tedd az összes telefont ugyanarra a Wi‑Fi-re).
3. A rendezés telefonján válaszd a **Rendezés** → *Hub indítása* lehetőséget. Jegyezd fel a színészek PIN-jét és szükség esetén a teremkódot.
4. A többin válaszd a **Néző** vagy **Színész** szerepet, jelöld ki a hubot a listában (vagy írd be a címét), és csatlakozz.
5. Minden telefon kap egy számot (rendezés 100, színészek 201…, nézők 6 jegyű). Vedd fel a kagylót, és hívj.

> Az Android figyelmeztethet a Google Playen kívül telepített appokra (Play Protect): koppints a További részletekre, majd a Telepítés mindenképp gombra. A frissítés csak azonos kulccsal aláírt verziók között működik.

### Gyorsindítás iPhone‑on és iPaden

1. Töltsd le a `CUPLAJ-<verzió>-ios-unsigned.ipa` fájlt a *Releases* oldalról. Nincs aláírva, ezért az iOS nem nyitja meg közvetlenül.
2. Telepítsd **AltStore**‑ral vagy **Sideloadly**‑val az Apple ID‑ddal (az ingyenes 7 napig érvényes és 3 appot enged; lejáratkor telepítsd újra), vagy építsd meg magad: nyisd meg az `ios/` mappát Xcode‑ban (`xcodegen generate`), válaszd ki a csapatot és futtasd.
3. iOS 16‑tól kapcsold be a *Beállítások → Adatvédelem és biztonság → Fejlesztői mód* opciót, és indítsd újra. Ha az iOS nem megbízható fejlesztőt jelez, bízz meg benne: *Beállítások → Általános → VPN és eszközkezelés*.
4. Az első indításkor engedélyezd a **Helyi hálózatot** (a hub megtalálásához), a **Mikrofont** (hívások) és az **Értesítéseket** (hívásjelzés). A kamerát csak a fényjelzésekhez engedélyezd.
5. Csatlakozz a rendezői telefon Wi‑Fi‑jéhez vagy hotspotjához. iPhone is lehet a rendezői: kapcsold be a *Személyes hotspotot*, válaszd a **Rendezői** → *Hub indítása* opciót, és a többiek csatlakozzanak ahhoz.
6. Válaszd ki a hubot a listából (vagy írd be a címét) és csatlakozz. Tartsd nyitva az appot bekapcsolt képernyővel (*Kijelző és fényerő → Automatikus zárolás → Soha* az előadás alatt); a rendezői telefon maradjon töltőn.
7. iOS‑en nem érhető el: Bluetooth‑kapcsolat, Wi‑Fi Direct és a néma SMS‑ és hívásátjáró. Az iPhone‑appot még nem próbálták ki valódi telefonon.

## A hat korszak

- **1985 · tárcsás** — valódi tárcsa, amit tekerni kell; az impulzusok hallhatók; két hosszú csengetés.
- **1995 · gombos** — DTMF hangok, számjegyzet, csipogó-üzenetek.
- **2003 · mobil** — nevek a telefonkönyvben, hívóazonosítás, legfeljebb 160 karakteres SMS.
- **2025 · okostelefon** — az emberek az elsők: koppints egy legutóbbi névjegyre a híváshoz; modern, sötét megjelenés; csevegés legfeljebb 500 karakterrel; lágy marimba‑csengőhang.
- **2045 · viselhető eszközök** — nincs billentyűzet, névválasztással hívsz; a csengés alig hallható tikkelés; rázd meg a telefont vagy koppints a képernyőre a felvételhez és a letevéshez; suttogott üzenetek legfeljebb 200 karakterrel.
- **2080 · közös elme** — nincs kézibeszélő: egy hatókörön belüli elmére hangolódsz; csengés helyett lassan erősödő hang; legfeljebb 60 karakteres gondolatok.

## Hogyan működik a közös vonal

<p align="center"><img src="docs/partyline.svg" width="620" alt="party line"></p>

1. A rendezés *párosít* két telefont (vagy automatikusan párokat képez). Az N. vonalon osztoznak, az 1. és 2. helyen.
2. Az 1. előfizetőnek szóló hívás mindkettőt csörgeti; az 1. előfizető a saját számának ritmusát hallja (egy hosszú csengetés), a 2. a sajátját (két rövid).
3. Aki felveszi, azé a hívás; a másik telefon abbahagyja a csörgést.
4. Amíg a vonal foglalt, a szomszéd, aki felveszi a kagylót, hallja a beszélgetést, a beszélők pedig halk kattanást hallanak.
5. A belehallgató *közbeszólhat*; a párosított telefonra érkező hívás foglalt vonalnál foglaltjelzést kap.
6. A rendezés bármelyik vonalat csendben (kattanás nélkül) megfigyelheti.

## Dokumentáció

- [Felhasználói útmutató](docs/GUIDE.hu.md)
- [Protokoll](docs/PROTOCOL.md) *(English)*
- [Ellenséges szemléletű audit](docs/AUDIT.md) *(English)*
- [Változásnapló](CHANGELOG.md) *(English)*
- [Őszinte értékelés és összehasonlítás](docs/REVIEW.md) *(English)*
- [Bemutatóoldal](https://chiuta.github.io/CUPLAJ/)

## Őszinte állapot

A hub logikáját (hívások, közös vonal, jogosultságok, korlátok, tartós állapot) több mint 250 automatikus ellenőrzés fedi le JVM-en. Az Android-felület és a hangút típusellenőrzésen átment az Android 14 SDK ellen, de **valódi eszközön még nem tesztelték**; az APK-t a GitHub Actions építi. Előadás előtt próbáld ki néhány telefonnal. A helyi hálózati forgalom nincs titkosítva (a PIN és a teremkód a jóhiszemű kívülállókat tartja távol, nem kriptográfia). Az 1.1‑es hálózati funkciókat (titkosítás, P2P, SIP, SMS) szimulált SIP‑szerver elleni hubtesztek fedik, de **nem** próbáltuk valódi szolgáltatóval, NAT‑tal vagy SIM‑mel. A Bluetooth, Wi‑Fi Direct és mDNS kapcsolatok (1.2) ugyanazt a szállítási réteget használják, amelyet JVM‑en szimulált csatornákkal teszteltünk, de **nem** próbáltuk valódi eszközön; a viselkedés gyártónként eltér. A plug and play (automatikus csatlakozás, meghívók, Wi‑Fi Aware, 1.3) **nincs** eszközökön tesztelve; a meghívó formátumát JVM‑tesztek fedik. Az 1.4‑es átvizsgálás javításait és az AIR kodekeket JVM‑tesztek fedik (szimulált zaj, órajel‑sodródás, hibás kulcs, interferencia); a zseblámpa, kamera, mikrofon, hangszóró, motor és gyorsulásmérő kódja **nincs** valódi telefonokon tesztelve, a valós hatótávolság és sebesség eltérhet. Az 1.5 funkciókat JVM-tesztek fedik (órabecslés, késleltetések, szavazási szabályok, fényérzékenységi korlátok); a képernyő-rátétek, a headset gomb és a valós órapontosság telefonokon **nincs** tesztelve. Az 1.6 funkciókat JVM-tesztek fedik (ducking görbe, OSC fuzzing és áradat, átviteli sértetlenség és útvonal-biztonság, hub-relé); a kép/videó/klip lejátszása, a fájlválasztó és a beállító ablakok **nincs** eszközökön tesztelve. A zónákat, szabályokat, mérőt, hangeffekteket és nyelveket JVM-tesztek fedik (szabálymotor, hub-szabályok, mérő-összesítés, frekvenciában mért hangszűrők); a valódi hangjelzők beltérben, a mikrofon- és gyorsulásmérő-mérések és a hangminőség telefonokon **nincs** eszközökön tesztelve. Egy felvett jelzés sorszámonként egyszer visszajátszható: a zónák hangulatra valók, nem biztonságra. Az Art‑Net csomagokat, DMX áttűnéseket, MIDI üzeneteket, Morze‑mintákat, a jelentést és a hub logikáját (hullám, süket telefon, titok, jelentés) JVM‑tesztek fedik; a szellemmagnó, a valódi fénypultok és MIDI‑eszközök, és minden, ami valódi telefonon történik, nem tesztelt. **iOS:** a Swift‑mag tesztelése macOS‑en, CI‑ben történik, az app szimulátorra lefordul, de **valódi iPhone‑on és valódi SIP‑szolgáltatóval még nem próbálták ki**.

## Építés

JDK 17 szükséges. A `./gradlew assembleRelease` az `app/build/outputs/apk/release/app-release.apk` fájlt állítja elő (a hibakereső kulccsal aláírva, közvetlenül telepíthető). Egységtesztek: `./gradlew test`. A `.github/workflows` munkafolyamat mindkettőt elvégzi, Node ≥ 24 eszközökkel Ubuntu 26.04 rendszeren.

## Adatvédelem

Nincs analitika, nincs szerver, nincs fiók. A hang és az üzenetek csak a helyi hálózatotokon, a telefonok és a hub között haladnak. A hub a számokat, csoportokat és párosításokat a rendezés telefonján tárolja, hogy újraindítás után se vesszenek el.

## Támogasd a projektet

A CUPLAJ ingyenes és trade‑free. Ha segít az előadásodban, meghívhatod a szerzőt egy kávéra: <https://buymeacoffee.com/echo.of.the.strings>.

## Licenc

MIT © Alexandru‑Ionuț Chiuță (Alexio).
