package ch.sponsorplatz.shared.medien;

import java.util.Optional;
import java.util.UUID;

/**
 * DIP-Seam für den Logo-Lookup einer Organisation. Das Logo ist ein
 * {@code MedienAsset} (AssetTyp.LOGO) im {@code projekt}-Package, der
 * {@code organisation}-Controller darf das aber nicht direkt nutzen —
 * sonst entstünde der Zyklus {@code projekt → organisation → projekt}
 * (MedienController kennt schon organisation, ARCH-06 verbietet den Kreis).
 *
 * <p>Daher dieses Interface in {@code shared/}: organisation hängt nur an
 * der Abstraktion, die Implementierung lebt im {@code projekt}-Package.
 */
public interface OrganisationLogoLookup {

    /**
     * @return Auslieferungs-Pfad ({@code /medien/{id}}) des Logo-Assets der
     *         Organisation, oder {@code Optional.empty()} wenn keins gesetzt.
     */
    Optional<String> findeLogoUrl(UUID organisationId);
}
