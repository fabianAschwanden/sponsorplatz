# Azure Container Registry — Basic SKU reicht für 1 Repository + 10 GB.
# Admin-User bewusst aus (admin_enabled=false) — Auth läuft ausschliesslich
# über die UAMI mit AcrPull-Rolle (siehe identity.tf).
resource "azurerm_container_registry" "this" {
  # ACR-Namen dürfen nur alphanumerisch sein, keine Bindestriche.
  name                = replace("${var.resource_prefix}${var.env_name}acr", "-", "")
  resource_group_name = azurerm_resource_group.this.name
  location            = azurerm_resource_group.this.location
  sku                 = var.acr_sku
  admin_enabled       = false
  tags                = local.tags
}
