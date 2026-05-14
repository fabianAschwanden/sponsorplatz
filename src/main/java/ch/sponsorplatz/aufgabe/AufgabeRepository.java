package ch.sponsorplatz.aufgabe;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface AufgabeRepository extends JpaRepository<Aufgabe, UUID> {

    /** Offene Aufgaben für eine konkrete Entity (z.B. Org X) — Basis der Auto-Erledigung. */
    List<Aufgabe> findByEntityTypAndEntityIdAndStatus(TriggerEntityTyp entityTyp,
                                                      UUID entityId,
                                                      AufgabenStatus status);

    /**
     * Offene Aufgaben für einen User: Vereinigung aus „Org-Aufgaben meiner Mitgliedschafts-Orgs"
     * und „Plattform-Admin-Aufgaben (wenn User Admin ist)".
     *
     * <p>Status-Filter wird im Query festgenagelt, damit der Aufrufer nicht versehentlich
     * geschlossene Aufgaben aufruft. Sortierung: neueste zuerst.
     */
    @Query("""
            select a from Aufgabe a
             where a.status = ch.sponsorplatz.aufgabe.AufgabenStatus.OFFEN
               and (
                    (a.assigneeOrg is not null and a.assigneeOrg.id in :orgIds)
                 or (a.nurPlatformAdmin = true and :istAdmin = true)
               )
             order by a.erstelltAm desc
            """)
    List<Aufgabe> findOffeneFuer(@Param("orgIds") Collection<UUID> orgIds,
                                  @Param("istAdmin") boolean istAdmin);

    /** Hat die Entity bereits eine offene Aufgabe für diese Definition? Idempotenz-Check. */
    boolean existsByDefinitionIdAndEntityIdAndStatus(UUID definitionId, UUID entityId, AufgabenStatus status);

    /** Anzahl offener Aufgaben — für Badge in der Sidebar. */
    @Query("""
            select count(a) from Aufgabe a
             where a.status = ch.sponsorplatz.aufgabe.AufgabenStatus.OFFEN
               and (
                    (a.assigneeOrg is not null and a.assigneeOrg.id in :orgIds)
                 or (a.nurPlatformAdmin = true and :istAdmin = true)
               )
            """)
    long zaehleOffeneFuer(@Param("orgIds") Collection<UUID> orgIds,
                          @Param("istAdmin") boolean istAdmin);
}
