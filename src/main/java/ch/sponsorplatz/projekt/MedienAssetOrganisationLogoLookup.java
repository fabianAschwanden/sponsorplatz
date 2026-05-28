package ch.sponsorplatz.projekt;

import ch.sponsorplatz.shared.medien.OrganisationLogoLookup;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementiert {@link OrganisationLogoLookup} über {@link MedienAssetService}.
 * Liegt im {@code projekt}-Package, weil {@code MedienAsset}/{@code EntityTyp}
 * hier wohnen — das {@code organisation}-Package hängt nur am shared-Interface
 * (DIP, kein Feature-Cycle).
 */
@Component
class MedienAssetOrganisationLogoLookup implements OrganisationLogoLookup {

    private final MedienAssetService medienAssetService;

    MedienAssetOrganisationLogoLookup(MedienAssetService medienAssetService) {
        this.medienAssetService = medienAssetService;
    }

    @Override
    public Optional<String> findeLogoUrl(UUID organisationId) {
        return medienAssetService.findeLogoUrl(EntityTyp.ORGANISATION, organisationId);
    }
}
