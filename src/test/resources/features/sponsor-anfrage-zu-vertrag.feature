# language: de
Funktionalität: Vom Verein zur Sponsoring-Vereinbarung mit CSS

  Der vom User gewünschte E2E-Flow: ein Verein-Owner registriert sich,
  legt seinen Verein an, sendet proaktiv eine Sponsoring-Anfrage an CSS.
  CSS nimmt die Anfrage an. Der Verein-Owner sieht den „Vertrag erstellen"-
  Button und lässt einen Vertrags-Entwurf entstehen — Snapshot der Betreff-
  und Org-Daten aus der Kontakt-Anfrage.

  Fachliche Spec: specs/KONTAKT_ANFRAGE_VERTRAG.md

  Hintergrund:
    Angenommen die Plattform läuft mit dem Test-Sponsor "CSS Versicherung"

  Szenario: Verein registriert sich, stellt Kontakt-Anfrage, CSS nimmt an, Vertrag entsteht
    Wenn ich mich als neuer Verein-Owner "Max Muster" registriere
    Und meine E-Mail-Adresse bestätigt wird
    Und ich mich einlogge
    Und ich im Onboarding den Verein "FC E2E" in der Branche "SPORT" anlege
    Und ich eine Kontakt-Anfrage an "CSS Versicherung" mit Betreff "Sommerfest-Sponsoring 2026" stelle

    Und sich der Sponsor "CSS Versicherung" einloggt
    Und der Sponsor die Kontakt-Anfrage von "FC E2E" annimmt

    Und ich mich wieder als Verein-Owner einlogge
    Und ich für die angenommene Kontakt-Anfrage einen Vertrag erstelle

    Dann existiert in der Datenbank ein Vertrag zwischen "FC E2E" und "CSS Versicherung"
    Und der Vertrag referenziert die Kontakt-Anfrage als Quelle
