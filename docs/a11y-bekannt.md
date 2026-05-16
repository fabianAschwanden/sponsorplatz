# Bekannte Accessibility-Findings (Baseline)

Diese Datei listet axe-core-Regeln, die der [`A11ySmokeIT`](../src/test/java/ch/sponsorplatz/e2e/A11ySmokeIT.java)
aktuell **toleriert**, statt den Build rot zu machen. Sie dient als Baseline für den
Pilot-Launch — neue, nicht hier gelistete Verstösse mit Severity `serious` / `critical`
brechen die Suite und müssen behoben werden.

| axe-Regel | Wo | Status | Plan |
|---|---|---|---|
| `color-contrast` | Hero-Subclaim auf der Home-Seite (`/`) — Coral- und Violet-Tinten haben ~3:1 statt der geforderten 4.5:1 für Normaltext | Baseline | Phase 11+: neue Farb-Tokens im `dashboard.css`, dann hier streichen |

## Aufnahme-Konvention

Eintrag hinzufügen NUR wenn:

1. Die Regel-ID + Severity ist genau identifiziert (`impact: serious` oder `critical`)
2. Der betroffene Bereich ist eingegrenzt (welche Komponente, welche Farbe)
3. Ein konkreter Behebungs-Plan steht im Plan-Spalte — *nicht* „später".

Eintrag entfernen wenn:

- Behoben → Test wird wieder grün, Plan-Eintrag löschen
- Wenn ein neuer Finding hier toleriert wird, muss er in der `ZUGELASSENE_IDS`-Liste in
  [`A11ySmokeIT.java`](../src/test/java/ch/sponsorplatz/e2e/A11ySmokeIT.java) ergänzt werden.
