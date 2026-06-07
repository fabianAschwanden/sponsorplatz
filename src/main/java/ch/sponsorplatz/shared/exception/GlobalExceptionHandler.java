package ch.sponsorplatz.shared.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * Zentraler Exception-Handler. Mappt fachliche Exceptions auf HTTP-Statuscodes
 * und rendert die Thymeleaf-Error-Page (`error.html`).
 *
 * Mapping:
 *   - NotFoundException        → 404
 *   - IllegalArgumentException → 400
 *   - IllegalStateException    → 409
 *   - AccessDeniedException    → 403
 *   - alles andere             → 500 (mit vollem Stacktrace ins Log/Sentry)
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String ERROR_VIEW = "error";

    @ExceptionHandler(NotFoundException.class)
    public ModelAndView handleNotFound(NotFoundException ex) {
        log.info("Nicht gefunden: {}", ex.getMessage());
        return errorView(HttpStatus.NOT_FOUND, "Nicht gefunden", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ModelAndView handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Validierungsfehler: {}", ex.getMessage());
        return errorView(HttpStatus.BAD_REQUEST, "Ungültige Anfrage", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ModelAndView handleIllegalState(IllegalStateException ex) {
        log.warn("Inkonsistenter Zustand: {}", ex.getMessage());
        return errorView(HttpStatus.CONFLICT, "Konflikt", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView handleAccessDenied(AccessDeniedException ex) {
        log.warn("Zugriff verweigert: {}", ex.getMessage());
        return errorView(HttpStatus.FORBIDDEN, "Zugriff verweigert", "Sie haben keine Berechtigung für diese Aktion.");
    }

    /**
     * Auffangnetz für alles Unerwartete (z.B. {@code Error}-Subtypen aus dem
     * AWT/Java2D-Font-Subsystem, die bisher als roher 500 ohne Kontext
     * durchschlugen). Wir loggen den <strong>vollen Stacktrace</strong> — der
     * landet im Ops-RecentErrors-Dashboard und in Sentry — und rendern eine
     * saubere 500-Seite statt eines generischen JSON-Fehlers.
     *
     * <p>Bewusst auf {@link Throwable} (nicht nur Exception), damit auch
     * {@code InternalError} u.ä. erfasst werden. Spring ruft den Handler nur,
     * wenn kein spezifischerer @ExceptionHandler greift.
     *
     * <p><strong>Wichtig:</strong> Spring-eigene MVC-Exceptions (fehlender/
     * ungültiger {@code @RequestBody} → 400, falsche Methode → 405, …) werden
     * <em>durchgereicht</em>, damit Springs {@code DefaultHandlerExceptionResolver}
     * sie auf die korrekten Statuscodes mappt — sonst würde dieser catch-all
     * fälschlich jede dieser Anfragen auf 500 ziehen.
     */
    @ExceptionHandler(Throwable.class)
    public ModelAndView handleUnerwartet(Throwable ex) throws Throwable {
        if (istSpringMvcException(ex)) {
            throw ex;
        }
        log.error("Unerwarteter Fehler ({}): {}", ex.getClass().getName(), ex.getMessage(), ex);
        return errorView(HttpStatus.INTERNAL_SERVER_ERROR, "Interner Fehler",
                "Es ist ein unerwarteter Fehler aufgetreten. Das Ereignis wurde protokolliert.");
    }

    /**
     * Springs Standard-MVC-Exceptions (Paket {@code org.springframework.web})
     * sowie alles, was {@link org.springframework.web.ErrorResponse} implementiert
     * (trägt seinen eigenen HTTP-Status), gehören NICHT zu uns — der Framework-
     * Resolver kennt deren korrekten Statuscode.
     */
    private static boolean istSpringMvcException(Throwable ex) {
        if (ex instanceof org.springframework.web.ErrorResponse) {
            return true;
        }
        return ex.getClass().getName().startsWith("org.springframework.web.");
    }

    private ModelAndView errorView(HttpStatus status, String error, String message) {
        ModelAndView mav = new ModelAndView(ERROR_VIEW);
        mav.setStatus(status);
        mav.addObject("status", status.value());
        mav.addObject("error", error);
        mav.addObject("message", message);
        return mav;
    }
}
