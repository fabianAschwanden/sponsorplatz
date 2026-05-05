package ch.sponsorplatz.repository;

import ch.sponsorplatz.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByVerifikationsToken(String verifikationsToken);

    boolean existsByEmail(String email);

    long countByAktivTrue();

    List<AppUser> findAllByOrderByRegistriertAmDesc();
}
