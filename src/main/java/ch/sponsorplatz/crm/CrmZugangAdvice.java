package ch.sponsorplatz.crm;

import ch.sponsorplatz.organisation.MitgliedschaftRepository;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.Rolle;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Set;

/**
 * Stellt jedem Controller {@code crmSponsorSlug} bereit: den Slug der Firma
 * ({@link OrgTyp#UNTERNEHMEN}), auf der der eingeloggte User Bearbeitungsrechte
 * hat. Das Sidebar-Fragment blendet daraus den CRM-Einstieg ein — nur für
 * Mitglieder einer Firma mit Edit-Rolle, nicht für reine Vereins-User oder
 * Betrachter. Bei mehreren Firmen gewinnt die alphabetisch erste.
 *
 * <p>{@link MitgliedschaftRepository} wird als {@link ObjectProvider} injiziert,
 * damit {@code @WebMvcTest}-Slices ohne JPA-Beans laden — der Lookup liefert dort
 * {@code null} und der CRM-Eintrag bleibt aus.
 */
@ControllerAdvice(basePackages = "ch.sponsorplatz")
public class CrmZugangAdvice {

    /** „Bearbeitungsrechte" = Owner oder Editor (Viewer reicht nicht). */
    private static final Set<Rolle> EDIT_ROLLEN = Set.of(Rolle.ORG_OWNER, Rolle.ORG_EDITOR);

    private final ObjectProvider<MitgliedschaftRepository> mitgliedschaftRepositoryProvider;

    public CrmZugangAdvice(ObjectProvider<MitgliedschaftRepository> mitgliedschaftRepositoryProvider) {
        this.mitgliedschaftRepositoryProvider = mitgliedschaftRepositoryProvider;
    }

    @ModelAttribute("crmSponsorSlug")
    public String crmSponsorSlug(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        MitgliedschaftRepository repo = mitgliedschaftRepositoryProvider.getIfAvailable();
        if (repo == null) {
            return null;
        }
        return repo.findSponsorOrgSlugs(
                        authentication.getName().toLowerCase().trim(),
                        EDIT_ROLLEN,
                        OrgTyp.UNTERNEHMEN)
                .stream().findFirst().orElse(null);
    }
}
