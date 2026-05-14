package ch.sponsorplatz.benutzer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByVerifikationsToken(String verifikationsToken);

    Optional<AppUser> findByResetToken(String resetToken);

    boolean existsByEmail(String email);

    long countByAktivTrue();

    List<AppUser> findAllByOrderByRegistriertAmDesc();

    /**
     * Alle User mit einer bestimmten Plattform-Rolle — wird genutzt, um neue
     * Org-Registrierungen an alle PLATFORM_ADMINs zu pushen (In-App-Glocke
     * und E-Mail).
     */
    List<AppUser> findByPlatformRolle(PlatformRolle platformRolle);
}
