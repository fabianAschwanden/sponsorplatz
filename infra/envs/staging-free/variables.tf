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
  description = "OCIR-Image-Referenz; CD-Workflow setzt staging-latest, Initial-Boot zieht den"
  default     = "zrh.ocir.io/REPLACE_NS/sponsorplatz:staging-latest"
}

variable "ocir_username" {
  type        = string
  description = "OCI-Login für `docker pull` von OCIR (z.B. default/github_actions_deploy@example.ch)"
}

variable "ocir_auth_token" {
  type        = string
  sensitive   = true
  description = "OCIR Auth Token für `docker pull`"
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
