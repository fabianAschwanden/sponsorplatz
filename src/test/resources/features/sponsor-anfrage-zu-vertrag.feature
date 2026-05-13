# language: de
Funktionalität: Sponsor-Anfrage von Vereins-Paket zu Vertrag

  Damit ein Verein einen passenden Sponsor finden kann, soll der Pfad
  vom Verein-Onboarding bis zum unterzeichneten Vertrag durchgehend
  funktionieren — egal über welche Browser-Seite die einzelnen Schritte
  laufen. Im Hintergrund existiert eine Sponsor-Org „CSS Versicherung",
  die als Anfragender auftritt.

  Hintergrund:
    Angenommen die Plattform läuft mit dem Test-Sponsor "CSS Versicherung"

  Szenario: Verein registriert sich, legt Paket an, CSS stellt Anfrage, Annahme + Vertrag
    Wenn ich mich als neuer Verein-Owner "Max Muster" registriere
    Und meine E-Mail-Adresse bestätigt wird
    Und ich mich einlogge
    Und ich im Onboarding den Verein "FC E2E" in der Branche "SPORT" anlege
    Und ich ein Projekt "Sommerfest 2026" mit Sponsoring-Paket "Gold" zu 1000 CHF erstelle

    Und sich der Sponsor "CSS Versicherung" einloggt
    Und der Sponsor eine Sponsor-Anfrage zum Paket "Gold" stellt

    Und ich mich wieder als Verein-Owner einlogge
    Und ich die Anfrage von "CSS Versicherung" annehme
    Und ich für die angenommene Anfrage einen Vertrag erstelle

    Dann existiert in der Datenbank ein Vertrag zwischen "FC E2E" und "CSS Versicherung"
