package ch.sponsorplatz.repository;

import ch.sponsorplatz.model.PlattformEinstellungen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PlattformEinstellungenRepository extends JpaRepository<PlattformEinstellungen, UUID> {
}
