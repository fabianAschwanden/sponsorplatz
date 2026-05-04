package ch.sponsorplatz.exception;

/**
 * Wird geworfen, wenn eine angeforderte Ressource (Slug, ID) nicht existiert.
 * Wird vom GlobalExceptionHandler auf HTTP 404 + error-View gemappt.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
