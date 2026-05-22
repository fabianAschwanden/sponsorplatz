package ch.sponsorplatz.benutzer;

import ch.sponsorplatz.shared.config.SecurityConfig;
import ch.sponsorplatz.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Controller-Tests für {@link TwoFaSetupController}.
 * Test-IDs: AUTH-2FA-06..09 in {@code specs/AUTH_2FA_TOTP.md}.
 */
@WebMvcTest(controllers = TwoFaSetupController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("dev")
class TwoFaSetupControllerTest {

    private static final String USER_EMAIL = "user@sponsorplatz.ch";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private TwoFaService twoFaService;
    @MockitoBean private SponsorplatzUserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = USER_EMAIL)
    @DisplayName("AUTH-2FA-06: GET /einstellungen/2fa zeigt Setup-Seite mit QR-Code für nicht-aktiven User")
    void getSetupSeiteOhneTotp() throws Exception {
        when(twoFaService.findStatus(USER_EMAIL))
                .thenReturn(new TwoFaService.TwoFaStatus(false, null, 0));
        when(twoFaService.bereiteSetupVor(eq(USER_EMAIL), any()))
                .thenReturn(new TwoFaService.TwoFaSetupContext("ABCDEFGH", "data:image/png;base64,...", "ABCDEFGH"));

        mockMvc.perform(get("/einstellungen/2fa"))
                .andExpect(status().isOk())
                .andExpect(view().name("benutzer/2fa-setup"))
                .andExpect(model().attribute("totpAktiv", false))
                .andExpect(model().attributeExists("qrDataUrl"))
                .andExpect(model().attributeExists("manuellerCode"));
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    @DisplayName("AUTH-2FA-06b: GET /einstellungen/2fa zeigt Status-Seite für aktiven User")
    void getStatusSeiteMitTotp() throws Exception {
        when(twoFaService.findStatus(USER_EMAIL))
                .thenReturn(new TwoFaService.TwoFaStatus(true, Instant.now(), 7));

        mockMvc.perform(get("/einstellungen/2fa"))
                .andExpect(status().isOk())
                .andExpect(view().name("benutzer/2fa-status"))
                .andExpect(model().attribute("totpAktiv", true))
                .andExpect(model().attributeExists("aktiviertAm"))
                .andExpect(model().attribute("backupCodesVerbleibend", 7));
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    @DisplayName("AUTH-2FA-07: POST /aktivieren mit erfolgreichem Service-Call → Flash-Codes + Redirect")
    void aktivierenMitKorrektemCode() throws Exception {
        when(twoFaService.aktivieren(eq(USER_EMAIL), any(), eq("123456")))
                .thenReturn(new TwoFaService.TwoFaAktivierungsErgebnis(true, false,
                        List.of("AAAAAAAA", "BBBBBBBB", "CCCCCCCC")));

        mockMvc.perform(post("/einstellungen/2fa/aktivieren").param("code", "123456").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/einstellungen/2fa"))
                .andExpect(flash().attributeExists(TwoFaSetupController.FLASH_NEW_BACKUP_CODES));
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    @DisplayName("AUTH-2FA-08: POST /aktivieren mit Service-Failure → Fehler-Flash, kein Flash-Codes")
    void aktivierenMitFalschemCode() throws Exception {
        when(twoFaService.aktivieren(eq(USER_EMAIL), any(), eq("000000")))
                .thenReturn(TwoFaService.TwoFaAktivierungsErgebnis.UNGUELTIG);

        mockMvc.perform(post("/einstellungen/2fa/aktivieren").param("code", "000000").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/einstellungen/2fa"))
                .andExpect(flash().attributeExists("fehlermeldung"))
                .andExpect(flash().attribute(TwoFaSetupController.FLASH_NEW_BACKUP_CODES, (Object) null));
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    @DisplayName("AUTH-2FA-09: POST /deaktivieren — Service-False → Fehler, Service-True → Erfolg")
    void deaktivierenServiceErgebnisGespiegelt() throws Exception {
        when(twoFaService.deaktivieren(eq(USER_EMAIL), eq("falsch"), eq("000000"))).thenReturn(false);
        when(twoFaService.deaktivieren(eq(USER_EMAIL), eq("dev"), eq("123456"))).thenReturn(true);

        mockMvc.perform(post("/einstellungen/2fa/deaktivieren")
                        .param("aktuellesPasswort", "falsch").param("code", "000000").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("fehlermeldung"));

        mockMvc.perform(post("/einstellungen/2fa/deaktivieren")
                        .param("aktuellesPasswort", "dev").param("code", "123456").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("erfolgsMeldung"));
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    @DisplayName("AUTH-2FA-09b: POST /backup-codes/regenerieren — empty → Fehler, present → Flash mit Codes")
    void backupCodesRegenerieren() throws Exception {
        when(twoFaService.regeneriereBackupCodes(eq(USER_EMAIL), eq("000000")))
                .thenReturn(Optional.empty());
        when(twoFaService.regeneriereBackupCodes(eq(USER_EMAIL), eq("123456")))
                .thenReturn(Optional.of(List.of("XXXX", "YYYY")));

        mockMvc.perform(post("/einstellungen/2fa/backup-codes/regenerieren")
                        .param("code", "000000").with(csrf()))
                .andExpect(flash().attributeExists("fehlermeldung"));

        mockMvc.perform(post("/einstellungen/2fa/backup-codes/regenerieren")
                        .param("code", "123456").with(csrf()))
                .andExpect(flash().attributeExists(TwoFaSetupController.FLASH_NEW_BACKUP_CODES))
                .andExpect(flash().attributeExists("erfolgsMeldung"));
    }
}
