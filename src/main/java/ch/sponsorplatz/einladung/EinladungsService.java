package ch.sponsorplatz.einladung;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.organisation.MitgliedschaftService;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrganisationRepository;
import ch.sponsorplatz.organisation.Rolle;
import ch.sponsorplatz.shared.exception.BenutzerNichtRegistriertException;
import ch.sponsorplatz.shared.util.TokenGenerator;

/**
 * Service für den Mitglieder-Einladungs-Flow.
 *
 * <p>
 * Der Mail-Versand selbst läuft NICHT mehr in dieser Service-Methode, sondern
 * im {@link EinladungsMailListener} via
 * {@code @TransactionalEventListener(AFTER_COMMIT)}.
 * Damit kann eine Mail-Failure nicht den DB-State korrumpieren (H4-Fix):
 * der Service publiziert {@link EinladungErstelltEvent}, Spring stellt das
 * Event
 * erst nach erfolgreichem Tx-Commit zu.
 * </p>
 */
@Service
@Transactional
public class EinladungsService {

    private static final long TOKEN_GUELTIG_TAGE = 7;

    /**
     * Vereinfachter RFC-5322-Check. Akzeptiert die meisten gültigen E-Mails und
     * blockt
     * offensichtlichen Müll (kein „@", fehlende TLD, Leerzeichen, doppelte „@"
     * usw.).
     * Bewusst nicht der Hibernate-{@code @Email}-Validator, weil EinladungsService
     * keinen
     * Form-DTO-Wrapper hat — Defense in depth direkt am Service-Eingang.
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final EinladungRepository einladungRepository;
    private final AppUserRepository appUserRepository;
    private final OrganisationRepository organisationRepository;
    private final MitgliedschaftService mitgliedschaftService;
    private final ApplicationEventPublisher eventPublisher;

    public EinladungsService(EinladungRepository einladungRepository,
            AppUserRepository appUserRepository,
            OrganisationRepository organisationRepository,
            MitgliedschaftService mitgliedschaftService,
            ApplicationEventPublisher eventPublisher) {
        this.einladungRepository = einladungRepository;
        this.appUserRepository = appUserRepository;
        this.organisationRepository = organisationRepository;
        this.mitgliedschaftService = mitgliedschaftService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Erstellt eine Einladung und publiziert ein {@link EinladungErstelltEvent}.
     * Die Mail wird vom Listener nach AFTER_COMMIT versendet.
     *
     * @throws IllegalArgumentException bei ungültiger E-Mail oder wenn bereits
     *                                  eingeladen
     */
    public Einladung erstelleEinladung(UUID orgId, String email, Rolle rolle, UUID eingeladenVonId) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("E-Mail darf nicht leer sein");
        }
        String normalisiert = email.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(normalisiert).matches()) {
            throw new IllegalArgumentException("E-Mail-Format ist ungültig: " + email);
        }

        Organisation org = organisationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organisation nicht gefunden"));
        AppUser eingeladenVon = appUserRepository.findById(eingeladenVonId)
                .orElseThrow(() -> new IllegalArgumentException("Einladender User nicht gefunden"));

        // M2: existierende Einladung berücksichtigen — abgelaufene löschen + neu
        // erlauben,
        // gültige weiterhin blocken (sonst könnte ein Editor unbegrenzt Mails
        // triggern).
        einladungRepository.findByOrgIdAndEmail(orgId, normalisiert).ifPresent(existing -> {
            if (Instant.now().isAfter(existing.getGueltigBis())) {
                einladungRepository.delete(existing);
            } else {
                throw new IllegalArgumentException("Es existiert bereits eine gültige Einladung für diese E-Mail");
            }
        });

        String token = TokenGenerator.generiere();

        Einladung einladung = new Einladung();
        einladung.setOrg(org);
        einladung.setEmail(normalisiert);
        einladung.setRolle(rolle);
        einladung.setToken(token);
        einladung.setEingeladenVon(eingeladenVon);
        einladung.setGueltigBis(Instant.now().plus(TOKEN_GUELTIG_TAGE, ChronoUnit.DAYS));

        Einladung gespeichert = einladungRepository.save(einladung);

        eventPublisher.publishEvent(new EinladungErstelltEvent(
                token, normalisiert, org.getName(), eingeladenVon.getAnzeigename(), rolle));

        return gespeichert;
    }

    /**
     * Lädt eine Vorschau der Einladung — nur lesend, keine State-Änderung.
     * Wird auf dem GET-Endpunkt verwendet, um Outlook-/Slack-Crawler nicht
     * versehentlich die Einladung konsumieren zu lassen (K3-Fix).
     *
     * @throws IllegalArgumentException bei unbekanntem Token
     * @throws IllegalStateException    bei abgelaufenem Token
     */
    @Transactional(readOnly = true)
    public EinladungVorschauView ladeVorschau(String token) {
        Einladung einladung = pruefeUndLade(token);
        return EinladungVorschauView.von(einladung);
    }

    /**
     * Nimmt eine Einladung an. Erstellt die Mitgliedschaft und markiert die
     * Einladung
     * als angenommen (M4-Fix: idempotent — wiederholte Klicks auf denselben Token
     * geben kein 400, sondern werden stillschweigend akzeptiert, solange der Token
     * noch nicht durch den Cleanup-Job entfernt wurde).
     *
     * @throws IllegalArgumentException bei unbekanntem Token
     * @throws IllegalStateException    bei abgelaufenem Token oder wenn der User
     *                                  noch nicht registriert ist
     */
    public void nimmAn(String token) {
        Einladung einladung = pruefeUndLade(token);

        // M4: bereits angenommen → idempotent OK, keine doppelte Mitgliedschaft
        if (einladung.getAngenommenAm() != null) {
            return;
        }

        // User muss bereits registriert sein mit dieser E-Mail
        AppUser user = appUserRepository.findByEmail(einladung.getEmail())
                .orElseThrow(() -> new BenutzerNichtRegistriertException(einladung.getEmail()));

        mitgliedschaftService.fuegeHinzu(
                einladung.getOrg().getId(),
                user.getId(),
                einladung.getRolle(),
                einladung.getEingeladenVon().getId());

        einladung.setAngenommenAm(Instant.now());
        einladungRepository.save(einladung);
    }

    /**
     * Lädt die Einladung und validiert Existenz + Ablauf. Wird sowohl von
     * {@link #ladeVorschau} (read-only) als auch von {@link #nimmAn} verwendet,
     * damit beide Pfade konsistent prüfen.
     */
    private Einladung pruefeUndLade(String token) {
        Einladung einladung = einladungRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Einladungs-Token ist ungültig"));
        if (Instant.now().isAfter(einladung.getGueltigBis())) {
            throw new IllegalStateException("Einladungs-Token ist abgelaufen");
        }
        return einladung;
    }

}
