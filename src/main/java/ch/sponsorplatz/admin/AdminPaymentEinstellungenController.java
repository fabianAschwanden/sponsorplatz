package ch.sponsorplatz.admin;

import ch.sponsorplatz.shared.einstellungen.PaymentModus;
import ch.sponsorplatz.shared.einstellungen.PlattformEinstellungenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Admin-Seite zur Steuerung des Payment-Modus (QR-Rechnung vs. Datatrans).
 */
@Controller
@RequestMapping("/admin/payment-einstellungen")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdminPaymentEinstellungenController {

    private static final Logger log = LoggerFactory.getLogger(AdminPaymentEinstellungenController.class);
    private final PlattformEinstellungenService einstellungenService;

    public AdminPaymentEinstellungenController(PlattformEinstellungenService einstellungenService) {
        this.einstellungenService = einstellungenService;
    }

    @GetMapping
    public String anzeigen(Model model) {
        model.addAttribute("aktiverModus", einstellungenService.ladePaymentModus());
        model.addAttribute("modi", PaymentModus.values());
        return "admin/payment-einstellungen";
    }

    @PostMapping
    public String speichern(@RequestParam("paymentModus") String modus,
                            Authentication auth,
                            RedirectAttributes redirect) {
        PaymentModus neuerModus;
        try {
            neuerModus = PaymentModus.valueOf(modus);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("fehlermeldung", "Ungültiger Payment-Modus: " + modus);
            return "redirect:/admin/payment-einstellungen";
        }

        einstellungenService.setzePaymentModus(neuerModus, auth.getName());

        log.info("Payment-Modus geändert auf {} (durch {})", neuerModus, auth.getName());
        redirect.addFlashAttribute("erfolgsMeldung",
                "Payment-Modus auf «" + anzeigeName(neuerModus) + "» umgestellt.");
        return "redirect:/admin/payment-einstellungen";
    }

    private String anzeigeName(PaymentModus modus) {
        return switch (modus) {
            case QR_RECHNUNG -> "QR-Rechnung per E-Mail";
            case DATATRANS -> "Online-Zahlung (Datatrans)";
        };
    }
}

