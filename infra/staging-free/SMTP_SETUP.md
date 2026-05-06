# SMTP-Setup für Sponsorplatz (staging-free)

Solange `SMTP_HOST` leer bleibt, sendet die App keine Mails — Verifizierung +
Einladungen schlagen still fehl. Dieser Guide aktiviert den Mail-Versand mit
Brevo (300 Mails/Tag dauerhaft gratis).

## Schritt 1 — Brevo-Account anlegen

1. https://www.brevo.com/ → Sign up
2. E-Mail-Verifizierung
3. Persönliche Daten + Phone (für Anti-Spam)
4. Im Dashboard: **SMTP & API** (links unten unter „Senders, Domains & Dedicated IPs" → „SMTP & API")

## Schritt 2 — Sender verifizieren

Bevor du senden kannst, musst du **mindestens einen Sender** verifizieren — am
einfachsten per E-Mail (kein DNS-Setup):

1. **Senders, Domains & Dedicated IPs** → **Senders** → **Add a sender**
2. From-Name: `Sponsorplatz`
3. From-E-Mail: deine eigene Adresse (z.B. `fabian.aschwanden@gmail.com`)
4. Bestätigungs-Mail erhalten + Klick auf den Link

Optional besser: Domain `for-better.biz` verifizieren (DNS-TXT + DKIM/SPF) —
dann kannst du als `noreply@for-better.biz` versenden.

## Schritt 3 — SMTP-Credentials holen

1. **SMTP & API** → **SMTP** Tab
2. Werte ablesen:

| Feld | Wert |
|---|---|
| **SMTP-Server** | `smtp-relay.brevo.com` |
| **Port** | `587` (STARTTLS) |
| **Login** | deine Brevo-Account-E-Mail |
| **Master-Password** | Klick auf **Generate a new SMTP key** → kopieren |

## Schritt 4 — auf der VM eintragen

```bash
ssh -i ~/.ssh/sponsoren_staging_free_deploy opc@144.24.246.244

# docker-compose.yml editieren
sudo nano /opt/sponsorplatz/docker-compose.yml
# In der app-section folgende ENV-Vars setzen:
#   SMTP_HOST: smtp-relay.brevo.com
#   SMTP_PORT: "587"
#   SMTP_USER: <deine-brevo-email>
#   SMTP_PASSWORD: <generated-smtp-key>
#   MAIL_ABSENDER: <verifizierte-from-adresse>   # gleiche wie SMTP_USER oder dedizierte
#                                                 # noreply@<deine-domain> nach Domain-Verify

# App neu starten
cd /opt/sponsorplatz && sudo docker compose up -d app
sudo docker logs sponsorplatz-app --tail 20
```

**Wichtig:** Die From-Adresse kommt aus `MAIL_ABSENDER` (Property
`sponsorplatz.mail.absender`, Default `noreply@sponsorplatz.ch`). Brevo lehnt
mit `550 Sender not verified` ab, falls die Adresse nicht in Schritt 2
verifiziert wurde — also `MAIL_ABSENDER` auf die verifizierte Adresse setzen.

## Schritt 5 — Verifizierungs-Flow testen

1. https://sponsorplatz.for-better.biz/registrieren
2. E-Mail eintragen + Passwort
3. Brevo-Inbox: Bestätigungs-Mail mit Token sollte ankommen (innerhalb 1 min)
4. Brevo-Dashboard → **Statistics** → die Mail erscheint als „Delivered"

## Auch in Terraform pflegen

Wenn die VM mal komplett neu provisioniert wird, müssen die Werte auch in
`infra/envs/staging-free/terraform.tfvars` gesetzt sein, damit cloud-init sie
ins compose schreibt:

```hcl
smtp_host     = "smtp-relay.brevo.com"
smtp_port     = 587
smtp_user     = "<deine-brevo-email>"
smtp_password = "<generated-smtp-key>"
```

## Troubleshooting

- **Mail kommt nicht an, App-Log zeigt `MailException: 535 Authentication required`** → SMTP-Key falsch / abgelaufen → in Brevo neu generieren.
- **App-Log zeigt `530 5.7.0 Authentication required`** → STARTTLS nicht aktiv → prüfen ob `SMTP_AUTH=true` und `SMTP_STARTTLS=true` in compose stehen (sind als ENV-Default gesetzt).
- **Brevo zeigt `Sender not verified`** → Sender aus Schritt 2 nicht bestätigt; oder andere From-Adresse benutzt → `SMTP_USER` muss verifizierte Mail sein.
- **Limit erreicht** (300/Tag) → Brevo Free-Plan; bei Bedarf auf Lite (~CHF 25/Mt für 20'000) upgraden.
