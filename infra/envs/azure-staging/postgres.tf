# Azure Database for PostgreSQL Flexible Server.
# VNet-Integration über das delegierte db-Subnet — kein Public Endpoint.
# Backup-Retention: 7 Tage (Default-Minimum), redundancy=Local (kein Geo-Backup
# in der Staging-Phase — kostet extra).

resource "azurerm_postgresql_flexible_server" "this" {
  name                = "${local.name_base}-pg"
  resource_group_name = azurerm_resource_group.this.name
  location            = azurerm_resource_group.this.location
  version             = var.postgres_version
  sku_name            = var.postgres_sku

  administrator_login    = var.db_admin_user
  administrator_password = var.db_password

  storage_mb                   = var.postgres_storage_mb
  backup_retention_days        = 7
  geo_redundant_backup_enabled = false
  zone                         = var.zone

  # MUSS bei VNet-Integration explizit false sein — azurerm 4.x defaultet
  # jetzt auf true, was mit delegated_subnet_id kollidiert
  # (ConflictingPublicNetworkAccessAndVirtualNetworkConfiguration).
  public_network_access_enabled = false

  delegated_subnet_id = azurerm_subnet.db.id
  private_dns_zone_id = azurerm_private_dns_zone.postgres.id

  tags = local.tags

  # DNS-Zone-Link muss vor dem Server existieren, sonst FlexServer-Create
  # schlägt fehl.
  depends_on = [
    azurerm_private_dns_zone_virtual_network_link.postgres
  ]

  lifecycle {
    ignore_changes = [zone] # Azure kann die Zone autom. setzen
  }
}

# Die App-Datenbank selbst (FlexServer ist ein Cluster, die DB darin separat)
resource "azurerm_postgresql_flexible_server_database" "sponsorplatz" {
  name      = "sponsorplatz"
  server_id = azurerm_postgresql_flexible_server.this.id
  collation = "en_US.utf8"
  charset   = "UTF8"
}

# Postgres-Konfiguration: Logging an, max_connections moderat
resource "azurerm_postgresql_flexible_server_configuration" "log_statement" {
  name      = "log_statement"
  server_id = azurerm_postgresql_flexible_server.this.id
  value     = "ddl"
}
