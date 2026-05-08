package ch.sponsorplatz.projekt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Form-DTO für Anlage / Bearbeitung eines SponsoringPakets.
 */
public class SponsoringPaketFormDto {

    private UUID id;

    @NotBlank(message = "Name ist Pflicht")
    @Size(min = 2, max = 255, message = "Name muss zwischen 2 und 255 Zeichen lang sein")
    private String name;

    private String beschreibung;

    private BigDecimal preisChf;

    private String gegenleistungen;

    private int sortierung;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBeschreibung() { return beschreibung; }
    public void setBeschreibung(String beschreibung) { this.beschreibung = beschreibung; }

    public BigDecimal getPreisChf() { return preisChf; }
    public void setPreisChf(BigDecimal preisChf) { this.preisChf = preisChf; }

    public String getGegenleistungen() { return gegenleistungen; }
    public void setGegenleistungen(String gegenleistungen) { this.gegenleistungen = gegenleistungen; }

    public int getSortierung() { return sortierung; }
    public void setSortierung(int sortierung) { this.sortierung = sortierung; }
}

