package ch.sponsorplatz.crm;

import ch.sponsorplatz.organisation.AccessControl;
import ch.sponsorplatz.shared.exception.NotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service für {@link KontaktPerson} (Dynamics „Contact"). Jede Methode prüft
 * {@link AccessControl#kannSponsorDatenSehen} gegen den Mandanten-Schlüssel —
 * beim Lesen/Anlegen über den Account, beim Löschen über den denormalisierten
 * Besitzer des Kontakts. Kein Pfad zu Kontaktdaten ohne diese Schranke.
 */
@Service
@Transactional
public class KontaktPersonService {

    private final KontaktPersonRepository repository;
    private final SponsorAccountRepository accountRepository;
    private final AccessControl accessControl;

    public KontaktPersonService(KontaktPersonRepository repository,
                                SponsorAccountRepository accountRepository,
                                AccessControl accessControl) {
        this.repository = repository;
        this.accountRepository = accountRepository;
        this.accessControl = accessControl;
    }

    @Transactional(readOnly = true)
    public List<KontaktPersonView> findeKontakte(UUID accountId, Authentication auth) {
        ladePruefe(accountId, auth);
        return KontaktPersonView.von(repository.findByAccountIdOrderByNachnameAscVornameAsc(accountId));
    }

    public KontaktPersonView erstelle(UUID accountId, String vorname, String nachname,
                                      String funktion, KontaktRolle rolle,
                                      String email, String telefon, String mobile,
                                      Authentication auth) {
        SponsorAccount account = ladePruefe(accountId, auth);

        KontaktPerson k = new KontaktPerson();
        k.setAccount(account);
        k.setBesitzerSponsorOrgId(account.getBesitzerSponsorOrgId());
        k.setVorname(vorname);
        k.setNachname(nachname);
        k.setFunktion(funktion);
        k.setKontaktRolle(rolle != null ? rolle : KontaktRolle.SONSTIGE);
        k.setEmail(email);
        k.setTelefon(telefon);
        k.setMobile(mobile);
        return KontaktPersonView.von(repository.save(k));
    }

    public void loesche(UUID kontaktId, Authentication auth) {
        KontaktPerson k = repository.findById(kontaktId)
                .orElseThrow(() -> new NotFoundException("Kontakt nicht gefunden: " + kontaktId));
        if (!accessControl.kannSponsorDatenSehen(k.getBesitzerSponsorOrgId(), auth)) {
            throw new AccessDeniedException("Kein Zugriff auf diesen Kontakt");
        }
        repository.delete(k);
    }

    /** Lädt den Account und prüft den Sponsor-Zugriff — gemeinsamer Guard. */
    private SponsorAccount ladePruefe(UUID accountId, Authentication auth) {
        SponsorAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account nicht gefunden: " + accountId));
        if (!accessControl.kannSponsorDatenSehen(account.getBesitzerSponsorOrgId(), auth)) {
            throw new AccessDeniedException("Kein Zugriff auf die Kontakte dieses Accounts");
        }
        return account;
    }
}
