# Sponsorplatz — Produkt-Übersicht

## Positionierung

> **Sponsorplatz ist die Schweizer Sponsoring-Plattform für Sport und Gesundheit.**

## Slogan

> **Wo Vereine und Marken zueinander finden.**

## Vision

Eine Schweizer Plattform, auf der Sport- und Gesundheits-Vereine ihre Sponsoring-Daten
gemeinsam pflegen — und Marken aus dem Health-Umfeld die Vereine finden, deren Mission
zu ihrer Botschaft passt. Statt Sponsoring-Streuverluste schaffen wir einen kuratierten
Ort, an dem Health-Affinität auf beiden Seiten gegeben ist.

## Nische (strikt) und Themenfokus (breit)

**Strikt — wer auf die Plattform kommt:** Ausschliesslich Vereine, die sich klar im
Sport- oder Gesundheitsbereich positionieren. Vereine ohne Health-Bezug werden im
Verifizierungs-Workflow durch den Plattform-Admin abgelehnt. Diese Schärfe ist unser
zentrales Versprechen an Sponsoren.

**Breit — was unter Health zählt:** Sport im klassischen Sinn, aber auch Bewegung,
Reha, Behindertensport, Seniorensport, Prävention, Mentale Gesundheit, Ernährung,
Wellness, Selbsthilfe, Patientenorganisationen. Technisch abgebildet als
`Branche`-Enum mit elf Werten (siehe `DATENMODELL.md` und `Branche.java`).

## Sub-Claims

- **Für Vereine im Health-Bereich.** Sport, Bewegung, Reha, Mental Health, Ernährung,
  Selbsthilfe — pflege deine Sponsoren-Daten, veröffentliche Projekte, werd' von den
  richtigen Marken gefunden.
- **Für Firmen mit Health-Bezug.** Krankenkassen, Apotheken, Lebensmittel,
  Fitness-Marken: Finde Schweizer Vereine, die wirklich zu deiner Botschaft passen.
  Lokal. Messbar. Ehrlich.

## Zielgruppen

### Primär: Schweizer Sport- und Health-Vereine

- Sportvereine (~20'000 in der Schweiz, davon ca. 12'000 mit aktivem Sponsoring-Bedarf)
- Reha- und Bewegungsangebote (Physio-nahe, Bewegungstherapie)
- Behindertensport, Seniorensport
- Selbsthilfegruppen, Patientenorganisationen (~2'500)
- Vereine für Prävention, Ernährung, Mental Health

Pain: Excel-Chaos, jährlich gleicher Aufwand, manuelles Versenden.
Wunsch: zentrale Übersicht, professionelle Tools, Sichtbarkeit gegenüber thematisch
passenden Sponsoren.

### Sekundär: Marken mit Health-Affinität

- Krankenkassen (CSS, Helsana, Swica, Sanitas, ÖKK, Sympany, Concordia, Visana ...)
- Apotheken-Ketten (Galenica/Amavita, TopPharm, Coop Vitality)
- Lebensmittel mit Gesundheitsbezug (Bio-Marken, Sportnahrung, Migros/Coop Health-Linien)
- Fitness- und Sportartikel-Marken
- Stiftungen mit Schwerpunkt Gesundheit / Bewegung

Pain: Streuverluste in generischen Sponsoring-Plattformen; Aufwand, geeignete
Vereine ausserhalb des Top-Sports zu finden.
Wunsch: kuratiertes Vereinsportfolio mit klarer Health-Mission, Region, Wirkung.

## Drei Säulen

```
Säule 1                Säule 2                Säule 3
Vereins-               Health-Markt-          Verbands-
Werkzeug               platz                  Partnerschaft

"CRM + Tools           "Marken finden         "Wir bringen Dir
für Sponsoring"        Sport- & Health-       Deine Mitgliedsvereine"
                       Vereine — kuratiert"
```

## Modell: Kollaborative Plattform

- Mehrere Vereine, **gemeinsame Datenbasis**
- Edit-Rechte über Mitgliedschaft (User → Org → Rolle)
- Sponsor-Stammdaten **geteilt** (Wikipedia-Modell)
- Notizen, Beteiligungen, Kommunikation pro Verein zugeordnet

Details: [`ROLLENKONZEPT.md`](ROLLENKONZEPT.md)

## Differenzierung

| Wettbewerber | Lücke |
|---|---|
| Fairgate | Tools-only, kein Marktplatz, kein Themenfokus |
| fundoo / lokalhelden | Crowdfunding-Spenden, kein B2B, themenagnostisch |
| MY SPONSOR | Fan-Spenden, kein Unternehmens-Sponsoring |
| Sponsoo (DE) | Sport-only, kein CH-Fokus, keine Health-Erweiterung |
| Generische CH-Plattformen | Gegen alle Themen — kein Vertrauensvorteil für Health-Marken |

**Sponsorplatz vereint:** CRM + Health-fokussierter Marktplatz + B2B-Sponsoring + CH-Fokus.

## Verifizierungs-Versprechen

Jede neue Organisation durchläuft die Verifizierungs-Queue (`PlatformAdmin`).
Der Admin prüft mindestens:

1. **Health-Fokus** — passt die Vereinsmission zu einer der Health-Branchen?
2. **Schweizer Bezug** — Sitz, Tätigkeit, Sprache.
3. **Plausibilität** — Website, Rechtsform, Zefix-UID (sofern angegeben).

Vereine ohne klaren Health-Bezug werden mit Begründung abgelehnt — das ist der Kern
unseres Vertrauens-Versprechens an die Sponsoren-Seite.

## Roadmap

Siehe [`ROADMAP.md`](ROADMAP.md).
