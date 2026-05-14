package ch.sponsorplatz.aufgabe;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AufgabenDefinitionRepository extends JpaRepository<AufgabenDefinition, UUID> {

    List<AufgabenDefinition> findAllByOrderByTitelAsc();

    /** Aktive Definitionen, die auf den (entityTyp, triggerStatus)-Wechsel reagieren. */
    List<AufgabenDefinition> findByAktivTrueAndTriggerEntityTypAndTriggerStatus(
            TriggerEntityTyp triggerEntityTyp, String triggerStatus);
}
