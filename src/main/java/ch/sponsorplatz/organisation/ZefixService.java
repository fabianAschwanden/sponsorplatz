package ch.sponsorplatz.organisation;

import java.util.Optional;

/**
 * Schnittstelle zum Zefix-Handelsregister für Auto-Verifizierung von Organisationen.
 * Gibt die UID zurück, falls der Name im Register gefunden wird.
 */
public interface ZefixService {

    /**
     * Prüft, ob eine Organisation mit dem gegebenen Namen im Handelsregister existiert.
     *
     * @return UID der Organisation, falls gefunden
     */
    Optional<String> pruefeOrganisation(String name);
}

