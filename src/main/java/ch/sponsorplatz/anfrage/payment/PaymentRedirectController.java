package ch.sponsorplatz.anfrage.payment;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Öffentliche Redirect-Ziele nach Datatrans-Zahlung.
 * Diese Seiten sind permitAll — der Sponsor landet hier nach der HPP.
 */
@Controller
@RequestMapping("/payment")
public class PaymentRedirectController {

    @GetMapping("/erfolg")
    public String erfolg(@RequestParam(required = false) String ref, Model model) {
        model.addAttribute("rechnungId", ref);
        return "anfrage/payment-erfolg";
    }

    @GetMapping("/abgebrochen")
    public String abgebrochen(@RequestParam(required = false) String ref, Model model) {
        model.addAttribute("rechnungId", ref);
        return "anfrage/payment-abgebrochen";
    }

    @GetMapping("/fehler")
    public String fehler(@RequestParam(required = false) String ref, Model model) {
        model.addAttribute("rechnungId", ref);
        return "anfrage/payment-fehler";
    }
}

