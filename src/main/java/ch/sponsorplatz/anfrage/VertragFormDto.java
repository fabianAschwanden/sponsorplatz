package ch.sponsorplatz.anfrage;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Form-DTO zum Editieren eines Entwurf-Vertrags.
 *
 * <p>Snapshot-Felder (orgName, sponsorName etc.) sind read-only und werden
 * nicht überschrieben.
 */
public class VertragFormDto {

    private String paketBeschreibung;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal preisChf;

    private LocalDate laufzeitVon;
    private LocalDate laufzeitBis;

    private String leistungVerein;
    private String leistungSponsor;

    public String getPaketBeschreibung() { return paketBeschreibung; }
    public void setPaketBeschreibung(String paketBeschreibung) { this.paketBeschreibung = paketBeschreibung; }

    public BigDecimal getPreisChf() { return preisChf; }
    public void setPreisChf(BigDecimal preisChf) { this.preisChf = preisChf; }

    public LocalDate getLaufzeitVon() { return laufzeitVon; }
    public void setLaufzeitVon(LocalDate laufzeitVon) { this.laufzeitVon = laufzeitVon; }

    public LocalDate getLaufzeitBis() { return laufzeitBis; }
    public void setLaufzeitBis(LocalDate laufzeitBis) { this.laufzeitBis = laufzeitBis; }

    public String getLeistungVerein() { return leistungVerein; }
    public void setLeistungVerein(String leistungVerein) { this.leistungVerein = leistungVerein; }

    public String getLeistungSponsor() { return leistungSponsor; }
    public void setLeistungSponsor(String leistungSponsor) { this.leistungSponsor = leistungSponsor; }
}
