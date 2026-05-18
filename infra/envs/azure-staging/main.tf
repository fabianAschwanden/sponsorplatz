locals {
  # Naming-Konvention: <prefix>-<env>-<resource-kurz>
  # Storage-Account-Namen müssen lowercase + 3-24 chars sein, daher
  # eigenes Schema mit random_string-Suffix für Eindeutigkeit.
  name_base = "${var.resource_prefix}-${var.env_name}"

  tags = merge(var.tags, {
    environment = var.env_name
    location    = var.location
  })

  basis_url_effective = var.basis_url != "" ? var.basis_url : "https://${var.domain}"

  # Wenn image_url nicht überschrieben wurde, dynamisch aus dem ACR-FQDN
  # zusammensetzen — vermeidet Placeholder-Bugs (REPLACE_ACR...).
  image_url_effective = var.image_url != "" ? var.image_url : "${azurerm_container_registry.this.login_server}/sponsorplatz:azure-staging-latest"

  # Cloud-init wird unten gerendert.
  cloud_init_content = templatefile("${path.module}/cloud-init.yaml.tftpl", {
    acr_login_server = azurerm_container_registry.this.login_server
    image_url        = local.image_url_effective

    domain     = var.domain
    acme_email = var.acme_email
    basis_url  = local.basis_url_effective

    # DB extern (Flexible Server) — kein lokaler Postgres-Container.
    # WICHTIG: Flexible Server will den nackten Username, NICHT 'user@server'
    # (das war das alte Single-Server-Format). Sonst:
    # FATAL: password authentication failed for user "user@server"
    db_host     = azurerm_postgresql_flexible_server.this.fqdn
    db_name     = "sponsorplatz"
    db_user     = var.db_admin_user
    db_password = var.db_password

    admin_email    = var.admin_email
    admin_password = var.admin_password

    smtp_host            = var.smtp_host
    smtp_port            = var.smtp_port
    smtp_user            = var.smtp_user
    smtp_password        = var.smtp_password
    mail_absender        = var.mail_absender
    mail_live            = var.mail_live
    mail_test_empfaenger = var.mail_test_empfaenger

    storage_provider  = var.storage_provider
    storage_account   = azurerm_storage_account.this.name
    account_url       = "https://${azurerm_storage_account.this.name}.blob.core.windows.net"
    container_uploads = azurerm_storage_container.uploads.name
    container_backups = azurerm_storage_container.backups.name
    managed_identity  = azurerm_user_assigned_identity.app.client_id
  })
}

# Resource Group als Container für alles
resource "azurerm_resource_group" "this" {
  name     = "${local.name_base}-rg"
  location = var.location
  tags     = local.tags
}

# Render der cloud-init zur Disk — Debug only (Klartext-PWs!)
resource "local_file" "cloud_init_rendered" {
  filename        = "${path.module}/cloud-init.rendered.yaml"
  file_permission = "0600"
  content         = local.cloud_init_content
}
