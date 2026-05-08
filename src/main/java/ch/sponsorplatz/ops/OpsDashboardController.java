package ch.sponsorplatz.ops;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Ops-Dashboard für PLATFORM_ADMIN unter {@code /admin/system}:
 * Live-Werte zu JVM, DB, Object Storage und letzten Errors.
 *
 * <p>JSON-Endpoint {@code /admin/system/data} für AJAX-Polling.
 */
@Controller
@RequestMapping("/admin/system")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class OpsDashboardController {

    private static final int RECENT_ERRORS_LIMIT = 20;

    private final SystemSnapshotService snapshotService;

    public OpsDashboardController(SystemSnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @GetMapping
    public String anzeigen(Model model) {
        model.addAttribute("snapshot", snapshotService.snapshot(RECENT_ERRORS_LIMIT));
        return "admin/system";
    }

    @GetMapping("/data")
    @ResponseBody
    public SystemSnapshotView data() {
        return snapshotService.snapshot(RECENT_ERRORS_LIMIT);
    }
}
