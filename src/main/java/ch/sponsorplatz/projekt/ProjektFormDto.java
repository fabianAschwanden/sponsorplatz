package ch.sponsorplatz.projekt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Form-DTO für Anlage / Bearbeitung eines Projekts.
 */
public class ProjektFormDto {

    private UUID id;

    @NotBlank(message = "Name ist Pflicht")
    @Size(min = 2, max = 255, message = "Name muss zwischen 2 und 255 Zeichen lang sein")
    private String name;

    private String beschreibung;

    @Size(max = 50)
    private String kategorie;

    @Size(max = 100)
    private String ort;

    private LocalDate startDatum;

    private LocalDate endDatum;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBeschreibung() { return beschreibung; }
    public void setBeschreibung(String beschreibung) { this.beschreibung = beschreibung; }

    public String getKategorie() { return kategorie; }
    public void setKategorie(String kategorie) { this.kategorie = kategorie; }

    public String getOrt() { return ort; }
    public void setOrt(String ort) { this.ort = ort; }

    public LocalDate getStartDatum() { return startDatum; }
    public void setStartDatum(LocalDate startDatum) { this.startDatum = startDatum; }

    public LocalDate getEndDatum() { return endDatum; }
    public void setEndDatum(LocalDate endDatum) { this.endDatum = endDatum; }
}

