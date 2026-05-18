# Azure-Storage-Account-Namen müssen global eindeutig sein und nur lowercase
# alphanumerisch — wir hängen ein zufälliges 4-Zeichen-Suffix an.
resource "random_string" "storage_suffix" {
  length  = 4
  upper   = false
  special = false
  numeric = true
}

resource "azurerm_storage_account" "this" {
  # max 24 Zeichen, nur lowercase alphanumerisch.
  # Basis (prefix+env, ohne '-') auf 20 Zeichen kürzen, dann 4-Zeichen-
  # Random-Suffix anhängen. Beispiel: 'sponsorplatzazurestauv7w' (24 chars).
  name                = lower("${substr(replace("${var.resource_prefix}${var.env_name}", "-", ""), 0, 20)}${random_string.storage_suffix.result}")
  resource_group_name = azurerm_resource_group.this.name
  location            = azurerm_resource_group.this.location

  account_tier             = "Standard"
  account_replication_type = "LRS" # Phase-1 — lokal-redundant. Für DR später ZRS/GZRS.
  account_kind             = "StorageV2"

  # Sicherheits-Hardening
  min_tls_version                 = "TLS1_2"
  allow_nested_items_to_be_public = false
  public_network_access_enabled   = true # MVP — später nur Private-Endpoint
  shared_access_key_enabled       = true # für Connection-String-Fallback (lokales Testen)

  blob_properties {
    versioning_enabled = true
    delete_retention_policy {
      days = 30
    }
    container_delete_retention_policy {
      days = 30
    }
  }

  tags = local.tags
}

# Workaround für ARM-Consistency-Lag: Storage-Account-Create gibt 200 zurück
# bevor das Resource regional propagiert ist. Container-Create + Management-
# Policy bekommen sonst 404 "ParentResourceNotFound". 30s reicht erfahrungsgemäß.
resource "time_sleep" "storage_account_settle" {
  depends_on      = [azurerm_storage_account.this]
  create_duration = "30s"
}

# Container: uploads (Medien) — keine Lifecycle-Regel, Editor-verwaltet
resource "azurerm_storage_container" "uploads" {
  name                  = "sponsorplatz-uploads"
  storage_account_id    = azurerm_storage_account.this.id
  container_access_type = "private"

  depends_on = [time_sleep.storage_account_settle]
}

# Container: backups (DB-Dumps) — Lifecycle: Cool nach 30, Archive nach 90, Löschen nach 365
resource "azurerm_storage_container" "backups" {
  name                  = "sponsorplatz-backups"
  storage_account_id    = azurerm_storage_account.this.id
  container_access_type = "private"

  depends_on = [time_sleep.storage_account_settle]
}

# Lifecycle-Policy auf Account-Ebene (filtert per Container-Präfix)
resource "azurerm_storage_management_policy" "this" {
  storage_account_id = azurerm_storage_account.this.id

  depends_on = [time_sleep.storage_account_settle]

  rule {
    name    = "backups-tiering"
    enabled = true
    filters {
      prefix_match = ["${azurerm_storage_container.backups.name}/"]
      blob_types   = ["blockBlob"]
    }
    actions {
      base_blob {
        tier_to_cool_after_days_since_modification_greater_than    = 30
        tier_to_archive_after_days_since_modification_greater_than = 90
        delete_after_days_since_modification_greater_than          = 365
      }
    }
  }
}
