package ch.sponsorplatz.shared.einstellungen;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lese-/Schreib-Zugriff auf die Singleton-Row {@link PlattformEinstellungen}.
 * Die Row wird bei Migration V15 angelegt und niemals gelöscht.
 */
@Service
@Transactional
public class PlattformEinstellungenService {

    private final PlattformEinstellungenRepository repository;

    public PlattformEinstellungenService(PlattformEinstellungenRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PlattformEinstellungen lade() {
        return repository.findById(PlattformEinstellungen.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Singleton-Row für PlattformEinstellungen fehlt — Migration V15 nicht gelaufen?"));
    }

    public PlattformEinstellungen speichere(PlattformEinstellungen einstellungen, String aktualisiertVon) {
        einstellungen.setId(PlattformEinstellungen.SINGLETON_ID);
        einstellungen.setAktualisiertVon(aktualisiertVon);
        return repository.save(einstellungen);
    }
}
