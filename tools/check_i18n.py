# Validates assets/i18n.tsv: 9 columns, no empty cell, same format placeholders in every language, every L.t("key") in the code exists.
import re, sys, glob, os
root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
tsv = os.path.join(root, "app/src/main/assets/i18n.tsv")
keys, bad = {}, 0
for n, l in enumerate(open(tsv, encoding="utf8"), 1):
    if l.startswith("#") or not l.strip():
        continue
    p = l.rstrip("\n").split("\t")
    if len(p) != 9:
        print("line", n, "needs 9 columns"); bad += 1; continue
    ph = lambda s: sorted(re.findall(r"%\d\$[sd]", s))
    for i in range(1, 9):
        if not p[i].strip():
            print(p[0], "empty in column", i); bad += 1
        elif ph(p[i]) != ph(p[1]):
            print(p[0], "placeholder mismatch in column", i); bad += 1
    keys[p[0]] = 1
dyn = ("cue_", "hint_", "role_", "denied_", "era_btn_")
for f in glob.glob(os.path.join(root, "app/src/main/kotlin/**/*.kt"), recursive=True):
    for k in re.findall(r'L\.t\("([a-z_0-9]+)"', open(f, encoding="utf8").read()):
        if k not in keys and not k.endswith("_"):
            print("missing key", k, "in", os.path.basename(f)); bad += 1
print("keys:", len(keys), "problems:", bad)
sys.exit(1 if bad else 0)
