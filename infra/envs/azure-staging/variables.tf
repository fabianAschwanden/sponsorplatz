# ── Azure-Auth ──────────────────────────────────────────────────────────────
variable "subscription_id" {
  type        = string
  description = "Azure-Subscription-ID, in der dieses Stack deployed wird"
}

variable "tenant_id" {
  type        = string
  description = "Azure-AD-Tenant-ID (für Role-Assignments + Managed Identity)"
}

variable "location" {
  type        = string
  default     = "switzerlandnorth"
  description = "Azure-Region. Default Zürich für CH-Datenresidenz. Fallback bei Capacity-Engpässen: switzerlandwest oder westeurope."
}

variable "zone" {
  type        = string
  default     = "1"
  description = "Availability Zone (1|2|3) — für VM + Postgres gemeinsam, damit DB-Zugriff intra-AZ bleibt. Bei Capacity-Engpässen Zone wechseln."
}

# ── Naming + Tags ───────────────────────────────────────────────────────────
variable "resource_prefix" {
  type        = string
  default     = "sponsorplatz"
  description = "Prefix für alle Azure-Resource-Namen"
}

variable "env_name" {
  type        = string
  default     = "azure-staging"
  description = "Environment-Name — fliesst in Resource-Namen und Tags ein"
}

# ── VM + SSH ────────────────────────────────────────────────────────────────
variable "vm_size" {
  type        = string
  default     = "Standard_B2s"
  description = "VM-Grösse. B2s = 2 vCPU / 4 GB RAM, ~CHF 30/Monat in switzerlandnorth"
}

variable "ssh_public_key" {
  type        = string
  description = "SSH Public Key für den Login-User 'sponsoradmin' (Pipeline + manueller Zugriff)"
}

# ── Domain + TLS ────────────────────────────────────────────────────────────
variable "domain" {
  type        = string
  description = "Public Hostname für Caddy (Let's Encrypt). Beispiel: azure-staging.sponsorplatz.ch"
}

variable "acme_email" {
  type        = string
  description = "Kontakt-E-Mail für Let's Encrypt"
}

variable "basis_url" {
  type        = string
  description = "Externe Basis-URL für Mail-Links (Token-Bestätigung). Leer = wird auf https://<domain> gesetzt"
  default     = ""
}

# ── App-Image (ACR) ─────────────────────────────────────────────────────────
variable "acr_sku" {
  type        = string
  default     = "Basic"
  description = "Azure-Container-Registry-Tier. Basic reicht für 1 Repo + 10 GB."
}

variable "image_url" {
  type        = string
  description = "Override für die Image-Referenz. Leer = automatisch aus dem in diesem Modul erstellten ACR + default-Tag ':azure-staging-latest' zusammensetzen."
  default     = ""
}

# ── PostgreSQL ──────────────────────────────────────────────────────────────
variable "postgres_sku" {
  type        = string
  default     = "B_Standard_B1ms"
  description = "Flexible-Server-SKU. B1ms = 1 vCPU / 2 GB Burstable, ~CHF 25/Monat."
}

variable "postgres_storage_mb" {
  type        = number
  default     = 32768
  description = "Storage in MB für die Flex-DB (Default 32 GB — Minimum)"
}

variable "postgres_version" {
  type    = string
  default = "16"
}

variable "db_admin_user" {
  type    = string
  default = "sponsorplatz"
}

variable "db_password" {
  type        = string
  sensitive   = true
  description = "Passwort für PostgreSQL-Admin (Flexible-Server-Admin-Login)"
}

# ── App-Admin + SMTP (analog OCI) ───────────────────────────────────────────
variable "admin_email" {
  type        = string
  description = "Initialer Plattform-Admin (ProdAdminSeedRunner)"
}

variable "admin_password" {
  type        = string
  sensitive   = true
  description = "Passwort des initialen Plattform-Admins (sollte nach erster Anmeldung geändert werden)"
}

variable "smtp_host" {
  type        = string
  default     = ""
  description = "SMTP-Host (z.B. smtp-relay.brevo.com). Leer = Mail-Versand deaktiviert."
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
  type    = string
  default = "noreply@sponsorplatz.ch"
}

variable "mail_live" {
  type    = bool
  default = false
}

variable "mail_test_empfaenger" {
  type    = string
  default = ""
}

# ── Storage-Provider ────────────────────────────────────────────────────────
variable "storage_provider" {
  type        = string
  default     = "azure"
  description = "lokal | oci | azure — bestimmt StorageService-Impl in der App"
  validation {
    condition     = contains(["lokal", "oci", "azure"], var.storage_provider)
    error_message = "storage_provider muss 'lokal', 'oci' oder 'azure' sein."
  }
}

variable "tags" {
  type = map(string)
  default = {
    project    = "sponsorplatz"
    managed_by = "terraform"
  }
}
