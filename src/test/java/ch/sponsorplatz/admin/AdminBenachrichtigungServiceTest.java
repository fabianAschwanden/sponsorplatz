package ch.sponsorplatz.admin;

import ch.sponsorplatz.benachrichtigung.BenachrichtigungTyp;
import ch.sponsorplatz.benachrichtigung.NotificationService;
import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.benutzer.PlatformRolle;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.shared.mail.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests für {@link AdminBenachrichtigungService}.
 * Test-IDs: ADMIN-NOTIF-01..04.
 */
@ExtendWith(MockitoExtension.class)
class AdminBenachrichtigungServiceTest {

    @Mock private AppUserRepository appUserRepository;
    @Mock private NotificationService notificationService;
    @Mock private MailService mailService;

    private AdminBenachrichtigungService service;

    @BeforeEach
    void setUp() {
        service = new AdminBenachrichtigungService(appUserRepository, notificationService, mailService);
    }

    @Test
    @DisplayName("ADMIN-NOTIF-01: Verein-Registrierung → für jeden Admin eine In-App-Notification + Mail")
    void verein_jedenAdminBenachrichtigen() {
        AppUser admin1 = admin("admin1@sponsorplatz.ch");
        AppUser admin2 = admin("admin2@sponsorplatz.ch");
        when(appUserRepository.findByPlatformRolle(PlatformRolle.PLATFORM_ADMIN))
                .thenReturn(List.of(admin1, admin2));
        Organisation org = org("FC Beispiel", OrgTyp.VEREIN);

        service.benachrichtigeUeberNeueOrgRegistrierung(org);

        verify(notificationService).benachrichtige(eq(admin1.getId()),
                eq(BenachrichtigungTyp.NEUE_ORG_REGISTRIERT),
                anyString(), anyString(), eq("/admin/verifizierungen"));
        verify(notificationService).benachrichtige(eq(admin2.getId()),
                eq(BenachrichtigungTyp.NEUE_ORG_REGISTRIERT),
                anyString(), anyString(), eq("/admin/verifizierungen"));
        verify(mailService).sendePlain(eq("admin1@sponsorplatz.ch"), anyString(), anyString());
        verify(mailService).sendePlain(eq("admin2@sponsorplatz.ch"), anyString(), anyString());
    }

    @Test
    @DisplayName("ADMIN-NOTIF-02: Titel/Body unterscheidet Verein vs. Sponsor-Organisation")
    void titelUnterscheidetTyp() {
        AppUser admin = admin("a@s.ch");
        when(appUserRepository.findByPlatformRolle(PlatformRolle.PLATFORM_ADMIN))
                .thenReturn(List.of(admin));

        service.benachrichtigeUeberNeueOrgRegistrierung(org("Muster AG", OrgTyp.UNTERNEHMEN));

        ArgumentCaptor<String> subjectCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCap = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendePlain(eq("a@s.ch"), subjectCap.capture(), bodyCap.capture());
        assertThat(subjectCap.getValue()).contains("Sponsor-Organisation", "Muster AG");
        assertThat(bodyCap.getValue()).contains("registriert", "Muster AG", "Verifizierung");
    }

    @Test
    @DisplayName("ADMIN-NOTIF-03: Mail-Versand wirft → andere Admins werden trotzdem bedient, keine Exception")
    void mailFehlerSchluckenUndWeiter() {
        AppUser admin1 = admin("admin1@s.ch");
        AppUser admin2 = admin("admin2@s.ch");
        when(appUserRepository.findByPlatformRolle(PlatformRolle.PLATFORM_ADMIN))
                .thenReturn(List.of(admin1, admin2));
        doThrow(new MailSendException("SMTP down"))
                .when(mailService).sendePlain(eq("admin1@s.ch"), anyString(), anyString());

        service.benachrichtigeUeberNeueOrgRegistrierung(org("FC Beispiel", OrgTyp.VEREIN));

        verify(notificationService).benachrichtige(eq(admin1.getId()),
                eq(BenachrichtigungTyp.NEUE_ORG_REGISTRIERT),
                anyString(), anyString(), anyString());
        verify(notificationService).benachrichtige(eq(admin2.getId()),
                eq(BenachrichtigungTyp.NEUE_ORG_REGISTRIERT),
                anyString(), anyString(), anyString());
        verify(mailService).sendePlain(eq("admin2@s.ch"), anyString(), anyString());
    }

    @Test
    @DisplayName("ADMIN-NOTIF-04: Keine PLATFORM_ADMINs konfiguriert → kein Notification-/Mail-Versand, keine Exception")
    void keineAdmins_keinVersand() {
        when(appUserRepository.findByPlatformRolle(PlatformRolle.PLATFORM_ADMIN))
                .thenReturn(List.of());

        service.benachrichtigeUeberNeueOrgRegistrierung(org("FC Beispiel", OrgTyp.VEREIN));

        verifyNoInteractions(notificationService);
        verifyNoInteractions(mailService);
    }

    private static AppUser admin(String email) {
        AppUser u = new AppUser();
        u.setId(UUID.randomUUID());
        u.setEmail(email);
        u.setPlatformRolle(PlatformRolle.PLATFORM_ADMIN);
        return u;
    }

    private static Organisation org(String name, OrgTyp typ) {
        Organisation o = new Organisation();
        o.setId(UUID.randomUUID());
        o.setName(name);
        o.setTyp(typ);
        return o;
    }
}
