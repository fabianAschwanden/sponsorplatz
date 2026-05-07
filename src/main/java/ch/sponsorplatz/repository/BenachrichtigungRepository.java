package ch.sponsorplatz.repository;

import ch.sponsorplatz.model.Benachrichtigung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BenachrichtigungRepository extends JpaRepository<Benachrichtigung, UUID> {

    List<Benachrichtigung> findByEmpfaengerIdOrderByCreatedAtDesc(UUID empfaengerId);

    List<Benachrichtigung> findTop20ByEmpfaengerIdOrderByCreatedAtDesc(UUID empfaengerId);

    long countByEmpfaengerIdAndGelesenFalse(UUID empfaengerId);

    @Modifying
    @Query("UPDATE Benachrichtigung b SET b.gelesen = true WHERE b.empfaenger.id = :empfaengerId AND b.gelesen = false")
    void markiereAlleAlsGelesen(@Param("empfaengerId") UUID empfaengerId);
}

