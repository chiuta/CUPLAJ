# CUPLAJ — Felhasználói útmutató

[English](GUIDE.md) · [Română](GUIDE.ro.md) · [Français](GUIDE.fr.md) · [Italiano](GUIDE.it.md) · [Español](GUIDE.es.md) · [Português](GUIDE.pt.md) · [Deutsch](GUIDE.de.md) · **Magyar**

_Verzió: 1.1.0 · Utolsó frissítés: 2026-11-24_

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

---

[← README](../README.hu.md)
