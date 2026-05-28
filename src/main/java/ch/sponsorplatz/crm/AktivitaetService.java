package ch.sponsorplatz.crm;

import ch.sponsorplatz.organisation.AccessControl;
import ch.sponsorplatz.shared.exception.NotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service für {@link Aktivitaet} (Dynamics „Activity"). Jede Methode prüft
 * {@link AccessControl#kannSponsorDatenSehen} gegen den Mandanten-Schlüssel.
 * Ein optionaler {@link KontaktPerson}-Bezug wird validiert: der Kontakt muss
 * zum selben Account gehören (verhindert Cross-Account-Verknüpfung per
 * ID-Guessing).
 */
@Service
@Transactional
public class AktivitaetService {

    private final AktivitaetRepository repository;
    private final SponsorAccountRepository accountRepository;
    private final KontaktPersonRepository kontaktRepository;
    private final AccessControl accessControl;

    public AktivitaetService(AktivitaetRepository repository,
                             SponsorAccountRepository accountRepository,
                             KontaktPersonRepository kontaktRepository,
                             AccessControl accessControl) {
        this.repository = repository;
        this.accountRepository = accountRepository;
        this.kontaktRepository = kontaktRepository;
        this.accessControl = accessControl;
    }

    @Transactional(readOnly = true)
    public List<AktivitaetView> findeTimeline(UUID accountId, Authentication auth) {
        ladePruefe(accountId, auth);
        return AktivitaetView.von(repository.findByAccountIdOrderByDatumDescErstelltAmDesc(accountId));
    }

    public AktivitaetView erstelle(UUID accountId, AktivitaetTyp typ, LocalDate datum,
                                   String betreff, String notiz, UUID kontaktPersonId,
                                   Authentication auth) {
        SponsorAccount account = ladePruefe(accountId, auth);

        Aktivitaet a = new Aktivitaet();
        a.setAccount(account);
        a.setBesitzerSponsorOrgId(account.getBesitzerSponsorOrgId());
        a.setTyp(typ != null ? typ : AktivitaetTyp.NOTIZ);
        a.setDatum(datum);
        a.setBetreff(betreff);
        a.setNotiz(notiz);
        if (kontaktPersonId != null) {
            a.setKontaktPerson(ladeKontaktImAccount(kontaktPersonId, accountId));
        }
        return AktivitaetView.von(repository.save(a));
    }

    public void loesche(UUID aktivitaetId, Authentication auth) {
        Aktivitaet a = repository.findById(aktivitaetId)
                .orElseThrow(() -> new NotFoundException("Aktivität nicht gefunden: " + aktivitaetId));
        if (!accessControl.kannSponsorDatenSehen(a.getBesitzerSponsorOrgId(), auth)) {
            throw new AccessDeniedException("Kein Zugriff auf diese Aktivität");
        }
        repository.delete(a);
    }

    private SponsorAccount ladePruefe(UUID accountId, Authentication auth) {
        SponsorAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account nicht gefunden: " + accountId));
        if (!accessControl.kannSponsorDatenSehen(account.getBesitzerSponsorOrgId(), auth)) {
            throw new AccessDeniedException("Kein Zugriff auf die Aktivitäten dieses Accounts");
        }
        return account;
    }

    /** Validiert dass der Kontakt zum selben Account gehört (kein Cross-Account-Leak). */
    private KontaktPerson ladeKontaktImAccount(UUID kontaktPersonId, UUID accountId) {
        KontaktPerson k = kontaktRepository.findById(kontaktPersonId)
                .orElseThrow(() -> new NotFoundException("Kontakt nicht gefunden: " + kontaktPersonId));
        if (!k.getAccount().getId().equals(accountId)) {
            throw new IllegalArgumentException("Kontakt gehört nicht zu diesem Account");
        }
        return k;
    }
}
