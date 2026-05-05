package ch.sponsorplatz.service;

import ch.sponsorplatz.dto.AppUserFormDto;
import ch.sponsorplatz.dto.OrganisationFormDto;
import ch.sponsorplatz.dto.SponsorRegistrierungFormDto;
import ch.sponsorplatz.model.AppUser;
import ch.sponsorplatz.model.OrgTyp;
import ch.sponsorplatz.model.Organisation;
import ch.sponsorplatz.model.Rolle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service für die kombinierte Sponsor-Registrierung.
 * Erstellt in einer Transaktion: AppUser + Organisation (UNTERNEHMEN) + Mitgliedschaft (ORG_OWNER).
 */
@Service
@Transactional
public class SponsorRegistrierungService {

    private final AppUserService appUserService;
    private final OrganisationService organisationService;
    private final MitgliedschaftService mitgliedschaftService;

    public SponsorRegistrierungService(AppUserService appUserService,
                                        OrganisationService organisationService,
                                        MitgliedschaftService mitgliedschaftService) {
        this.appUserService = appUserService;
        this.organisationService = organisationService;
        this.mitgliedschaftService = mitgliedschaftService;
    }

    /**
     * Registriert einen Sponsor: erstellt User, Organisation (UNTERNEHMEN/PENDING)
     * und verknüpft beide via Mitgliedschaft als ORG_OWNER.
     *
     * @throws IllegalArgumentException bei doppelter E-Mail oder Slug-Konflikt
     */
    public void registriereSponsor(SponsorRegistrierungFormDto dto) {
        // 1. User erstellen (inkl. E-Mail-Verifizierung)
        AppUserFormDto userForm = new AppUserFormDto();
        userForm.setEmail(dto.getEmail());
        userForm.setAnzeigename(dto.getAnzeigename());
        userForm.setPasswort(dto.getPasswort());
        AppUser user = appUserService.registriere(userForm);

        // 2. Organisation als UNTERNEHMEN erstellen
        OrganisationFormDto orgForm = new OrganisationFormDto();
        orgForm.setTyp(OrgTyp.UNTERNEHMEN);
        orgForm.setName(dto.getFirmenname());
        orgForm.setBranche(dto.getBranche());
        orgForm.setRechtsform(dto.getRechtsform());
        orgForm.setWebsiteUrl(dto.getWebsiteUrl());
        orgForm.setBeschreibung(dto.getBeschreibung());
        Organisation org = organisationService.erstelle(orgForm);

        // 3. Mitgliedschaft als ORG_OWNER
        mitgliedschaftService.fuegeHinzu(org.getId(), user.getId(), Rolle.ORG_OWNER, null);
    }
}

