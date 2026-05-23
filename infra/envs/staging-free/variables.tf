variable "tenancy_ocid" {
  type = string
}

variable "compartment_ocid" {
  type        = string
  description = "OCID des sponsorplatz-staging-Compartments"
}

variable "region" {
  type    = string
  default = "eu-zurich-1"
}

variable "object_storage_namespace" {
  type        = string
  description = "Object-Storage-Namespace (`oci os ns get`)"
}

variable "availability_domain" {
  type        = string
  description = "z.B. fXdz:EU-ZURICH-1-AD-1 (`oci iam availability-domain list`)"
}

variable "ssh_public_key" {
  type        = string
  description = "SSH Public Key, der auf der VM autorisiert wird (Pipeline + manueller Zugriff)"
}

variable "domain" {
  type        = string
  description = "Public Hostname für Caddy (Let's Encrypt). Beispiel: sponsorplatz.example.ch"
}

variable "acme_email" {
  type        = string
  description = "Kontakt-E-Mail für Let's Encrypt"
}

variable "image_url" {
  type        = string
  description = "GHCR-Image-Referenz; CD-Workflow setzt :staging-latest, Initial-Boot zieht den (Package muss public sein, sonst docker login auf der VM nötig)."
  default     = "ghcr.io/fabianaschwanden/sponsorplatz:staging-latest"
}


variable "db_password" {
  type        = string
  sensitive   = true
  description = "Passwort für PostgreSQL-User 'sponsorplatz'"
}

variable "admin_email" {
  type        = string
  description = "Initialer Plattform-Admin (ProdAdminSeedRunner)"
}

variable "admin_password" {
  type        = string
  sensitive   = true
  description = "Passwort des initialen Plattform-Admins (sollte nach erster Anmeldung geändert werden)"
}

# ── SMTP — wenn smtp_host leer bleibt, deaktiviert sich der Versand ─────────
variable "smtp_host" {
  type        = string
  description = "SMTP-Host (z.B. smtp-relay.brevo.com, smtp.mailgun.org)"
  default     = ""
}

variable "smtp_port" {
  type    = number
  default = 587
}

variable "smtp_user" {
  type      = string
  sensitive = true
  default   = ""
}

variable "smtp_password" {
  type      = string
  sensitive = true
  default   = ""
}

variable "mail_absender" {
  type        = string
  description = "From-Adresse für ausgehende Mails. Muss bei Brevo/Mailgun verifiziert sein."
  default     = "noreply@sponsorplatz.ch"
}

variable "mail_live" {
  type        = bool
  description = "MAIL_LIVE-Schalter. false = alle Mails an Test-Empfänger umleiten oder skippen. true = echte Empfänger."
  default     = false
}

variable "mail_test_empfaenger" {
  type        = string
  description = "Test-Empfänger für AUS-Modus-Routing. Empfehlung: dein eigenes Postfach."
  default     = ""
}

variable "basis_url" {
  type        = string
  description = "Externe Basis-URL für Mail-Links (Token-Bestätigung). Leer = wird auf https://<domain> gesetzt"
  default     = ""
}

# ── Storage-Provider — Default OCI Object Storage ───────────────────────────
variable "storage_provider" {
  type        = string
  description = "lokal | oci — bestimmt LokalerStorageService vs. OciStorageService"
  default     = "oci"
}
