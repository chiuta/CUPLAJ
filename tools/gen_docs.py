# -*- coding: utf-8 -*-
"""Generates README.<xx>.md, docs/GUIDE.<xx>.md, docs/index.html from docs_src.py. Run: python3 tools/gen_docs.py"""
import json, os, re, sys
sys.path.insert(0, os.path.dirname(__file__))
from docs_src import D, LANGS, NAMES

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VERSION, DATE = "1.9.0", "2026-11-25"
RELEASE = "1.9.1"  # GitHub release whose files the download links point to (APK/IPA signed/packaged after 1.9.0)
REPO = "https://github.com/chiuta/CUPLAJ"

# ---- generated: full cue reference from the app's own strings (always matches the app)
import csv
_rows = list(csv.reader(open(os.path.join(ROOT, "app/src/main/assets/i18n.tsv"), encoding="utf-8"), delimiter="\t"))
_hdr = _rows[0]; _hdr[0] = _hdr[0].lstrip("# ")
_kinds = re.findall(r'"(\w+)"', re.search(r'val CUE_KINDS = listOf\((.*?)\)', open(os.path.join(ROOT, "app/src/main/kotlin/ro/trom/cuplaj/Tabs.kt"), encoding="utf-8").read()).group(1))
_T = {"en": ("Cue reference (all kinds)", "**Prefixes** — `@3.5;` fires the cue after 3.5 s on every phone at once; `~150;` (after `@…;`) sends it to the phones one after another, 150 ms apart; `en=Hello|ro=Salut` picks each phone's language; `{name}` and `{number}` are replaced on each phone.", "**Rule triggers and actions** — "),
 "ro": ("Referință cue‑uri (toate tipurile)", "**Prefixe** — `@3.5;` declanșează cue‑ul după 3,5 s pe toate telefoanele deodată; `~150;` (după `@…;`) îl trimite telefoanelor pe rând, la 150 ms; `en=Hello|ro=Salut` alege limba fiecărui telefon; `{name}` și `{number}` se înlocuiesc pe fiecare telefon.", "**Declanșatoare și acțiuni de reguli** — "),
 "fr": ("Référence des signaux (tous les types)", "**Préfixes** — `@3.5;` déclenche le signal après 3,5 s sur tous les téléphones à la fois ; `~150;` (après `@…;`) l'envoie aux téléphones l'un après l'autre, à 150 ms ; `en=Hello|ro=Salut` choisit la langue de chaque téléphone ; `{name}` et `{number}` sont remplacés sur chaque téléphone.", "**Déclencheurs et actions de règles** — "),
 "it": ("Riferimento dei cue (tutti i tipi)", "**Prefissi** — `@3.5;` fa partire il cue dopo 3,5 s su tutti i telefoni insieme; `~150;` (dopo `@…;`) lo invia ai telefoni uno dopo l'altro, a 150 ms; `en=Hello|ro=Salut` sceglie la lingua di ogni telefono; `{name}` e `{number}` sono sostituiti su ogni telefono.", "**Trigger e azioni delle regole** — "),
 "es": ("Referencia de señales (todos los tipos)", "**Prefijos** — `@3.5;` lanza la señal a los 3,5 s en todos los teléfonos a la vez; `~150;` (tras `@…;`) la envía a los teléfonos uno tras otro, a 150 ms; `en=Hello|ro=Salut` elige el idioma de cada teléfono; `{name}` y `{number}` se sustituyen en cada teléfono.", "**Disparadores y acciones de reglas** — "),
 "pt": ("Referência de sinais (todos os tipos)", "**Prefixos** — `@3.5;` dispara o sinal após 3,5 s em todos os telemóveis ao mesmo tempo; `~150;` (depois de `@…;`) envia‑o aos telemóveis um a um, com 150 ms; `en=Hello|ro=Salut` escolhe o idioma de cada telemóvel; `{name}` e `{number}` são substituídos em cada telemóvel.", "**Gatilhos e ações de regras** — "),
 "de": ("Cue‑Referenz (alle Arten)", "**Präfixe** — `@3.5;` löst den Cue nach 3,5 s auf allen Telefonen gleichzeitig aus; `~150;` (nach `@…;`) sendet ihn nacheinander an die Telefone, im Abstand von 150 ms; `en=Hello|ro=Salut` wählt die Sprache jedes Telefons; `{name}` und `{number}` werden auf jedem Telefon ersetzt.", "**Regel‑Auslöser und ‑Aktionen** — "),
 "hu": ("Jelzések referenciája (minden típus)", "**Előtagok** — az `@3.5;` 3,5 mp múlva, minden telefonon egyszerre indítja a jelzést; a `~150;` (az `@…;` után) egymás után küldi a telefonoknak, 150 ms különbséggel; az `en=Hello|ro=Salut` minden telefonnak a saját nyelvét választja; a `{name}` és `{number}` telefononként cserélődik.", "**Szabály‑kioldók és ‑műveletek** — ")}
for _l in LANGS:
    _c = _hdr.index(_l)
    _m = {r[0]: re.sub(r"<([^<>]*)>", r"‹\1›", r[_c]) for r in _rows[1:] if len(r) > _c}
    _items = [_T[_l][1]] + ["**`%s`** — %s: %s" % (k, _m["cue_" + k], _m["hint_" + k]) for k in _kinds] + [_T[_l][2] + _m["rules_hint"]]
    D[_l]["g"] = D[_l]["g"] + [(_T[_l][0], _items)]

# ---- generated: every screen and button, with the app's own labels
_UI_T = {"en": ("Screens and buttons (exactly as in the app)", "Settings (Aa menu)", "Settings and Control options also show their state (on/off)."),
 "ro": ("Ecrane și butoane (exact ca în aplicație)", "Setări (meniul Aa)", "Setările și opțiunile Regiei arată și starea (pornit/oprit)."),
 "fr": ("Écrans et boutons (exactement comme dans l'application)", "Réglages (menu Aa)", "Les réglages et options de la régie indiquent aussi leur état (activé/désactivé)."),
 "it": ("Schermate e pulsanti (esattamente come nell'app)", "Impostazioni (menu Aa)", "Impostazioni e opzioni della regia mostrano anche lo stato (attivo/spento)."),
 "es": ("Pantallas y botones (exactamente como en la app)", "Ajustes (menú Aa)", "Los ajustes y opciones de regiduría muestran también su estado (activado/desactivado)."),
 "pt": ("Ecrãs e botões (exatamente como na app)", "Definições (menu Aa)", "As definições e opções da Regia mostram também o estado (ligado/desligado)."),
 "de": ("Bildschirme und Schaltflächen (genau wie in der App)", "Einstellungen (Menü Aa)", "Einstellungen und Regie‑Optionen zeigen auch ihren Zustand (an/aus)."),
 "hu": ("Képernyők és gombok (pontosan, ahogy az appban)", "Beállítások (Aa menü)", "A beállítások és a rendezői opciók az állapotukat (be/ki) is mutatják.")}
_UI = [("tab_voice", ["voice_pa_btn", "voice_back_btn"]),
 ("tab_regie", ["btn_number", "btn_group", "btn_couple", "btn_uncouple", "btn_auto", "btn_listen", "btn_mute", "btn_forget", "btn_journal", "btn_call_sel", "btn_era", "opt_listen", "opt_click", "opt_peermsg", "opt_p2p", "btn_send_cue", "meter_noise_btn", "meter_motion_btn", "fx_btn", "media_send", "tools18"]),
 ("tab_script", ["btn_add_step", "btn_go", "btn_rewind", "btn_del", "btn_export", "btn_import"]),
 (None, ["set_scale", "set_battery", "btn_auto_join", "btn_scan_bt", "btn_scan_wfd", "bt_visible", "btn_invite_sp", "btn_invite_ac", "btn_sip", "set_air", "beacon_btn", "set_osc", "set_dmx", "set_rules", "duck_on", "hw_ptt_on", "guess_btn", "set_reset_hub", "set_about", "btn_exit"])]
for _l in LANGS:
    _c = _hdr.index(_l)
    _m = {r[0]: r[_c] for r in _rows[1:] if len(r) > _c}
    def _lab(k):
        v = _m[k].replace("\\n", " ").replace("%1$s", "…").replace("%s", "…").replace("%d", "…")
        return "`" + v.strip() + "`"
    _items = []
    for _tab, _keys in _UI:
        _h = _m[_tab] if _tab else _UI_T[_l][1]
        _items.append("**%s** — %s" % (_h, " · ".join(_lab(k) for k in _keys)))
    _items.append(_UI_T[_l][2])
    D[_l]["g"] = D[_l]["g"] + [(_UI_T[_l][0], _items)]

# ---- iPhone / iPad (added in 1.9.0)
_IOS = {
 "en": ("**iPhone and iPad** — a full client and host (Control) in Swift, same protocol as Android, so both mix freely in one show; build from source with Xcode (`ios/`).",
  "iPhone and iPad (iOS)",
  ["The iOS app is a full client and can also host the show (Control). It speaks the same protocol, so Android and iOS phones mix freely.",
   "Build: open `ios/` (XcodeGen: `cd ios && xcodegen generate`), set your signing team in Xcode and run. There is no App Store or TestFlight build yet.",
   "Included: calls and party line, cues, script, polls, rules, meter, media, public address, Art‑Net, MIDI, OSC, direct P2P audio, SIP gateway, CUPLAJ‑AIR (light, sound, vibration) and background audio.",
   "Not possible on iOS (no public API): Bluetooth RFCOMM, Wi‑Fi Direct, silent SMS and phone‑call gateway. Use the iPhone hotspot or a shared Wi‑Fi; the *real call* cue opens the dialler.",
   "Keep the app open with the screen on, and the control phone plugged in. Only simulator builds and automated tests have run so far — **not yet tried on real iPhones**."],
  " **iOS:** the Swift core is tested on macOS in CI and the app builds for the simulator, but it has **not been tried on real iPhones or against a real SIP provider**.",
  "Android app (APK) · iPhone/iPad app built from source with Xcode — same protocol, they mix in one show."),
 "ro": ("**iPhone și iPad** — client complet și gazdă (Regie) în Swift, același protocol ca pe Android, deci se amestecă liber în același spectacol; se construiește din sursă cu Xcode (`ios/`).",
  "iPhone și iPad (iOS)",
  ["Aplicația iOS este un client complet și poate găzdui spectacolul (Regie). Vorbește același protocol, deci telefoanele Android și iOS se amestecă liber.",
   "Construire: deschide `ios/` (XcodeGen: `cd ios && xcodegen generate`), alege echipa de semnare în Xcode și rulează. Încă nu există versiune în App Store sau TestFlight.",
   "Include: apeluri și linie comună, cue‑uri, scenariu, sondaje, reguli, contor, media, anunțuri (PA), Art‑Net, MIDI, OSC, audio P2P direct, gateway SIP, CUPLAJ‑AIR (lumină, sunet, vibrații) și audio în fundal.",
   "Imposibil pe iOS (fără API public): Bluetooth RFCOMM, Wi‑Fi Direct, gateway silențios de SMS și apeluri. Folosește hotspotul iPhone‑ului sau un Wi‑Fi comun; cue‑ul *apel real* deschide aplicația de apelare.",
   "Ține aplicația deschisă, cu ecranul pornit, iar telefonul Regie la încărcător. Până acum au rulat doar build‑uri pentru simulator și teste automate — **nu a fost încă încercată pe iPhone‑uri reale**."],
  " **iOS:** nucleul Swift este testat pe macOS în CI, iar aplicația se construiește pentru simulator, dar **nu a fost încercată pe iPhone‑uri reale sau cu un furnizor SIP real**.",
  "Aplicație Android (APK) · aplicație iPhone/iPad construită din sursă cu Xcode — același protocol, se amestecă în același spectacol."),
 "fr": ("**iPhone et iPad** — client complet et hôte (régie) en Swift, même protocole qu'Android : les deux se mélangent librement dans un même spectacle ; compilation depuis les sources avec Xcode (`ios/`).",
  "iPhone et iPad (iOS)",
  ["L'application iOS est un client complet et peut aussi héberger le spectacle (régie). Elle parle le même protocole : téléphones Android et iOS se mélangent librement.",
   "Compilation : ouvrez `ios/` (XcodeGen : `cd ios && xcodegen generate`), choisissez votre équipe de signature dans Xcode et lancez. Il n'y a pas encore de version App Store ni TestFlight.",
   "Inclus : appels et ligne partagée, signaux, scénario, sondages, règles, compteur, médias, sonorisation (PA), Art‑Net, MIDI, OSC, audio P2P direct, passerelle SIP, CUPLAJ‑AIR (lumière, son, vibration) et audio en arrière‑plan.",
   "Impossible sur iOS (pas d'API publique) : Bluetooth RFCOMM, Wi‑Fi Direct, passerelle silencieuse de SMS et d'appels. Utilisez le point d'accès de l'iPhone ou un Wi‑Fi commun ; le signal *appel réel* ouvre le composeur.",
   "Gardez l'application ouverte, écran allumé, et le téléphone de régie branché. Seuls des builds pour simulateur et des tests automatiques ont tourné — **pas encore essayée sur de vrais iPhone**."],
  " **iOS :** le cœur Swift est testé sur macOS en CI et l'application se compile pour le simulateur, mais elle **n'a pas été essayée sur de vrais iPhone ni avec un vrai fournisseur SIP**.",
  "Application Android (APK) · application iPhone/iPad compilée depuis les sources avec Xcode — même protocole, mélange possible dans un même spectacle."),
 "it": ("**iPhone e iPad** — client completo e host (regia) in Swift, stesso protocollo di Android: i due si mescolano liberamente nello stesso spettacolo; si compila dai sorgenti con Xcode (`ios/`).",
  "iPhone e iPad (iOS)",
  ["L'app iOS è un client completo e può anche ospitare lo spettacolo (regia). Parla lo stesso protocollo: telefoni Android e iOS si mescolano liberamente.",
   "Compilazione: apri `ios/` (XcodeGen: `cd ios && xcodegen generate`), scegli il team di firma in Xcode ed esegui. Non esiste ancora una versione App Store o TestFlight.",
   "Incluso: chiamate e linea condivisa, cue, copione, sondaggi, regole, contatore, media, impianto voce (PA), Art‑Net, MIDI, OSC, audio P2P diretto, gateway SIP, CUPLAJ‑AIR (luce, suono, vibrazione) e audio in background.",
   "Impossibile su iOS (nessuna API pubblica): Bluetooth RFCOMM, Wi‑Fi Direct, gateway silenzioso di SMS e chiamate. Usa l'hotspot dell'iPhone o un Wi‑Fi comune; il cue *chiamata reale* apre il tastierino.",
   "Tieni l'app aperta con lo schermo acceso e il telefono di regia in carica. Finora sono girati solo build per simulatore e test automatici — **non ancora provata su iPhone reali**."],
  " **iOS:** il nucleo Swift è testato su macOS in CI e l'app compila per il simulatore, ma **non è stata provata su iPhone reali né con un vero provider SIP**.",
  "App Android (APK) · app iPhone/iPad compilata dai sorgenti con Xcode — stesso protocollo, si mescolano nello stesso spettacolo."),
 "es": ("**iPhone y iPad** — cliente completo y anfitrión (regiduría) en Swift, mismo protocolo que Android: ambos se mezclan libremente en una misma función; se compila desde el código con Xcode (`ios/`).",
  "iPhone e iPad (iOS)",
  ["La app de iOS es un cliente completo y también puede alojar la función (regiduría). Habla el mismo protocolo: teléfonos Android e iOS se mezclan libremente.",
   "Compilación: abre `ios/` (XcodeGen: `cd ios && xcodegen generate`), elige tu equipo de firma en Xcode y ejecuta. Aún no hay versión en App Store ni TestFlight.",
   "Incluye: llamadas y línea compartida, señales, guion, encuestas, reglas, contador, medios, megafonía (PA), Art‑Net, MIDI, OSC, audio P2P directo, pasarela SIP, CUPLAJ‑AIR (luz, sonido, vibración) y audio en segundo plano.",
   "Imposible en iOS (sin API pública): Bluetooth RFCOMM, Wi‑Fi Direct, pasarela silenciosa de SMS y llamadas. Usa el punto de acceso del iPhone o un Wi‑Fi común; la señal *llamada real* abre el marcador.",
   "Mantén la app abierta con la pantalla encendida y el teléfono de regiduría enchufado. Hasta ahora solo se han ejecutado compilaciones para simulador y pruebas automáticas — **aún no se ha probado en iPhone reales**."],
  " **iOS:** el núcleo Swift se prueba en macOS en CI y la app compila para el simulador, pero **no se ha probado en iPhone reales ni con un proveedor SIP real**.",
  "App Android (APK) · app de iPhone/iPad compilada desde el código con Xcode — mismo protocolo, se mezclan en una misma función."),
 "pt": ("**iPhone e iPad** — cliente completo e anfitrião (régie) em Swift, mesmo protocolo do Android: os dois misturam‑se livremente no mesmo espetáculo; compila‑se a partir do código com o Xcode (`ios/`).",
  "iPhone e iPad (iOS)",
  ["A app iOS é um cliente completo e também pode alojar o espetáculo (régie). Fala o mesmo protocolo: telemóveis Android e iOS misturam‑se livremente.",
   "Compilação: abre `ios/` (XcodeGen: `cd ios && xcodegen generate`), escolhe a tua equipa de assinatura no Xcode e executa. Ainda não existe versão na App Store nem no TestFlight.",
   "Inclui: chamadas e linha partilhada, sinais, guião, sondagens, regras, contador, multimédia, sonorização (PA), Art‑Net, MIDI, OSC, áudio P2P direto, gateway SIP, CUPLAJ‑AIR (luz, som, vibração) e áudio em segundo plano.",
   "Impossível no iOS (sem API pública): Bluetooth RFCOMM, Wi‑Fi Direct, gateway silencioso de SMS e chamadas. Usa o hotspot do iPhone ou um Wi‑Fi comum; o sinal *chamada real* abre o marcador.",
   "Mantém a app aberta com o ecrã ligado e o telemóvel da régie ligado ao carregador. Até agora só correram compilações para simulador e testes automáticos — **ainda não foi testada em iPhones reais**."],
  " **iOS:** o núcleo Swift é testado em macOS na CI e a app compila para o simulador, mas **não foi testada em iPhones reais nem com um fornecedor SIP real**.",
  "App Android (APK) · app iPhone/iPad compilada a partir do código com o Xcode — mesmo protocolo, misturam‑se no mesmo espetáculo."),
 "de": ("**iPhone und iPad** — vollständiger Client und Host (Regie) in Swift, gleiches Protokoll wie Android: beide lassen sich in einer Vorstellung frei mischen; Build aus dem Quellcode mit Xcode (`ios/`).",
  "iPhone und iPad (iOS)",
  ["Die iOS‑App ist ein vollständiger Client und kann auch die Vorstellung hosten (Regie). Sie spricht dasselbe Protokoll: Android‑ und iOS‑Telefone lassen sich frei mischen.",
   "Build: `ios/` öffnen (XcodeGen: `cd ios && xcodegen generate`), in Xcode das Signing‑Team wählen und starten. Es gibt noch keinen App‑Store‑ oder TestFlight‑Build.",
   "Enthalten: Gespräche und Gemeinschaftsanschluss, Cues, Ablauf, Umfragen, Regeln, Zähler, Medien, Durchsagen (PA), Art‑Net, MIDI, OSC, direktes P2P‑Audio, SIP‑Gateway, CUPLAJ‑AIR (Licht, Schall, Vibration) und Audio im Hintergrund.",
   "Unter iOS nicht möglich (keine öffentliche API): Bluetooth RFCOMM, Wi‑Fi Direct, stilles SMS‑ und Anruf‑Gateway. Den iPhone‑Hotspot oder ein gemeinsames WLAN nutzen; der Cue *echter Anruf* öffnet die Wähl‑App.",
   "App geöffnet und Bildschirm an lassen, das Regie‑Telefon am Ladegerät. Bisher liefen nur Simulator‑Builds und automatische Tests — **noch nicht auf echten iPhones ausprobiert**."],
  " **iOS:** Der Swift‑Kern wird in der CI auf macOS getestet und die App baut für den Simulator, wurde aber **weder auf echten iPhones noch mit einem echten SIP‑Anbieter ausprobiert**.",
  "Android‑App (APK) · iPhone/iPad‑App aus dem Quellcode mit Xcode gebaut — gleiches Protokoll, in einer Vorstellung mischbar."),
 "hu": ("**iPhone és iPad** — teljes kliens és házigazda (rendezői) Swiftben, ugyanaz a protokoll, mint Androidon: egy előadáson belül szabadon keverhetők; forrásból építhető Xcode‑dal (`ios/`).",
  "iPhone és iPad (iOS)",
  ["Az iOS‑app teljes kliens, és az előadást is képes házigazdaként vezetni (rendezői). Ugyanazt a protokollt beszéli, így az Android‑ és iOS‑telefonok szabadon keverhetők.",
   "Építés: nyisd meg az `ios/` mappát (XcodeGen: `cd ios && xcodegen generate`), válaszd ki az aláírási csapatot Xcode‑ban, és futtasd. App Store‑os vagy TestFlight‑verzió még nincs.",
   "Tartalma: hívások és közös vonal, jelzések, forgatókönyv, szavazások, szabályok, számláló, média, hangosítás (PA), Art‑Net, MIDI, OSC, közvetlen P2P hang, SIP‑átjáró, CUPLAJ‑AIR (fény, hang, rezgés) és háttérben futó hang.",
   "iOS‑en nem lehetséges (nincs nyilvános API): Bluetooth RFCOMM, Wi‑Fi Direct, néma SMS‑ és hívásátjáró. Használd az iPhone hotspotját vagy közös Wi‑Fit; a *valódi hívás* jelzés a tárcsázót nyitja meg.",
   "Tartsd nyitva az appot bekapcsolt képernyővel, a rendezői telefont pedig töltőn. Eddig csak szimulátoros buildek és automatikus tesztek futottak — **valódi iPhone‑on még nem próbálták ki**."],
  " **iOS:** a Swift‑mag tesztelése macOS‑en, CI‑ben történik, az app szimulátorra lefordul, de **valódi iPhone‑on és valódi SIP‑szolgáltatóval még nem próbálták ki**.",
  "Android‑app (APK) · forrásból Xcode‑dal épített iPhone/iPad‑app — ugyanaz a protokoll, egy előadáson belül keverhetők."),
}
for _l in LANGS:
    _f, _gt, _gi, _st, _pl = _IOS[_l]
    D[_l]["feat"] = D[_l]["feat"] + [_f]
    D[_l]["g"] = D[_l]["g"] + [(_gt, _gi)]
    D[_l]["status"] = D[_l]["status"] + _st
    D[_l]["site"]["plat"] = _pl


_DL = {'en': ('Download for iPhone/iPad (.ipa)', 'All releases', 'The .ipa is unsigned: install it with AltStore or Sideloadly using your Apple ID (a free ID lasts 7 days), or build it with Xcode. Android: download the APK.'), 'ro': ('Descarcă pentru iPhone/iPad (.ipa)', 'Toate versiunile', 'Fișierul .ipa nu este semnat: instalează-l cu AltStore sau Sideloadly folosind Apple ID-ul tău (un ID gratuit ține 7 zile) sau construiește-l cu Xcode. Android: descarcă APK-ul.'), 'fr': ('Télécharger pour iPhone/iPad (.ipa)', 'Toutes les versions', "Le .ipa n'est pas signé : installez-le avec AltStore ou Sideloadly via votre identifiant Apple (un compte gratuit dure 7 jours), ou compilez-le avec Xcode. Android : téléchargez l'APK."), 'it': ('Scarica per iPhone/iPad (.ipa)', 'Tutte le versioni', "L'.ipa non è firmato: installalo con AltStore o Sideloadly usando il tuo Apple ID (uno gratuito dura 7 giorni) oppure compilalo con Xcode. Android: scarica l'APK."), 'es': ('Descargar para iPhone/iPad (.ipa)', 'Todas las versiones', 'El .ipa no está firmado: instálalo con AltStore o Sideloadly usando tu Apple ID (uno gratuito dura 7 días) o compílalo con Xcode. Android: descarga el APK.'), 'pt': ('Descarregar para iPhone/iPad (.ipa)', 'Todas as versões', 'O .ipa não está assinado: instala‑o com o AltStore ou o Sideloadly usando o teu Apple ID (um gratuito dura 7 dias) ou compila‑o com o Xcode. Android: descarrega o APK.'), 'de': ('Für iPhone/iPad herunterladen (.ipa)', 'Alle Versionen', 'Die .ipa ist nicht signiert: mit AltStore oder Sideloadly und deiner Apple‑ID installieren (eine kostenlose ID gilt 7 Tage) oder mit Xcode bauen. Android: APK herunterladen.'), 'hu': ('Letöltés iPhone/iPad-re (.ipa)', 'Minden kiadás', 'Az .ipa nincs aláírva: telepítsd AltStore-ral vagy Sideloadly-val az Apple ID-ddal (az ingyenes 7 napig érvényes), vagy építsd Xcode-dal. Android: töltsd le az APK-t.')}
for _l in LANGS:
    D[_l]["site"]["get_ios"], D[_l]["site"]["all_rel"], D[_l]["site"]["ios_dl"] = _DL[_l]
_MED = {'en': ('Screens', "Illustrations drawn from the app's own concepts and layout. They are not captures from a physical phone.", 'Feature demos', ['Spectator: incoming call', 'Actor: lines and script', 'Control: cues, meter, lines', 'Script: cue list', 'CUPLAJ-AIR: no network', 'Poll: live result'], [('Party line', 'A1 calls B1; B1 rings long, its line-mate B2 short; A2 listens in; control cuts in.'), ('Cues', 'Ring, whisper, blackout and sound cues reach every phone.'), ('CUPLAJ-AIR', 'Light, sound and vibration carry a cue with no network.'), ('Polls', 'The audience votes and the control phone sees the result live.')]), 'ro': ('Ecrane', 'Ilustrații făcute după conceptele și aranjarea aplicației. Nu sunt capturi de pe un telefon real.', 'Demonstrații ale funcțiilor', ['Spectator: apel primit', 'Actor: linii și scenariu', 'Regie: cue-uri, contor, linii', 'Scenariu: lista de cue-uri', 'CUPLAJ-AIR: fără rețea', 'Sondaj: rezultat live'], [('Linia comună', 'A1 îl sună pe B1; B1 sună lung, B2 (de pe aceeași linie) scurt; A2 ascultă; regia intervine.'), ('Cue-uri', 'Cue-urile de sunat, șoaptă, blackout și sunet ajung pe toate telefoanele.'), ('CUPLAJ-AIR', 'Lumina, sunetul și vibrația transmit un cue fără rețea.'), ('Sondaje', 'Publicul votează, iar regia vede rezultatul live.')]), 'fr': ('Écrans', "Illustrations dessinées d'après les concepts et la disposition de l'application. Ce ne sont pas des captures d'un vrai téléphone.", 'Démonstrations des fonctions', ['Spectateur : appel entrant', 'Acteur : lignes et scénario', 'Régie : signaux, compteur, lignes', 'Scénario : liste de signaux', 'CUPLAJ-AIR : sans réseau', 'Sondage : résultat en direct'], [('Ligne partagée', 'A1 appelle B1 ; B1 sonne long, B2 (même ligne) court ; A2 écoute ; la régie intervient.'), ('Signaux', 'Les signaux sonnerie, murmure, noir et son atteignent tous les téléphones.'), ('CUPLAJ-AIR', 'Lumière, son et vibration portent un signal sans réseau.'), ('Sondages', 'Le public vote, la régie voit le résultat en direct.')]), 'it': ('Schermate', "Illustrazioni disegnate sui concetti e sul layout dell'app. Non sono catture da un telefono reale.", 'Dimostrazioni delle funzioni', ['Spettatore: chiamata in arrivo', 'Attore: linee e copione', 'Regia: cue, contatore, linee', 'Copione: lista dei cue', 'CUPLAJ-AIR: senza rete', 'Sondaggio: risultato live'], [('Linea condivisa', 'A1 chiama B1; B1 squilla lungo, B2 (stessa linea) breve; A2 ascolta; la regia interviene.'), ('Cue', 'I cue di squillo, sussurro, buio e suono raggiungono tutti i telefoni.'), ('CUPLAJ-AIR', 'Luce, suono e vibrazione portano un cue senza rete.'), ('Sondaggi', 'Il pubblico vota e la regia vede il risultato dal vivo.')]), 'es': ('Pantallas', 'Ilustraciones dibujadas según los conceptos y el diseño de la app. No son capturas de un teléfono real.', 'Demostraciones de funciones', ['Espectador: llamada entrante', 'Actor: líneas y guion', 'Regiduría: señales, contador, líneas', 'Guion: lista de señales', 'CUPLAJ-AIR: sin red', 'Encuesta: resultado en vivo'], [('Línea compartida', 'A1 llama a B1; B1 suena largo, B2 (misma línea) corto; A2 escucha; la regiduría interviene.'), ('Señales', 'Las señales de timbre, susurro, apagón y sonido llegan a todos los teléfonos.'), ('CUPLAJ-AIR', 'Luz, sonido y vibración llevan una señal sin red.'), ('Encuestas', 'El público vota y la regiduría ve el resultado en vivo.')]), 'pt': ('Ecrãs', 'Ilustrações desenhadas a partir dos conceitos e do layout da app. Não são capturas de um telemóvel real.', 'Demonstrações das funções', ['Espectador: chamada recebida', 'Ator: linhas e guião', 'Régie: sinais, contador, linhas', 'Guião: lista de sinais', 'CUPLAJ-AIR: sem rede', 'Sondagem: resultado em direto'], [('Linha partilhada', 'A1 liga a B1; B1 toca longo, B2 (mesma linha) curto; A2 escuta; a régie intervém.'), ('Sinais', 'Os sinais de toque, sussurro, apagão e som chegam a todos os telemóveis.'), ('CUPLAJ-AIR', 'Luz, som e vibração levam um sinal sem rede.'), ('Sondagens', 'O público vota e a régie vê o resultado em direto.')]), 'de': ('Bildschirme', 'Illustrationen nach den Konzepten und dem Aufbau der App. Es sind keine Aufnahmen von einem echten Telefon.', 'Funktionsdemos', ['Zuschauer: eingehender Anruf', 'Darsteller: Leitungen und Ablauf', 'Regie: Cues, Zähler, Leitungen', 'Ablauf: Cue-Liste', 'CUPLAJ-AIR: ohne Netz', 'Umfrage: Live-Ergebnis'], [('Gemeinschaftsanschluss', 'A1 ruft B1 an; B1 klingelt lang, B2 (gleiche Leitung) kurz; A2 hört mit; die Regie schaltet sich ein.'), ('Cues', 'Klingel-, Flüster-, Blackout- und Ton-Cues erreichen alle Telefone.'), ('CUPLAJ-AIR', 'Licht, Schall und Vibration tragen einen Cue ohne Netz.'), ('Umfragen', 'Das Publikum stimmt ab, die Regie sieht das Ergebnis live.')]), 'hu': ('Képernyők', 'A funkciók és az elrendezés alapján rajzolt illusztrációk. Nem valódi telefonról készült képernyőképek.', 'Funkcióbemutatók', ['Néző: bejövő hívás', 'Színész: vonalak és forgatókönyv', 'Rendezői: jelzések, számláló, vonalak', 'Forgatókönyv: jelzéslista', 'CUPLAJ-AIR: hálózat nélkül', 'Szavazás: élő eredmény'], [('Közös vonal', 'A1 hívja B1-et; B1 hosszan, B2 (azonos vonal) röviden csörög; A2 hallgatózik; a rendezői beavatkozik.'), ('Jelzések', 'A csörgés, suttogás, sötétítés és hang jelzések minden telefonra eljutnak.'), ('CUPLAJ-AIR', 'Fény, hang és rezgés hordoz jelzést hálózat nélkül.'), ('Szavazások', 'A közönség szavaz, a rendezői élőben látja az eredményt.')])}
for _l in LANGS:
    _a = _MED[_l]
    D[_l]["site"].update(m_shots=_a[0], m_note=_a[1], m_clips=_a[2], m_sc=_a[3], m_cl=_a[4])
_DG = {'en': {'t1': 'What has really been verified', 'cols': ['Automated tests', 'Builds', 'Real phones'], 'leg': ['Done', 'Partly (formats, packets or simulated channels only)', 'Not done'], 'rows': ['Hub logic: calls, party line, limits', 'Android screens and audio', 'Network: encryption, P2P, SIP, SMS', 'Bluetooth, Wi‑Fi Direct, mDNS', 'Plug and play, invitations', 'CUPLAJ‑AIR: light, sound, vibration', 'Show control (1.5)', 'Media, OSC, public address (1.6)', 'Zones, rules, meter (1.7)', 'Art‑Net, MIDI, audience games (1.8)', 'iPhone and iPad app'], 't2': 'How far each step has gone', 'steps': ['Code written', 'Automated tests pass (JVM and macOS)', 'Builds on CI (APK, iOS simulator)', 'Release published (APK, unsigned IPA)', 'Tried on a handful of real phones', 'Rehearsal and live show']}, 'ro': {'t1': 'Ce a fost verificat cu adevărat', 'cols': ['Teste automate', 'Build-uri', 'Telefoane reale'], 'leg': ['Făcut', 'Parțial (doar formate, pachete sau canale simulate)', 'Nefăcut'], 'rows': ['Logica hubului: apeluri, linie comună, limite', 'Ecrane și audio Android', 'Rețea: cifrare, P2P, SIP, SMS', 'Bluetooth, Wi‑Fi Direct, mDNS', 'Plug and play, invitații', 'CUPLAJ‑AIR: lumină, sunet, vibrații', 'Control de spectacol (1.5)', 'Media, OSC, anunțuri (1.6)', 'Zone, reguli, contor (1.7)', 'Art‑Net, MIDI, jocuri cu publicul (1.8)', 'Aplicația iPhone și iPad'], 't2': 'Cât de departe a ajuns fiecare pas', 'steps': ['Cod scris', 'Teste automate trecute (JVM și macOS)', 'Build-uri în CI (APK, simulator iOS)', 'Release publicat (APK, IPA nesemnat)', 'Încercat pe câteva telefoane reale', 'Repetiție și spectacol live']}, 'fr': {'t1': 'Ce qui a réellement été vérifié', 'cols': ['Tests automatiques', 'Compilations', 'Vrais téléphones'], 'leg': ['Fait', 'Partiel (formats, paquets ou canaux simulés seulement)', 'Pas fait'], 'rows': ['Logique du hub : appels, ligne partagée, limites', 'Écrans et audio Android', 'Réseau : chiffrement, P2P, SIP, SMS', 'Bluetooth, Wi‑Fi Direct, mDNS', 'Plug and play, invitations', 'CUPLAJ‑AIR : lumière, son, vibration', 'Régie de spectacle (1.5)', 'Médias, OSC, sonorisation (1.6)', 'Zones, règles, compteur (1.7)', 'Art‑Net, MIDI, jeux avec le public (1.8)', 'Application iPhone et iPad'], 't2': 'Où en est chaque étape', 'steps': ['Code écrit', 'Tests automatiques réussis (JVM et macOS)', 'Compilations en CI (APK, simulateur iOS)', 'Version publiée (APK, IPA non signé)', 'Essai sur quelques vrais téléphones', 'Répétition et spectacle en direct']}, 'it': {'t1': 'Cosa è stato davvero verificato', 'cols': ['Test automatici', 'Build', 'Telefoni reali'], 'leg': ['Fatto', 'Parziale (solo formati, pacchetti o canali simulati)', 'Non fatto'], 'rows': ["Logica dell'hub: chiamate, linea condivisa, limiti", 'Schermate e audio Android', 'Rete: cifratura, P2P, SIP, SMS', 'Bluetooth, Wi‑Fi Direct, mDNS', 'Plug and play, inviti', 'CUPLAJ‑AIR: luce, suono, vibrazione', 'Regia dello spettacolo (1.5)', 'Media, OSC, impianto voce (1.6)', 'Zone, regole, contatore (1.7)', 'Art‑Net, MIDI, giochi col pubblico (1.8)', 'App iPhone e iPad'], 't2': 'A che punto è ogni passo', 'steps': ['Codice scritto', 'Test automatici superati (JVM e macOS)', 'Build in CI (APK, simulatore iOS)', 'Release pubblicata (APK, IPA non firmato)', 'Provato su alcuni telefoni reali', 'Prova e spettacolo dal vivo']}, 'es': {'t1': 'Lo que se ha verificado de verdad', 'cols': ['Pruebas automáticas', 'Compilaciones', 'Teléfonos reales'], 'leg': ['Hecho', 'Parcial (solo formatos, paquetes o canales simulados)', 'No hecho'], 'rows': ['Lógica del hub: llamadas, línea compartida, límites', 'Pantallas y audio de Android', 'Red: cifrado, P2P, SIP, SMS', 'Bluetooth, Wi‑Fi Direct, mDNS', 'Plug and play, invitaciones', 'CUPLAJ‑AIR: luz, sonido, vibración', 'Control de la función (1.5)', 'Medios, OSC, megafonía (1.6)', 'Zonas, reglas, contador (1.7)', 'Art‑Net, MIDI, juegos con el público (1.8)', 'App de iPhone e iPad'], 't2': 'Hasta dónde ha llegado cada paso', 'steps': ['Código escrito', 'Pruebas automáticas superadas (JVM y macOS)', 'Compilaciones en CI (APK, simulador iOS)', 'Versión publicada (APK, IPA sin firmar)', 'Probado en unos pocos teléfonos reales', 'Ensayo y función en directo']}, 'pt': {'t1': 'O que foi realmente verificado', 'cols': ['Testes automáticos', 'Compilações', 'Telemóveis reais'], 'leg': ['Feito', 'Parcial (apenas formatos, pacotes ou canais simulados)', 'Não feito'], 'rows': ['Lógica do hub: chamadas, linha partilhada, limites', 'Ecrãs e áudio do Android', 'Rede: cifragem, P2P, SIP, SMS', 'Bluetooth, Wi‑Fi Direct, mDNS', 'Plug and play, convites', 'CUPLAJ‑AIR: luz, som, vibração', 'Controlo do espetáculo (1.5)', 'Multimédia, OSC, sonorização (1.6)', 'Zonas, regras, contador (1.7)', 'Art‑Net, MIDI, jogos com o público (1.8)', 'App iPhone e iPad'], 't2': 'Até onde foi cada passo', 'steps': ['Código escrito', 'Testes automáticos aprovados (JVM e macOS)', 'Compilações na CI (APK, simulador iOS)', 'Versão publicada (APK, IPA não assinado)', 'Testado em alguns telemóveis reais', 'Ensaio e espetáculo ao vivo']}, 'de': {'t1': 'Was wirklich überprüft wurde', 'cols': ['Automatische Tests', 'Builds', 'Echte Telefone'], 'leg': ['Erledigt', 'Teilweise (nur Formate, Pakete oder simulierte Kanäle)', 'Nicht erledigt'], 'rows': ['Hub‑Logik: Gespräche, Gemeinschaftsanschluss, Limits', 'Android‑Bildschirme und Audio', 'Netz: Verschlüsselung, P2P, SIP, SMS', 'Bluetooth, Wi‑Fi Direct, mDNS', 'Plug and play, Einladungen', 'CUPLAJ‑AIR: Licht, Schall, Vibration', 'Showsteuerung (1.5)', 'Medien, OSC, Durchsagen (1.6)', 'Zonen, Regeln, Zähler (1.7)', 'Art‑Net, MIDI, Publikumsspiele (1.8)', 'iPhone‑ und iPad‑App'], 't2': 'Wie weit jeder Schritt gekommen ist', 'steps': ['Code geschrieben', 'Automatische Tests bestanden (JVM und macOS)', 'Builds in der CI (APK, iOS‑Simulator)', 'Release veröffentlicht (APK, unsignierte IPA)', 'Auf einigen echten Telefonen ausprobiert', 'Probe und Live‑Vorstellung']}, 'hu': {'t1': 'Mi lett valóban ellenőrizve', 'cols': ['Automatikus tesztek', 'Buildek', 'Valódi telefonok'], 'leg': ['Kész', 'Részleges (csak formátumok, csomagok vagy szimulált csatornák)', 'Nincs kész'], 'rows': ['Hub logikája: hívások, közös vonal, korlátok', 'Android képernyők és hang', 'Hálózat: titkosítás, P2P, SIP, SMS', 'Bluetooth, Wi‑Fi Direct, mDNS', 'Plug and play, meghívók', 'CUPLAJ‑AIR: fény, hang, rezgés', 'Előadásvezérlés (1.5)', 'Média, OSC, hangosítás (1.6)', 'Zónák, szabályok, számláló (1.7)', 'Art‑Net, MIDI, közönségjátékok (1.8)', 'iPhone és iPad app'], 't2': 'Meddig jutott az egyes lépés', 'steps': ['Kód megírva', 'Automatikus tesztek sikeresek (JVM és macOS)', 'Buildek CI-ben (APK, iOS-szimulátor)', 'Kiadás közzétéve (APK, aláíratlan IPA)', 'Néhány valódi telefonon kipróbálva', 'Próba és élő előadás']}}
for _l in LANGS:
    D[_l]["site"]["dg"] = _DG[_l]
os.makedirs(os.path.join(ROOT, "docs"), exist_ok=True)

def bar(prefix, cur, ext_en):
    out = []
    for l in LANGS:
        name = NAMES[l]
        fn = (prefix + (ext_en if l == "en" else "." + l + ".md")) if prefix != "README" else ("README.md" if l == "en" else "README.%s.md" % l)
        out.append("**%s**" % name if l == cur else "[%s](%s)" % (name, fn))
    return " · ".join(out)

_REV = {"en": "Honest review and comparison", "ro": "Recenzie onestă și comparație", "fr": "Revue honnête et comparaison", "it": "Recensione onesta e confronto",
        "es": "Reseña honesta y comparación", "pt": "Revisão honesta e comparação", "de": "Ehrliche Bewertung und Vergleich", "hu": "Őszinte értékelés és összehasonlítás"}

for _l in LANGS:
    D[_l]["site"]["review"] = _REV[_l]

def readme(l):
    d = D[l]
    s = ["# CUPLAJ", "", bar("README", l, ".md"), "", "<p align=\"center\"><img src=\"docs/logo.svg\" width=\"160\" alt=\"CUPLAJ\"></p>", "",
         "> **%s**" % d["tag"], "", "_%s_" % (d["ver"] % (VERSION, DATE)), "", d["intro"], "",
         "**[%s](%s/releases/download/v%s/CUPLAJ-%s.apk) · [%s](%s/releases/download/v%s/CUPLAJ-%s-ios-unsigned.ipa) · [%s](%s/releases)**" % (
             d["site"]["get"], REPO, RELEASE, RELEASE, d["site"]["get_ios"], REPO, RELEASE, RELEASE, d["site"]["all_rel"], REPO), "",
         "## " + d["h_feat"], ""]
    s += ["- " + x for x in d["feat"]]
    s += ["", "## " + d["h_quick"], ""]
    s += ["%d. %s" % (i + 1, x) for i, x in enumerate(d["quick"])]
    s += ["", "## " + d["h_eras"], ""] + ["- " + x for x in d["eras"]]
    s += ["", "## " + d["h_party"], "", "<p align=\"center\"><img src=\"docs/partyline.svg\" width=\"620\" alt=\"party line\"></p>", ""]
    s += ["%d. %s" % (i + 1, x) for i, x in enumerate(d["party"])]
    s += ["", "## " + d["h_docs"], ""]
    gd = "docs/GUIDE.md" if l == "en" else "docs/GUIDE.%s.md" % l
    s += ["- [%s](%s)" % (d["docs"]["guide"], gd),
          "- [%s](docs/PROTOCOL.md) *(English)*" % d["docs"]["protocol"],
          "- [%s](docs/AUDIT.md) *(English)*" % d["docs"]["audit"],
          "- [%s](CHANGELOG.md) *(English)*" % d["docs"]["changelog"],
          "- [%s](docs/REVIEW.md) *(English)*" % _REV[l],
          "- [%s](https://chiuta.github.io/CUPLAJ/)" % d["docs"]["site"]]
    s += ["", "## " + d["h_status"], "", d["status"], "", "## " + d["h_build"], "", d["build"], "",
          "## " + d["h_priv"], "", d["priv"], "", "## " + d["h_support"], "", d["support"], "",
          "## " + d["h_lic"], "", d["lic"], ""]
    return "\n".join(s)

def guide(l):
    d = D[l]
    s = ["# CUPLAJ — " + d["g_title"], "", bar("GUIDE", l, ".md").replace("(GUIDE.md)", "(GUIDE.md)"), "",
         "_%s_" % (d["ver"] % (VERSION, DATE)), ""]
    for i, (h, items) in enumerate(d["g"]):
        s += ["## %d. %s" % (i + 1, h), ""] + ["- " + x for x in items] + [""]
    s += ["---", "", "[← README](../README.md)" if l == "en" else "[← README](../README.%s.md)" % l, ""]
    return "\n".join(s)

for l in LANGS:
    open(os.path.join(ROOT, "README.md" if l == "en" else "README.%s.md" % l), "w", encoding="utf8").write(readme(l))
    open(os.path.join(ROOT, "docs", "GUIDE.md" if l == "en" else "GUIDE.%s.md" % l), "w", encoding="utf8").write(guide(l))

# ---- site
site = {}
for l in LANGS:
    d = D[l]
    site[l] = dict(name=NAMES[l], tag=d["tag"], ver=d["ver"] % (VERSION, DATE), intro=d["intro"], h_feat=d["h_feat"], feat=d["feat"],
                   h_quick=d["h_quick"], quick=d["quick"], h_eras=d["h_eras"], eras=d["eras"], h_party=d["h_party"], party=d["party"],
                   h_docs=d["h_docs"], docs=d["docs"], h_status=d["h_status"], status=d["status"], h_priv=d["h_priv"], priv=d["priv"],
                   h_support=d["h_support"], h_lic=d["h_lic"], lic=d["lic"], g_title=d["g_title"], s=d["site"])
sys.path.insert(0, os.path.dirname(__file__))
import site_roles
_checks = len(re.findall(r"\bcheck\(", open(os.path.join(ROOT, "app/src/test/kotlin/ro/trom/cuplaj/HubTest.kt"), encoding="utf-8").read()))
for l in LANGS:
    site[l]["roles"] = site_roles.R[l]; site[l]["rs"] = site_roles.S[l]
    site[l]["kinds"] = len(_kinds); site[l]["checks"] = (_checks // 50) * 50
tpl = open(os.path.join(os.path.dirname(__file__), "site_template.html"), encoding="utf8").read()
html = tpl.replace("/*__DATA__*/", "const DATA=" + json.dumps(site, ensure_ascii=False) + ";").replace("__RELEASE__", RELEASE).replace("__VERSION__", VERSION).replace("__REPO__", REPO)
open(os.path.join(ROOT, "docs", "index.html"), "w", encoding="utf8").write(html)
print("generated", len(LANGS), "readmes,", len(LANGS), "guides, site")
