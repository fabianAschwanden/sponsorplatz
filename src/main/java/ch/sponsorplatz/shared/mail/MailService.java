package ch.sponsorplatz.shared.mail;
import ch.sponsorplatz.shared.einstellungen.PlattformEinstellungenService;

import ch.sponsorplatz.shared.einstellungen.PlattformEinstellungen;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * Zentraler Mail-Versand für die Plattform.
 *
 * <h2>Effektive Konfiguration</h2>
 * Priorität pro Setting: <strong>DB ({@link PlattformEinstellungen}) &gt;
 * ENV/Property &gt; leer</strong>. Settings sind im Admin-UI unter
 * {@code /admin/mail-einstellungen} editierbar; Änderungen greifen ohne
 * Container-Restart, weil pro {@code send}-Call ein neuer
 * {@link JavaMailSenderImpl} aus den effektiven Werten gebaut wird.
 *
 * <h2>Live-Schalter (NUR via ENV)</h2>
 * <ul>
 *   <li>{@code MAIL_LIVE=true} — Mails gehen an echte Empfänger.</li>
 *   <li>{@code MAIL_LIVE=false} + Test-Empfänger (DB &gt; ENV) gesetzt →
 *       Umleitung auf Test-Adresse, Subject mit {@code [TEST → original]}-Prefix.</li>
 *   <li>{@code MAIL_LIVE=false} + Test-Empfänger leer → Versand wird
 *       übersprungen, WARN-Log mit Empfänger + Subject.</li>
 * </ul>
 *
 * <p>{@code MAIL_LIVE} ist <strong>bewusst nur ENV</strong> — kein UI-Toggle,
 * damit ein versehentlicher Klick nie zu echten Mails führen kann.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender fallbackSender;
    private final PlattformEinstellungenService einstellungenService;

    private final String envHost;
    private final int envPort;
    private final String envUser;
    private final String envPassword;
    private final boolean envSmtpAuth;
    private final boolean envSmtpStarttls;
    private final String envAbsender;
    private final String envTestEmpfaenger;
    private final boolean liveMode;

    public MailService(JavaMailSender fallbackSender,
                       PlattformEinstellungenService einstellungenService,
                       @Value("${spring.mail.host:}") String envHost,
                       @Value("${spring.mail.port:587}") int envPort,
                       @Value("${spring.mail.username:}") String envUser,
                       @Value("${spring.mail.password:}") String envPassword,
                       @Value("${spring.mail.properties.mail.smtp.auth:true}") boolean envSmtpAuth,
                       @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}") boolean envSmtpStarttls,
                       @Value("${sponsorplatz.mail.absender:noreply@sponsorplatz.ch}") String envAbsender,
                       @Value("${sponsorplatz.mail.test-empfaenger:}") String envTestEmpfaenger,
                       @Value("${sponsorplatz.mail.live:false}") boolean liveMode) {
        this.fallbackSender = fallbackSender;
        this.einstellungenService = einstellungenService;
        this.envHost = nvl(envHost);
        this.envPort = envPort;
        this.envUser = nvl(envUser);
        this.envPassword = nvl(envPassword);
        this.envSmtpAuth = envSmtpAuth;
        this.envSmtpStarttls = envSmtpStarttls;
        this.envAbsender = nvl(envAbsender);
        this.envTestEmpfaenger = nvl(envTestEmpfaenger).trim();
        this.liveMode = liveMode;

        if (liveMode) {
            log.warn("Mail-LIVE-MODUS AKTIV — Mails gehen an echte Empfänger-Adressen!");
        } else {
            log.info("Mail-Live-Modus AUS — Mails werden an Test-Empfänger umgeleitet oder übersprungen");
        }
    }

    /** True, wenn ein SMTP-Host effektiv konfiguriert ist (DB oder ENV). */
    public boolean istKonfiguriert() {
        return !effektiverHost().isBlank();
    }

    public boolean istLiveMode() {
        return liveMode;
    }

    public String effektiverHost() {
        String dbVal = nvl(einstellungen().getSmtpHost()).trim();
        return !dbVal.isBlank() ? dbVal : envHost;
    }

    public String effektiverTestEmpfaenger() {
        String dbVal = nvl(einstellungen().getMailTestEmpfaenger()).trim();
        return !dbVal.isBlank() ? dbVal : envTestEmpfaenger;
    }

    public String effektiverAbsender() {
        String dbVal = nvl(einstellungen().getMailAbsender()).trim();
        return !dbVal.isBlank() ? dbVal : envAbsender;
    }

    /**
     * Sendet eine einfache Plain-Text-Mail.
     */
    public void sendePlain(String to, String subject, String body) {
        Routing routing = wendeLiveModusAn(to, subject);
        if (routing.skip()) return;

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(routing.to());
        msg.setFrom(effektiverAbsender());
        msg.setSubject(routing.subject());
        msg.setText(body);
        sender().send(msg);
    }

    /**
     * Sendet eine HTML-Mail. Der {@code helperConfigurer} bekommt einen
     * fertig konfigurierten {@link MimeMessageHelper} mit gesetztem
     * {@code to}, {@code from} und {@code subject} — Body und ggf.
     * weitere Attribute werden vom Aufrufer ergänzt.
     */
    public void sendeHtml(String to, String subject, Consumer<MimeMessageHelper> helperConfigurer) {
        Routing routing = wendeLiveModusAn(to, subject);
        if (routing.skip()) return;

        JavaMailSender s = sender();
        MimeMessage mime = s.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
            helper.setTo(routing.to());
            helper.setFrom(effektiverAbsender());
            helper.setSubject(routing.subject());
            helperConfigurer.accept(helper);
        } catch (MessagingException e) {
            throw new MailSendException("MimeMessage konnte nicht aufgebaut werden", e);
        }
        s.send(mime);
    }

    /**
     * Sendet eine Test-Mail an die im UI hinterlegte Test-Empfänger-Adresse.
     * Geht direkt — kein Live-Mode-Routing —, damit der Admin-Klick auf
     * "Test-Mail senden" auch im AUS-Modus funktioniert.
     *
     * @throws IllegalStateException wenn kein Test-Empfänger konfiguriert ist
     */
    public void sendeTestMail() {
        String empfaenger = effektiverTestEmpfaenger();
        if (empfaenger.isBlank()) {
            throw new IllegalStateException(
                    "Kein Test-Empfänger konfiguriert — bitte unter Mail-Einstellungen setzen");
        }

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(empfaenger);
        msg.setFrom(effektiverAbsender());
        msg.setSubject("Sponsorplatz — Test-Mail");
        msg.setText("""
                Diese Test-Mail bestätigt, dass der SMTP-Versand korrekt konfiguriert ist.

                Effektiver Host:    %s
                Absender:           %s
                Live-Mode:          %s

                Zeitstempel: %s
                """.formatted(
                        effektiverHost(),
                        effektiverAbsender(),
                        liveMode ? "AN — Mails gehen an echte Empfänger" : "AUS — Mails werden umgeleitet",
                        java.time.LocalDateTime.now()));
        sender().send(msg);
    }

    // ── Effective-Settings (DB > ENV) ─────────────────────────────────────

    private PlattformEinstellungen einstellungen() {
        return einstellungenService.lade();
    }

    private int effektivePort() {
        Integer dbVal = einstellungen().getSmtpPort();
        return dbVal != null && dbVal > 0 ? dbVal : envPort;
    }

    private String effektiverUser() {
        String dbVal = nvl(einstellungen().getSmtpUser()).trim();
        return !dbVal.isBlank() ? dbVal : envUser;
    }

    private String effektivesPassword() {
        String dbVal = nvl(einstellungen().getSmtpPassword());
        return !dbVal.isBlank() ? dbVal : envPassword;
    }

    private boolean effektiveAuth() {
        return !nvl(einstellungen().getSmtpHost()).isBlank() ? einstellungen().isSmtpAuth() : envSmtpAuth;
    }

    private boolean effektiveStarttls() {
        return !nvl(einstellungen().getSmtpHost()).isBlank() ? einstellungen().isSmtpStarttls() : envSmtpStarttls;
    }

    /**
     * Liefert einen JavaMailSender mit den effektiven Settings. Wenn die DB
     * keinen Host konfiguriert, wird der Spring-injected Fallback-Sender
     * (aus ENV-Properties) verwendet. Sonst pro Call ein neuer Sender mit
     * den aktuellen DB-Werten — Settings-Änderungen greifen ohne Restart.
     */
    private JavaMailSender sender() {
        String dbHost = nvl(einstellungen().getSmtpHost()).trim();
        if (dbHost.isBlank()) {
            return fallbackSender;
        }
        JavaMailSenderImpl s = new JavaMailSenderImpl();
        s.setHost(dbHost);
        s.setPort(effektivePort());
        s.setUsername(effektiverUser());
        s.setPassword(effektivesPassword());
        Properties props = new Properties();
        props.put("mail.smtp.auth", String.valueOf(effektiveAuth()));
        props.put("mail.smtp.starttls.enable", String.valueOf(effektiveStarttls()));
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        s.setJavaMailProperties(props);
        return s;
    }

    // ── Live-Mode-Routing ─────────────────────────────────────────────────

    private Routing wendeLiveModusAn(@NonNull String originalTo, @NonNull String originalSubject) {
        if (liveMode) {
            return new Routing(originalTo, originalSubject, false);
        }
        String testRcpt = effektiverTestEmpfaenger();
        if (!testRcpt.isBlank()) {
            String markedSubject = "[TEST → " + originalTo + "] " + originalSubject;
            log.info("Live-Modus AUS — Mail an '{}' wird an Test-Empfänger '{}' umgeleitet",
                    originalTo, testRcpt);
            return new Routing(testRcpt, markedSubject, false);
        }
        log.warn("Live-Modus AUS — Versand an '{}' (Subject: '{}') wurde übersprungen. "
                        + "Setze MAIL_LIVE=true oder Mail-Test-Empfänger unter /admin/mail-einstellungen.",
                originalTo, originalSubject);
        return new Routing("", "", true);
    }

    private record Routing(String to, String subject, boolean skip) {}

    private static @NonNull String nvl(@Nullable String s) {
        return s == null ? "" : s;
    }
}
