# Minimales Netzwerk: VNet + 2 Subnets (App + DB-Delegation) + NSG für die VM.
# Die DB läuft im delegierten Subnet (privates VNet-Integration), die VM hat
# eine öffentliche IP (Caddy auf 80/443 + SSH von Restricted-Source).

resource "azurerm_virtual_network" "main" {
  name                = "${local.name_base}-vnet"
  address_space       = ["10.50.0.0/16"]
  location            = azurerm_resource_group.this.location
  resource_group_name = azurerm_resource_group.this.name
  tags                = local.tags
}

# Workaround für ARM-Consistency-Lag in westeurope/switzerland*:
# VNet-Create gibt 200 zurück bevor das Resource regional propagiert ist.
# Subnets + DNS-Zone-Link bekommen sonst sporadisch 404. 60s reicht.
resource "time_sleep" "vnet_settle" {
  depends_on      = [azurerm_virtual_network.main]
  create_duration = "60s"
}

resource "azurerm_subnet" "app" {
  name                 = "${local.name_base}-subnet-app"
  resource_group_name  = azurerm_resource_group.this.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = ["10.50.1.0/24"]

  depends_on = [time_sleep.vnet_settle]
}

# Eigenes Subnet für Postgres Flexible Server mit Delegation.
resource "azurerm_subnet" "db" {
  name                 = "${local.name_base}-subnet-db"
  resource_group_name  = azurerm_resource_group.this.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = ["10.50.2.0/24"]
  service_endpoints    = ["Microsoft.Storage"]

  delegation {
    name = "fs"
    service_delegation {
      name = "Microsoft.DBforPostgreSQL/flexibleServers"
      actions = [
        "Microsoft.Network/virtualNetworks/subnets/join/action",
      ]
    }
  }

  depends_on = [time_sleep.vnet_settle]
}

# NSG für das App-Subnet — 22/80/443 inbound aus Internet.
# Production-Hinweis: SSH-Source-IP eingrenzen sobald die GitHub-Actions-Runner-
# Ranges bekannt sind (siehe https://api.github.com/meta).
resource "azurerm_network_security_group" "app" {
  name                = "${local.name_base}-nsg-app"
  location            = azurerm_resource_group.this.location
  resource_group_name = azurerm_resource_group.this.name
  tags                = local.tags

  security_rule {
    name                       = "allow-ssh"
    priority                   = 1000
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "22"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }

  security_rule {
    name                       = "allow-http"
    priority                   = 1010
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "80"
    source_address_prefix      = "Internet"
    destination_address_prefix = "*"
  }

  security_rule {
    name                       = "allow-https"
    priority                   = 1020
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "443"
    source_address_prefix      = "Internet"
    destination_address_prefix = "*"
  }
}

resource "azurerm_subnet_network_security_group_association" "app" {
  subnet_id                 = azurerm_subnet.app.id
  network_security_group_id = azurerm_network_security_group.app.id
}

# Private DNS Zone für Postgres Flex — nötig damit die VM den Server-FQDN
# auflösen kann.
resource "azurerm_private_dns_zone" "postgres" {
  name                = "${local.name_base}.postgres.database.azure.com"
  resource_group_name = azurerm_resource_group.this.name
  tags                = local.tags
}

resource "azurerm_private_dns_zone_virtual_network_link" "postgres" {
  name                  = "${local.name_base}-postgres-link"
  resource_group_name   = azurerm_resource_group.this.name
  private_dns_zone_name = azurerm_private_dns_zone.postgres.name
  virtual_network_id    = azurerm_virtual_network.main.id
  tags                  = local.tags

  depends_on = [time_sleep.vnet_settle]
}
