package ch.sponsorplatz.projekt;

import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrganisationRepository;
import ch.sponsorplatz.shared.exception.NotFoundException;
import ch.sponsorplatz.shared.util.SlugGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Service fuer Vereins-Events (CRUD + kommende Events fuer Dashboard).
 */
@Service
@Transactional
public class EventService {

    private final EventRepository repository;
    private final OrganisationRepository orgRepository;
    private final SlugGenerator slugGenerator;

    public EventService(EventRepository repository, OrganisationRepository orgRepository,
                        SlugGenerator slugGenerator) {
        this.repository = repository;
        this.orgRepository = orgRepository;
        this.slugGenerator = slugGenerator;
    }

    public Event erstelle(UUID orgId, String name, String beschreibung, String ort,
                          LocalDate datum, LocalDate datumEnde, Integer kapazitaet) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Event-Name darf nicht leer sein");
        }
        validiereDatumUndKapazitaet(datum, datumEnde, kapazitaet);
        Organisation org = orgRepository.findById(orgId)
                .orElseThrow(() -> new NotFoundException("Organisation nicht gefunden: " + orgId));

        Event event = new Event();
        event.setOrg(org);
        event.setName(name.trim());
        // findeFreienSlug verhindert hässliche 500-DB-UniqueViolation bei
        // Namens-Kollision — hängt -1, -2, … an, bis ein freier Slug gefunden ist.
        event.setSlug(slugGenerator.findeFreienSlug(
                name, kandidat -> repository.findBySlug(kandidat).isPresent()));
        event.setBeschreibung(beschreibung);
        event.setOrt(ort);
        event.setDatum(datum);
        event.setDatumEnde(datumEnde);
        event.setKapazitaet(kapazitaet);
        return repository.save(event);
    }

    public Event aktualisiere(UUID eventId, String name, String beschreibung, String ort,
                              LocalDate datum, LocalDate datumEnde, Integer kapazitaet) {
        validiereDatumUndKapazitaet(datum, datumEnde, kapazitaet);
        Event event = findeNachId(eventId);
        if (name != null && !name.isBlank()) {
            event.setName(name.trim());
        }
        // Slug bewusst NICHT mit dem geänderten Namen neu berechnen — Slugs
        // sind URL-stabil. Wer den Slug wirklich ändern will, löscht das Event
        // und legt es neu an.
        event.setBeschreibung(beschreibung);
        event.setOrt(ort);
        event.setDatum(datum);
        event.setDatumEnde(datumEnde);
        event.setKapazitaet(kapazitaet);
        return repository.save(event);
    }

    /**
     * Datum-Pflicht, End-Datum darf nicht vor Start-Datum liegen, Kapazität
     * muss positiv sein wenn gesetzt. Wirft {@link IllegalArgumentException}.
     */
    private void validiereDatumUndKapazitaet(LocalDate datum, LocalDate datumEnde, Integer kapazitaet) {
        if (datum == null) {
            throw new IllegalArgumentException("Datum ist Pflicht");
        }
        if (datumEnde != null && datumEnde.isBefore(datum)) {
            throw new IllegalArgumentException(
                    "End-Datum darf nicht vor dem Start-Datum liegen");
        }
        if (kapazitaet != null && kapazitaet <= 0) {
            throw new IllegalArgumentException("Kapazität muss eine positive Zahl sein");
        }
    }

    public void loesche(UUID eventId) {
        Event event = findeNachId(eventId);
        repository.delete(event);
    }

    @Transactional(readOnly = true)
    public Event findeNachId(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event nicht gefunden: " + id));
    }

    @Transactional(readOnly = true)
    public List<Event> findeNachOrg(UUID orgId) {
        return repository.findByOrgIdOrderByDatumAsc(orgId);
    }

    /** View-Variante — Controller braucht keine Entity-Liste (ARCH-02). */
    @Transactional(readOnly = true)
    public List<EventView> findeViewsNachOrg(UUID orgId) {
        return EventView.von(findeNachOrg(orgId));
    }

    /**
     * Kommende Events fuer die angegebenen Orgs, sortiert nach Datum aufsteigend.
     * Wird vom DashboardService verwendet.
     */
    @Transactional(readOnly = true)
    public List<Event> findeKommendeNachOrgIds(Collection<UUID> orgIds, int limit) {
        if (orgIds.isEmpty()) {
            return List.of();
        }
        return repository.findByOrgIdInAndDatumGreaterThanEqualOrderByDatumAsc(orgIds, LocalDate.now())
                .stream()
                .limit(limit)
                .toList();
    }
}

