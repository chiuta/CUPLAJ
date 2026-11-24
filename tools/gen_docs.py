# -*- coding: utf-8 -*-
"""Generates README.<xx>.md, docs/GUIDE.<xx>.md, docs/index.html from docs_src.py. Run: python3 tools/gen_docs.py"""
import json, os, re, sys
sys.path.insert(0, os.path.dirname(__file__))
from docs_src import D, LANGS, NAMES

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VERSION, DATE = "1.7.0", "2026-11-24"
REPO = "https://github.com/chiuta/CUPLAJ"
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
