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

    private ModelAndView errorView(HttpStatus status, String error, String message) {
        ModelAndView mav = new ModelAndView(ERROR_VIEW);
        mav.setStatus(status);
        mav.addObject("status", status.value());
        mav.addObject("error", error);
        mav.addObject("message", message);
        return mav;
    }
}
