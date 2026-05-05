package ch.sponsorplatz.service;

import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Stub-Implementierung des Zefix-Service für Entwicklung.
 * Gibt immer {@link Optional#empty()} zurück — echte API-Anbindung folgt in Prod-Profil.
 */
@Service
public class ZefixServiceStub implements ZefixService {

    @Override
    public Optional<String> pruefeOrganisation(String name) {
        return Optional.empty();
    }
}

