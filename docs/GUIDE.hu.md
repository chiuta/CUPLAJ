# CUPLAJ — Felhasználói útmutató

[English](GUIDE.md) · [Română](GUIDE.ro.md) · [Français](GUIDE.fr.md) · [Italiano](GUIDE.it.md) · [Español](GUIDE.es.md) · [Português](GUIDE.pt.md) · [Deutsch](GUIDE.de.md) · **Magyar**

_Verzió: 1.3.0 · Utolsó frissítés: 2026-11-24_

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

---

[← README](../README.hu.md)
