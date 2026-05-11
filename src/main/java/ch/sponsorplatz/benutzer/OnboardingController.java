package ch.sponsorplatz.benutzer;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.organisation.MitgliedschaftRepository;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.OrganisationFormDto;
import ch.sponsorplatz.organisation.OrganisationService;
import ch.sponsorplatz.shared.config.ModelAttributeNames;
import ch.sponsorplatz.shared.exception.NotFoundException;

/**
 * Onboarding-Wizard — erscheint nach der ersten Anmeldung, wenn der User
 * noch keiner Organisation angehört. Zwei Optionen:
 * <ol>
 * <li><b>Verein erstellen:</b> Kompaktes Formular (Name, Branche, Ort) → Org +
 * ORG_OWNER</li>
 * <li><b>Einladung annehmen:</b> Token eingeben → Redirect auf
 * Einladungs-Annahme</li>
 * </ol>
 */
@Controller
@RequestMapping("/onboarding")
@PreAuthorize("isAuthenticated()")
public class OnboardingController {

    /**
     * Token-Whitelist: nur Hex/Base64URL-Zeichen erlaubt, Länge 16-128.
     * Verhindert URL-Injection beim Redirect (z.B. via Newlines, Querystring-
     * Anhänger oder Pfad-Manipulation).
     */
    private static final Pattern TOKEN_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{16,128}$");

    private final AppUserRepository appUserRepository;
    private final MitgliedschaftRepository mitgliedschaftRepository;
    private final OrganisationService organisationService;

    public OnboardingController(AppUserRepository appUserRepository,
            MitgliedschaftRepository mitgliedschaftRepository,
            OrganisationService organisationService) {
        this.appUserRepository = appUserRepository;
        this.mitgliedschaftRepository = mitgliedschaftRepository;
        this.organisationService = organisationService;
    }

    @GetMapping
    public String startseite(Authentication auth, Model model) {
        Optional<AppUser> userOpt = appUserRepository.findByEmail(auth.getName());
        // Plattform-Admins sehen das Onboarding nie.
        // User mit Mitgliedschaften brauchen kein Onboarding → Dashboard.
        if (userOpt.isPresent()) {
            AppUser user = userOpt.get();
            if (user.getPlatformRolle() == PlatformRolle.PLATFORM_ADMIN) {
                return "redirect:/dashboard";
            }
            if (!mitgliedschaftRepository.findOrgIdsByUserId(user.getId()).isEmpty()) {
                return "redirect:/dashboard";
            }
            // Wizard wird angezeigt — Flag setzen, damit künftige Logins nicht
            // erneut hierher umgeleitet werden, auch wenn der User keinen
            // Verein anlegt oder den Wizard abbricht.
            if (!user.isOnboardingGesehen()) {
                user.setOnboardingGesehen(true);
                appUserRepository.save(user);
            }
        }

        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "onboarding");
        model.addAttribute("vereinForm", new VereinSchnellFormDto());
        model.addAttribute("branchen", Branche.values());
        return "onboarding";
    }

    @PostMapping("/verein-erstellen")
    public String vereinErstellen(@Valid @ModelAttribute("vereinForm") VereinSchnellFormDto dto,
            BindingResult br,
            Authentication auth,
            Model model,
            RedirectAttributes redirect) {
        if (br.hasErrors()) {
            model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "onboarding");
            model.addAttribute("branchen", Branche.values());
            model.addAttribute("zeigeVereinForm", true);
            return "onboarding";
        }

        UUID userId = appUserRepository.findByEmail(auth.getName())
                .map(AppUser::getId)
                .orElseThrow(() -> new NotFoundException("User nicht gefunden"));

        try {
            OrganisationFormDto orgDto = new OrganisationFormDto();
            orgDto.setTyp(OrgTyp.VEREIN);
            orgDto.setName(dto.getVereinName());
            orgDto.setBranche(dto.getBranche());
            orgDto.setOrt(dto.getOrt());
            orgDto.setBeschreibung(dto.getBeschreibung());

            var org = organisationService.erstelleMitEigentuemer(orgDto, userId);

            redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                    "Willkommen! Ihr Verein \"" + org.getName() + "\" wurde erstellt. Sie können ihn jetzt verwalten.");
            return "redirect:/dashboard";
        } catch (IllegalArgumentException ex) {
            model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "onboarding");
            model.addAttribute(ModelAttributeNames.FEHLERMELDUNG, ex.getMessage());
            model.addAttribute("branchen", Branche.values());
            model.addAttribute("zeigeVereinForm", true);
            return "onboarding";
        }
    }

    /**
     * Leitet den User mit seinem Einladungs-Token auf die Standard-Vorschau weiter.
     * Token wird gegen Whitelist validiert (verhindert URL-Injection), und über
     * {@code RedirectAttributes.addAttribute} statt String-Konkatenation an die
     * URL gehängt — Spring kümmert sich um die Encoding-Quotes.
     */
    @PostMapping("/einladung-annehmen")
    public String einladungAnnehmen(@RequestParam(required = false) String token,
            RedirectAttributes redirect) {
        String trimmed = token == null ? "" : token.trim();
        if (trimmed.isEmpty()) {
            redirect.addFlashAttribute(ModelAttributeNames.FEHLERMELDUNG,
                    "Bitte geben Sie einen Einladungs-Token ein.");
            return "redirect:/onboarding";
        }
        if (!TOKEN_PATTERN.matcher(trimmed).matches()) {
            redirect.addFlashAttribute(ModelAttributeNames.FEHLERMELDUNG,
                    "Token-Format ungültig. Bitte den vollständigen Token aus der Einladungs-Mail kopieren.");
            return "redirect:/onboarding";
        }
        redirect.addAttribute("token", trimmed);
        return "redirect:/einladung/annehmen";
    }

    /**
     * Kompaktes DTO für die Schnell-Erstellung eines Vereins im Onboarding.
     * Nur die Pflichtfelder — alles andere kann der User nachträglich bearbeiten.
     */
    public static class VereinSchnellFormDto {

        @NotBlank(message = "Vereinsname ist Pflicht.")
        @Size(min = 2, max = 255, message = "Name muss zwischen 2 und 255 Zeichen lang sein.")
        private String vereinName;

        @NotNull(message = "Bitte wählen Sie eine Branche.")
        private Branche branche;

        @Size(max = 70)
        private String ort;

        @Size(max = 1000)
        private String beschreibung;

        public String getVereinName() {
            return vereinName;
        }

        public void setVereinName(String vereinName) {
            this.vereinName = vereinName;
        }

        public Branche getBranche() {
            return branche;
        }

        public void setBranche(Branche branche) {
            this.branche = branche;
        }

        public String getOrt() {
            return ort;
        }

        public void setOrt(String ort) {
            this.ort = ort;
        }

        public String getBeschreibung() {
            return beschreibung;
        }

        public void setBeschreibung(String beschreibung) {
            this.beschreibung = beschreibung;
        }
    }
}
