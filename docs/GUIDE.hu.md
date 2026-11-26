# CUPLAJ — Felhasználói útmutató

[English](GUIDE.md) · [Română](GUIDE.ro.md) · [Français](GUIDE.fr.md) · [Italiano](GUIDE.it.md) · [Español](GUIDE.es.md) · [Português](GUIDE.pt.md) · [Deutsch](GUIDE.de.md) · **Magyar**

_Verzió: 1.10.0 · Utolsó frissítés: 2026-11-26_

## 1. Az előadás előtt

- Használj egyetlen Wi‑Fi hálózatot minden telefonnak, vagy a rendezés telefonjának hotspotját. Kapcsold ki a routereken a *kliensizolációt*.
- Telepítsd az APK-t, add meg a mikrofon- és értesítési engedélyt, és az **Aa** menüben válaszd az *Az app maradjon aktív* lehetőséget, hogy a rendszer ne altassa el.
- Töltsd fel az összes telefont; a rendezés mutatja az akkumulátorszinteket, és 20 % alatt figyelmeztet.

## 2. Szerepek és számok

- **Rendezés** — a hub; mindent lát, jelzéseket küld, telefonokat párosít. A száma 100.
- **Színész** — PIN kell hozzá; beszélhet a stábbal és bárkit hívhat. Számok 201-től.
- **Néző** — hívásokat, üzeneteket és jelzéseket kap; csak a stábhoz beszélhet. Hatjegyű számok.
- A rendezés bármelyik számot módosíthatja, és csoportokat rendelhet hozzá (például *erkély*, *pince*).

## 3. A telefon használata

- **1985** — vedd fel a kagylót (gomb), várd meg a hangot, tekerd a tárcsát; a szám rövid szünet után magától tárcsázódik.
- **1995** — vedd fel, majd használd a gombokat; a jegyzet a rendezés által hozzáadott számokat sorolja fel.
- **2003** — írd be a számot, és nyomd meg a *Hívás* gombot; az üzenetek legfeljebb 160 karakteresek.
- A csörgő telefon az értesítésből is felvehető, zárolt képernyőn is.

## 4. A közös vonal a gyakorlatban

- A Rendezés fülön jelölj ki két eszközt, és nyomd meg a *2 összekapcsol* gombot, vagy az *Auto párok* gombot az összes szabad telefon párosításához.
- A rendezésen a *Belehallgat* gombbal úgy figyelhetsz meg egy vonalat, hogy nem hallanak.
- A beállítások kikapcsolhatják a szomszéd hallgatózását, a kattanást vagy a telefonok közti üzeneteket.

## 5. Jelzések

- Válassz célt (mindenki, nézők, színészek, kijelölt eszközök, csoport, szám), típust és szöveget, majd nyomd meg a *Jelzés küldése* gombot.
- Példák: `Anya;204455` névjegyet ad hozzá; `1;Mindjárt éjfél` feketére váltja a képernyőt halvány szöveggel; `Dr. Voss;204455;Gyertek a pincébe` szellemhívást indít, amely felvételkor megszólal.

## 6. Forgatókönyv

- Építsd fel az előadást lépésekből: cél, típus, szöveg, késleltetés. A **GO** a következő lépést küldi; a **PLAY** a kijelölt lépéstől automatikusan lejátssza a többit.
- Exportáld a forgatókönyvet JSON-ként, és importáld egy másik rendezői telefonon.

## 7. Hibaelhárítás

- *Nem található hub* — ellenőrizd, hogy ugyanazon a hálózaton vagytok-e, kapcsold ki a kliensizolációt, vagy írd be a hub címét.
- *Hibás PIN / teremkód* — a rendezés mindkettőt megmutatja a kezdőképernyőn.
- *Foglalt port* — zárd be azt az alkalmazást, amely a 8470-es portot használja.
- *Néma hívások* — ellenőrizd a mikrofonengedélyt és a Hang fülön a hangszóró kapcsolóját.

## 8. Internet, VoIP és mobilhálózat (1.1)

- **Hálózati kulcs** — a kezdőképernyőn kapcsold be a *Titkosított kapcsolat* opciót; a rendező kulcsot mutat (xxxx‑xxxx‑xxxx‑xxxx). Írd be a többi telefon *Hálózati kulcs* mezőjébe. Helyes kulcs nélkül a hub elutasítja a kapcsolatot. Használd, ha a forgalom elhagyja a helyi hálózatot.
- **Internetről** — a routeren irányítsd át a TCP 8470 és UDP 8472 portot a rendezői telefonra, és add meg a nyilvános címet (`host` vagy `host:port`). Mobiladaton semmi más nem kell.
- **P2P** — a Rendező fülön a *Közvetlen hívások* opció kikapcsolt / csak helyi hálózat / bárhol között vált. A *bárhol* lehetővé teszi, hogy két telefon megtudja egymás nyilvános IP‑jét; közvetlen hívást a rendező nem tud hallgatni, elnémítani vagy megszakítani (szükség esetén automatikusan visszavált a közvetítőre).
- **SIP vonal** — Beállítások → *VoIP / SIP vonal…*: szerver, felhasználó, jelszó, opcionális tartomány és a bejövő hívások belső száma (alap 100). Az alkalmazáshoz indítsd újra a hubot. A stáb `0` + külső számot tárcsáz; a nézők nem hívhatnak kifelé. Egy hívás legfeljebb 15 percig tarthat. A SIP és az RTP nem titkosított.
- **SMS** — Beállítások → *SMS‑átjáró* (SIM és SMS‑jogosultság kell). A stáb az Üzenetek fülön az *SMS valódi számra* célt választja; a fogadott SMS‑ek *SMS* feladótól érkező üzenetként jelennek meg. Korlát: percenként 20. A szolgáltató díjai érvényesek.
- **Valódi hívás** — a *valódi hívás* cue megnyitja a tárcsázót a számmal. Az Android nem engedi, hogy egy alkalmazás mobilhívást vezessen a közös vonalra: a GSM‑hang nem hidalható át; ehhez használd a SIP vonalat.

## 9. Csatlakozás router nélkül (1.2)

- **Ugyanaz a hub, bármilyen kapcsolat** — a hívások, közös vonal, cue‑k, titkosítás és PIN mindenhol ugyanúgy működnek; csak a telefon útja változik a hubig.
- **Hotspot, USB‑megosztás, Ethernet** — minden kapcsolat, amely IP‑hálózatot ad a telefonoknak, beállítás nélkül működik; add meg a címet, vagy hagyd, hogy a hub megtalálható legyen.
- **Bluetooth** — a rendezői telefonon indítsd a hubot *Bluetooth kapcsolat: BE* mellett, és nyomd meg a *Láthatóvá tétel Bluetoothon* gombot. A többin nyomd meg a *Keresés Bluetoothon* gombot, és válaszd a rendezői telefont (🔵). A hatótáv kicsi, egy hubra kb. 7 telefon fér; a hang automatikusan tömörítve megy. Párosítás nem szükséges; a hozzáférést továbbra is a PIN, a szobakód és a hálózati kulcs védi.
- **Wi‑Fi Direct** — a rendezői telefonon indítsd a hubot *Wi‑Fi Direct csoport: BE* mellett. A többin nyomd meg a *Wi‑Fi Direct keresése* gombot, és válaszd a rendezői telefont (📡); az kérheti minden telefon jóváhagyását. Csatlakozás után a hub megjelenik a listában. A telefonok általában nem maradhatnak a megszokott Wi‑Fi‑n Wi‑Fi Direct csoportban, és egyes típusok nem tudják mindkettőt.
- **mDNS** — a hub mDNS/DNS‑SD‑vel is jelzi magát (🌐), így azokon a hálózatokon is megtalálható, amelyek tiltják a szokásos szórást.
- **Nincs benne** — a Wi‑Fi Aware (kevés telefon támogatja) és a Google Nearby Connections (Google Play Services kell, amit ez a trade‑free app elkerül). A Bluetooth Low Energy túl lassú a beszédhez.

## 10. Plug and play (1.3)

- **Rendezői telefon** — válaszd a *Rendező* szerepet, majd a *Hub indítása* gombot. Bekapcsolt *Plug & play* mellett (alap) a CUPLAJ megnyitja a Wi‑Fi / hotspot, mDNS, Bluetooth és Wi‑Fi Aware kapcsolatot, és Wi‑Fi Direct csoportot hoz létre, ha nincs Wi‑Fi hálózat. Fogadd el az egyetlen jogosultságkérést. A beállítások mutatják az aktív kapcsolatokat.
- **A többi telefon, legegyszerűbben** — Beállítások → *Meghívó küldése nézőknek* (vagy *színészeknek*) a rendezői telefonon; küldd el a linket bármely appon. Aki telepített CUPLAJ‑jal megérinti, az beállítódik (cím, kulcs, PIN, szoba) és csatlakozik.
- **A többi telefon, link nélkül** — válassz szerepet, és érintsd meg az *Automatikus keresés és csatlakozás* gombot. A CUPLAJ egyszerre keres minden kapcsolaton (Wi‑Fi, mDNS, Bluetooth, Wi‑Fi Direct, Wi‑Fi Aware), és a legjobb hubhoz csatlakozik: előbb a helyi hálózat, utoljára a Bluetooth.
- **Wi‑Fi Aware** — a támogató telefonok router és beállítás nélkül megtalálják egymást. Sok telefon nem támogatja; semmi sem romlik el, ha a tiéd nem.
- **Biztonság** — meghívóval csak néző vagy színész hozható létre, rendezői telefon soha. A linkek tartalmazzák a kulcsot és a PIN‑t; csak a meghívottaknak küldd.

## 11. Megerősítés + AIR (1.4)

- **Biztonságosabb hub** — véletlen 6 jegyű PIN, minden kapcsolathoz új kulcs, korlátok a nem hitelesített kapcsolatokra és az üzenetméretre, zárolás ismételt hibás PIN után, és megerősítés bármely meghívó link alkalmazása előtt.
- **LUMEN (fény)** — a rendezői telefon villogtatja a zseblámpát; egy másik telefon a hátlapi kamerával figyeli. Halvány fény, 1–3 m, a lámpa a kamerára irányítva. Lassú (néhány másodperc), de robusztus.
- **SONUS (hang)** — a hangszóró hangkódot játszik le, közel ultrahangosat (a legtöbb felnőtt nem hallja) vagy hallhatót; a másik telefon mikrofonnal figyel. Jelzéseket és hub‑meghívót (cím és PIN) visz.
- **TACTUS (rezgés)** — a rezgőmotor rövid és hosszú impulzusokat küld; egy ugyanazon a felületen fekvő másik telefon gyorsulásmérővel érzékeli. Csendben és sötétben is működik, rövid jelzésekhez.
- **Használat** — Beállítások → *Légi kapcsolatok*: kapcsold be a kívánt figyelőket (kamera / mikrofon / gyorsulásmérő; a telefon engedélyt kér), majd a rendezői telefonon nyomj egy küldő gombot. A csomagok a hálózati kulcsból képzett rövid hitelesítő címkét hordoznak, és nem játszhatók vissza.

## 12. Előadásvezérlés (1.5)

- **Ugyanaz a pillanat** — kezdd a szöveget `@3.5;` előtaggal, és a jelzés 3,5 mp múlva egyszerre indul minden telefonon. A telefonok csatlakozáskor és percenként szinkronizálják az óráját a hubbal; a 10 mp-nél későbbi jelzés elvetésre kerül.
- **PÁNIK** — a *PÁNIK: mindent leállít* jelzés leállítja a csengést, rezgést, beszédet, hangeffekteket, fekete és színes képernyőket, hamis hívásokat és az időzített jelzéseket. AIR-en is működik.
- **Szín** — `#RRGGBB;másodperc;impulzus/másodperc` kitölti a képernyőt. Az impulzusok a fényérzékeny nézők miatt másodpercenként 2-re korlátozottak; hosszú nyomás bezárja.
- **Szavazás** — `kérdés;1. lehetőség;2. lehetőség;…` megkérdezi a nézőket és színészeket; telefononként egy szavazat (módosítható). A *Szavazás lezárása* az eredményt a rendezői telefonon mutatja. A szavazás a forgatókönyv lépése is lehet.
- **Kézbesítés** — minden jelzés után a rendező látja: *N / M online telefonra elküldve*. **Hardveres PTT** — Beállítások → *Hardveres PTT gomb*: a headset gomb és a hangerő + adó-gombbá válik, amíg az app előtérben van.

## 13. Média, OSC, elsőbbség (1.6)

- **PA elsőbbség** — amíg a rendezői telefon (vagy egy színész) mindenkihez beszél, a többi hang 30%-ra halkul, majd finoman visszatér. Beállítások → *PA elsőbbség* kikapcsolja.
- **Média jelzések** — a rendezői telefonon a Jelzések fül → *Fájl küldése…* kiválaszt egy képet, videót vagy hangot (legfeljebb 8 MB), és darabokban elküldi a kijelölt telefonoknak; mindegyik ellenőrzi a méretet és az ellenőrző összeget, a sérült fájlt eldobja. Utána használd a *Kép a képernyőn*, *Videó a képernyőn* vagy *Hangklip* jelzést (üres szöveg = utolsó fájl, vagy `kód;másodperc`). Hosszú nyomás bezárja a képet/videót. A később csatlakozók nem kapják meg a fájlt: küldd újra.
- **OSC kimenet** — Beállítások → *OSC*: minden jelzés `/cuplaj/cue <cél> <típus> <szöveg>` üzenetként is megy UDP-n egy fény- vagy hangpultra, QLab-ra vagy Companionra.
- **OSC bemenet** — a pult `/cuplaj/<hub PIN>/go`, `/panic`, `/stop` vagy `/cue <cél> <típus> <szöveg>` üzenetet küldhet a rendezői telefonnak (alap port: 8473). A címben lévő PIN az egyetlen védelem: tartsd privátban az előadás hálózatát. Másodpercenként legfeljebb 20 üzenet fogadható.
- **Nem része** — iOS (csak Android), program feed és a meglévő csoportoknál több csatorna.

## 14. Okos előadás (1.7)

- **Zónák** — tegyél egy telefont egy szobába (vagy egy szereplő zsebébe), Beállítások → *Légi kapcsolatok* → *Zónajelző*, és koppints, amíg az 1–9 szám megjelenik: 7 mp-enként közel ultrahangos kódot ad, amit a legtöbben nem hallanak. A *Figyel: SONUS* bekapcsolt közönségtelefonok jelentik a hallott zónát; a rendező a név mellett 📍-t lát. 30 mp csend = elmentek.
- **Szabályok** — rendezői telefon, Beállítások → *Szabályok*, soronként egy: `zone 3 once > self | text | Üdv, {name}`, `pollwin 1 > all | color | #00ff00;3`, `meter noise 70 > all | blackout | 1;`, `pollwin 2 > all | goto | B vég`. A *self* az, aki kiváltotta a szabályt; a `goto` név vagy szám szerint a forgatókönyv egy lépésére ugrik: egy szavazás választhat történetet.
- **Közönségmérő** — Jelzések fül: *Zaj* vagy *mozgás mérése* 5 mp-ig a kijelölt telefonokon. Mindegyik csak egy 0–100 számot küld, hangot soha. Látod az átlagot, a csúcsot és hányan válaszoltak; a szabályok reagálhatnak.
- **Hangeffektek** — jelölj ki telefonokat a rendezői listában, *Hangeffekt…*: robot, régi rádió, visszhang, mély, magas, lo‑fi. Az effekt azon a telefonon, küldés előtt érvényesül: mindenki ugyanazt a hangot hallja.
- **Feliratok és nyelvek** — a *Felirat* jelzés nagy feliratot mutat; bármely szöveges jelzés hordozhat `en=Hello|ro=Salut|fr=Bonjour` formát, és minden telefon a saját nyelvét használja (aztán angol, aztán az első). A `{name}` és `{number}` a fogadó telefon nevévé és számává válik.

## 15. Színpadi eszközök (1.8)

- **Fény és zene** — a *dmx* (`0;1=255,2-4=128;2000`) és *midi* (`note 60 127`, `cc 7 100`, `pc 5`, `mmc play`) jelzések a rendező telefonján futnak: Beállítások → *Art‑Net / DMX kimenet* Art‑Netet (UDP 6454) küld egy node‑nak vagy pultnak ugyanazon a Wi‑Fi‑n; a MIDI az első talált USB/Bluetooth MIDI‑eszközre megy.
- **Hullám** — kezdd a jelzés szövegét `~150;`-nel: a hub egymás után küldi a telefonoknak, szám szerinti sorrendben, 150 ms különbséggel.
- **Süket telefon** — *Eszközök → Süket telefon*: üzenet és telefonszámok sorrendben; mindenki látja, mit adott át az előző, megváltoztathatja és továbbadja. A rendező megkapja az eredetit és minden torzulást.
- **Szétosztott titok** — egy mondatot szavanként szétosztunk; aki elsőként mondja a teljes mondatot (Beállítások → *Mondd a titkos mondatot*), megfejti; a `solved` szabály‑kioldó jelzést indíthat.
- **Szellemmagnó** — egy telefon mikrofonjából legfeljebb 60 mp-et rögzít a hubon, és később lejátssza egy csoportnak, mint a semmiből jövő hangot (fenntartott forrás: 253).
- **Nézőtéri fények** — *house* jelzés: mindent leállít (mint a PÁNIK), rezeg, fehér képernyő és nyugodt üzenet. **Morze** — a *morse* jelzés egy szöveget Morze‑kóddal rezeg.
- **Jelentés** — *Eszközök → Jelentés*: Markdown fájl összesítéssel, telefononkénti hívás/üzenet/zónabelépés/akku adatokkal és teljes idővonallal.
- **Korlátok és biztonság (1.8)** — egy hullám sosem tart tovább 20 mp-nél (nagy termekben a lépés rövidül; egyszerre legfeljebb 4 hullám); a süket telefonban a 90 mp alatt nem válaszoló telefont átugorja; a szétosztott titokhoz legalább 2 szavas, 4 latin betűt vagy számjegyet tartalmazó mondat kell (az ellenőrzés figyelmen kívül hagyja a kis‑/nagybetűt, ékezetet és írásjelet); egyszerre egy magnólejátszás; a Morze 45 mp-nél megszakad; az Art‑Net az első *dmx* jelzésig semmit sem küld (így bekapcsolással nem sötétítheti el a fényeket), és legfeljebb 8 univerzumot kezel; a *dmx* és *midi* csak a rendezői telefonon működik (jelzések és forgatókönyvek), szabályműveletként nem.

## 16. Jelzések referenciája (minden típus)

- **Előtagok** — az `@3.5;` 3,5 mp múlva, minden telefonon egyszerre indítja a jelzést; a `~150;` (az `@…;` után) egymás után küldi a telefonoknak, 150 ms különbséggel; az `en=Hello|ro=Salut` minden telefonnak a saját nyelvét választja; a `{name}` és `{number}` telefononként cserélődik.
- **`text`** — Szöveg a képernyőn: Teljes képernyős szöveg
- **`vibrate`** — Rezgés: Időtartam ms-ban (pl. 800)
- **`ring`** — Csengetés: (nem kell szöveg)
- **`flash`** — Vaku: Időtartam ms-ban (pl. 1500)
- **`speaker`** — Hangszóró: 1 = be, 0 = ki
- **`contact`** — Névjegy hozzáadása: Név;szám (pl. Anya;204455)
- **`era`** — Korszakváltás: 1985, 1995, 2003, 2025, 2045 vagy 2080 (vagy lista: 1985,2080;4000)
- **`say`** — Beszélt hang: Felolvasandó szöveg
- **`sound`** — Hangeffekt: bell, siren, knock, static (csengő, sziréna, kopogás, zaj)
- **`blackout`** — Fekete képernyő: 1;szöveg vagy 0
- **`fakecall`** — Szellemhívás: Név;szám;felolvasott szöveg
- **`realcall`** — Valódi hívás (megnyitja a tárcsázót): A tárcsázandó szám, pl. +40721000111
- **`panic`** — PÁNIK: mindent leállít: (nincs szöveg)
- **`color`** — Teljes képernyős szín: #RRGGBB;másodperc;impulzus/másodperc (max. 2)
- **`poll`** — Szavazás (közönség): kérdés;1. lehetőség;2. lehetőség;… (2–6)
- **`pollend`** — Szavazás lezárása és eredmény: (nincs szöveg)
- **`image`** — Kép a képernyőn: fájlkód;másodperc (előbb küldd el a fájlt; üres = utolsó)
- **`video`** — Videó a képernyőn: fájlkód;másodperc (előbb küldd el a fájlt; üres = utolsó)
- **`clip`** — Hangklip: fájlkód;másodperc (előbb küldd el a fájlt; üres = utolsó)
- **`caption`** — Felirat a képernyőn: szöveg, vagy en=Hello|ro=Salut|fr=Bonjour (minden telefon a saját nyelvét mutatja; {name} = a telefon neve)
- **`dmx`** — Fények: Art‑Net/DMX (a rendező telefonja): univerzum;csatorna=érték,tartomány=érték;áttűnés ms — pl. 0;1=255,2-4=128;2000
- **`midi`** — MIDI kimenet (a rendező telefonja): note 60 127 [csat.] · off 60 [csat.] · cc 7 100 [csat.] · pc 5 [csat.] · mmc play|stop|record
- **`morse`** — Morze‑kód rezgéssel: küldendő szöveg (betűk és számjegyek; ~ 60 karakter)
- **`house`** — Nézőtéri fények (biztonság: leállítás + fehér képernyő): (opcionális) a fehér képernyőn megjelenő üzenet
- **Szabály‑kioldók és ‑műveletek** — Soronként egy szabály: ‹mikor› > ‹kinek› | ‹jelzés típusa› | ‹szöveg›. Mikor: `zone 3 [once]`, `pollwin 0` (nyertes opció, 0-tól), `meter noise|motion 70` (átlag ≥ 70). Kinek: all, staff, spectators, actors, self (aki kiváltotta), num:/id:/grp:. A `goto` típus név vagy szám szerint a forgatókönyv egy lépésére ugrik. A szöveg használhatja a {name} jelölést. Valamint `solved` (megfejtették a szétosztott titkot).

## 17. Képernyők és gombok (pontosan, ahogy az appban)

- **Hang** — `BEMONDÁS minden telefonra` · `CSAK A STÁBNAK`
- **Rendezés** — `Szám` · `Csoport` · `2 összekapcsol` · `Szétválaszt` · `Auto párok` · `Belehallgat` · `Némít` · `Töröl` · `Napló` · `Hívás` · `… korszak` · `Szomszéd hallgat: …` · `Kattanás felvételkor: …` · `Üzenetek telefonok között: …` · `Közvetlen hívások (P2P): …` · `Jelzés küldése` · `Zaj mérése (5 mp)` · `Mozgás mérése (5 mp)` · `Hangeffekt a kiválasztott telefonokra…` · `Fájl (kép, videó, hang) küldése a kiválasztott telefonokra…` · `Eszközök: lánc, titok, magnó, jelentés`
- **Forgatókönyv** — `Lépés hozzáadása` · `GO ▶` · `⏮ Eleje` · `Törlés` · `Exportálás` · `Importálás`
- **Beállítások (Aa menü)** — `SZÖVEGMÉRET` · `Az app maradjon aktív (akkumulátor-optimalizálás)` · `🔍 Automatikus keresés és csatlakozás` · `Keresés Bluetoothon` · `Wi‑Fi Direct keresése` · `Láthatóvá tétel Bluetoothon (5 perc)` · `Meghívó küldése nézőknek` · `Meghívó küldése színészeknek` · `VoIP / SIP vonal…` · `Légi kapcsolatok: fény, hang, rezgés` · `Zónajelző (ez a telefon, hangszóró): …` · `OSC: fény‑/hangpultok, QLab…` · `Art‑Net / DMX kimenet (fények)` · `Szabályok: zóna, szavazás, közönség → jelzés` · `PA elsőbbség: a többi hang halkul, amikor a rendező beszél` · `Hardveres PTT gomb: be (headset gomb, hangerő +)` · `Mondd a titkos mondatot` · `A hub memóriájának törlése (számok, párosítások)` · `A CUPLAJ-ról` · `Kilépés`
- A beállítások és a rendezői opciók az állapotukat (be/ki) is mutatják.

## 18. iPhone és iPad (iOS)

- Az iOS‑app teljes kliens, és az előadást is képes házigazdaként vezetni (rendezői). Ugyanazt a protokollt beszéli, így az Android‑ és iOS‑telefonok szabadon keverhetők.
- Építés: nyisd meg az `ios/` mappát (XcodeGen: `cd ios && xcodegen generate`), válaszd ki az aláírási csapatot Xcode‑ban, és futtasd. App Store‑os vagy TestFlight‑verzió még nincs.
- Tartalma: hívások és közös vonal, jelzések, forgatókönyv, szavazások, szabályok, számláló, média, hangosítás (PA), Art‑Net, MIDI, OSC, közvetlen P2P hang, SIP‑átjáró, CUPLAJ‑AIR (fény, hang, rezgés) és háttérben futó hang.
- iOS‑en nem lehetséges (nincs nyilvános API): Bluetooth RFCOMM, Wi‑Fi Direct, néma SMS‑ és hívásátjáró. Használd az iPhone hotspotját vagy közös Wi‑Fit; a *valódi hívás* jelzés a tárcsázót nyitja meg.
- Tartsd nyitva az appot bekapcsolt képernyővel, a rendezői telefont pedig töltőn. Eddig csak szimulátoros buildek és automatikus tesztek futottak — **valódi iPhone‑on még nem próbálták ki**.

---

[← README](../README.hu.md)
