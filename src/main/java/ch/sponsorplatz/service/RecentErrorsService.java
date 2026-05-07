package ch.sponsorplatz.service;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring-Bean-Fassade über den statisch zugänglichen
 * {@link RecentErrorsAppender}-Buffer. Damit Controller und Tests gegen ein
 * injizierbares Interface arbeiten statt gegen statische State.
 */
@Service
public class RecentErrorsService {

    public List<RecentErrorsAppender.RecentError> letzteErrors(int limit) {
        var snap = RecentErrorsAppender.snapshot();
        return snap.size() <= limit ? snap : snap.subList(0, limit);
    }

    public int anzahl() {
        return RecentErrorsAppender.aktuelleAnzahl();
    }
}
