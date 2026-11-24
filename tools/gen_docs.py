# -*- coding: utf-8 -*-
"""Generates README.<xx>.md, docs/GUIDE.<xx>.md, docs/index.html from docs_src.py. Run: python3 tools/gen_docs.py"""
import json, os, re, sys
sys.path.insert(0, os.path.dirname(__file__))
from docs_src import D, LANGS, NAMES

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VERSION, DATE = "1.8.0", "2026-11-24"
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
os.makedirs(os.path.join(ROOT, "docs"), exist_ok=True)

def bar(prefix, cur, ext_en):
    out = []
    for l in LANGS:
        name = NAMES[l]
        fn = (prefix + (ext_en if l == "en" else "." + l + ".md")) if prefix != "README" else ("README.md" if l == "en" else "README.%s.md" % l)
        out.append("**%s**" % name if l == cur else "[%s](%s)" % (name, fn))
    return " · ".join(out)

def readme(l):
    d = D[l]
    s = ["# CUPLAJ", "", bar("README", l, ".md"), "", "<p align=\"center\"><img src=\"docs/logo.svg\" width=\"160\" alt=\"CUPLAJ\"></p>", "",
         "> **%s**" % d["tag"], "", "_%s_" % (d["ver"] % (VERSION, DATE)), "", d["intro"], "",
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
tpl = open(os.path.join(os.path.dirname(__file__), "site_template.html"), encoding="utf8").read()
html = tpl.replace("/*__DATA__*/", "const DATA=" + json.dumps(site, ensure_ascii=False) + ";").replace("__VERSION__", VERSION).replace("__REPO__", REPO)
open(os.path.join(ROOT, "docs", "index.html"), "w", encoding="utf8").write(html)
print("generated", len(LANGS), "readmes,", len(LANGS), "guides, site")
