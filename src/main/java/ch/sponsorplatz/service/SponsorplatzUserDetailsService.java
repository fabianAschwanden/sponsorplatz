package ch.sponsorplatz.service;

import ch.sponsorplatz.model.AppUser;
import ch.sponsorplatz.repository.AppUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Lädt AppUser aus der Datenbank für Spring Security Form-Login.
 */
@Service
public class SponsorplatzUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public SponsorplatzUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser appUser = appUserRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new UsernameNotFoundException("Benutzer nicht gefunden: " + email));

        if (!appUser.isAktiv()) {
            throw new UsernameNotFoundException("Benutzer ist gesperrt: " + email);
        }

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (appUser.getPlatformRolle() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + appUser.getPlatformRolle().name()));
        }

        return User.builder()
                .username(appUser.getEmail())
                .password(appUser.getPasswortHash())
                .disabled(!appUser.isEmailVerifiziert())
                .authorities(authorities)
                .build();
    }
}

