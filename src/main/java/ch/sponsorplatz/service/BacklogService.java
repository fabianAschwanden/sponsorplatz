package ch.sponsorplatz.service;

import ch.sponsorplatz.exception.NotFoundException;
import ch.sponsorplatz.model.BacklogItem;
import ch.sponsorplatz.model.BacklogPrioritaet;
import ch.sponsorplatz.model.BacklogStatus;
import ch.sponsorplatz.repository.BacklogItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * CRUD für den Feature-Backlog. Custom-Sortierung in Java statt JPQL,
 * weil JPQL keine Enum-ordinal-basierte Reihenfolge sauber unterstützt:
 *
 * <ol>
 *   <li>Abgeschlossen ans Ende (ERLEDIGT/VERWORFEN)</li>
 *   <li>Status-Reihenfolge OFFEN → IN_ARBEIT → ERLEDIGT → VERWORFEN</li>
 *   <li>Priorität HOCH → MITTEL → NIEDRIG</li>
 *   <li>Erstell-Datum DESC (neueste zuerst)</li>
 * </ol>
 */
@Service
public class BacklogService {

    private static final Comparator<BacklogItem> SORT_ORDER =
            Comparator.comparing((BacklogItem b) -> b.getStatus().istAbgeschlossen())
                    .thenComparing(b -> b.getStatus().ordinal())
                    .thenComparing(b -> b.getPrioritaet().ordinal())
                    .thenComparing(BacklogItem::getErstelltAm,
                            Comparator.nullsLast(Comparator.reverseOrder()));

    private final BacklogItemRepository repository;

    public BacklogService(BacklogItemRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<BacklogItem> findeAlleSortiert() {
        return repository.findAll().stream().sorted(SORT_ORDER).toList();
    }

    @Transactional(readOnly = true)
    public Optional<BacklogItem> findeNachId(UUID id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public long zaehleOffen() {
        return repository.countByStatus(BacklogStatus.OFFEN)
                + repository.countByStatus(BacklogStatus.IN_ARBEIT);
    }

    @Transactional
    public BacklogItem erstelle(String titel, String beschreibung,
                                BacklogPrioritaet prioritaet, String erstelltVon) {
        BacklogItem item = new BacklogItem();
        item.setTitel(titel);
        item.setBeschreibung(blankToNull(beschreibung));
        item.setStatus(BacklogStatus.OFFEN);
        item.setPrioritaet(prioritaet != null ? prioritaet : BacklogPrioritaet.MITTEL);
        item.setErstelltVon(erstelltVon);
        return repository.save(item);
    }

    @Transactional
    public BacklogItem aktualisiere(UUID id, String titel, String beschreibung,
                                    BacklogPrioritaet prioritaet) {
        BacklogItem item = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Backlog-Item nicht gefunden: " + id));
        item.setTitel(titel);
        item.setBeschreibung(blankToNull(beschreibung));
        if (prioritaet != null) item.setPrioritaet(prioritaet);
        return repository.save(item);
    }

    @Transactional
    public BacklogItem aendereStatus(UUID id, BacklogStatus neuerStatus) {
        BacklogItem item = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Backlog-Item nicht gefunden: " + id));
        item.setStatus(neuerStatus);
        // Erledigt-Zeitstempel automatisch setzen / zurücksetzen
        if (neuerStatus == BacklogStatus.ERLEDIGT && item.getErledigtAm() == null) {
            item.setErledigtAm(Instant.now());
        } else if (neuerStatus == BacklogStatus.OFFEN || neuerStatus == BacklogStatus.IN_ARBEIT) {
            item.setErledigtAm(null);
        }
        return repository.save(item);
    }

    @Transactional
    public void loesche(UUID id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Backlog-Item nicht gefunden: " + id);
        }
        repository.deleteById(id);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
