# Public IP für die App-VM (Statisch, damit DNS + Cloudflare-Failover stabil bleibt)
resource "azurerm_public_ip" "app" {
  name                = "${local.name_base}-pip-app"
  location            = azurerm_resource_group.this.location
  resource_group_name = azurerm_resource_group.this.name
  allocation_method   = "Static"
  sku                 = "Standard"
  zones               = [var.zone]
  tags                = local.tags
}

resource "azurerm_network_interface" "app" {
  name                = "${local.name_base}-nic-app"
  location            = azurerm_resource_group.this.location
  resource_group_name = azurerm_resource_group.this.name
  tags                = local.tags

  ip_configuration {
    name                          = "internal"
    subnet_id                     = azurerm_subnet.app.id
    private_ip_address_allocation = "Dynamic"
    public_ip_address_id          = azurerm_public_ip.app.id
  }
}

# Linux-VM mit Ubuntu 24.04 LTS + cloud-init Bootstrap.
# Identity = UserAssigned + UAMI-ID — die App nutzt diese MSI zur Authentifizierung
# gegen Blob-Storage und ACR (`az login --identity --client-id <UAMI>`).
resource "azurerm_linux_virtual_machine" "app" {
  name                  = "${local.name_base}-vm-app"
  resource_group_name   = azurerm_resource_group.this.name
  location              = azurerm_resource_group.this.location
  size                  = var.vm_size
  admin_username        = "sponsoradmin"
  network_interface_ids = [azurerm_network_interface.app.id]
  zone                  = var.zone
  tags                  = local.tags

  admin_ssh_key {
    username   = "sponsoradmin"
    public_key = var.ssh_public_key
  }

  identity {
    type         = "UserAssigned"
    identity_ids = [azurerm_user_assigned_identity.app.id]
  }

  os_disk {
    caching              = "ReadWrite"
    storage_account_type = "Standard_LRS"
    disk_size_gb         = 32
  }

  source_image_reference {
    publisher = "Canonical"
    offer     = "ubuntu-24_04-lts"
    sku       = "server"
    version   = "latest"
  }

  custom_data = base64encode(local.cloud_init_content)

  # Image-Updates sollen die VM nicht neubauen — Updates kommen via CD-Pipeline
  # (`docker compose pull && up -d`).
  lifecycle {
    ignore_changes = [
      source_image_reference[0].version,
      custom_data
    ]
  }
}
