package ch.sponsorplatz.benutzer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/**
 * Schmaler Helper für {@link TwoFaLoginController}: installiert die nach
 * dem 2.-Faktor-Schritt gültige Authentication in den SecurityContext und
 * persistiert sie in der HTTP-Session (damit Folge-Requests authentisch
 * sind).
 *
 * <p>Existiert nur, weil ArchUnit (ARCH-01) keinen direkten Repository-
 * Typ-Bezug aus einer Controller-Klasse erlaubt — und Springs
 * {@code HttpSessionSecurityContextRepository} trägt das Wort
 * "Repository" im Namen. Die Logik selbst ist einzeilig.
 */
@Component
public class TwoFaLoginSession {

    private final SecurityContextRepository sessionStore = new HttpSessionSecurityContextRepository();

    public void installAuthentication(Authentication authentication,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(authentication);
        SecurityContextHolder.setContext(ctx);
        sessionStore.saveContext(ctx, request, response);
    }
}
