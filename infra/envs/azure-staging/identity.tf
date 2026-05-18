# User-Assigned Managed Identity (UAMI) für die App-VM.
# Empfohlen über System-Assigned weil die ID den VM-Lifecycle überlebt
# (Rebuild der VM behält die Identity und damit alle Role-Assignments).
resource "azurerm_user_assigned_identity" "app" {
  name                = "${local.name_base}-uami-app"
  location            = azurerm_resource_group.this.location
  resource_group_name = azurerm_resource_group.this.name
  tags                = local.tags
}

# Rolle 'Storage Blob Data Contributor' auf den Storage-Account.
# Erlaubt der UAMI lesend/schreibend in beide Container (uploads + backups).
resource "azurerm_role_assignment" "uami_blob" {
  scope                = azurerm_storage_account.this.id
  role_definition_name = "Storage Blob Data Contributor"
  principal_id         = azurerm_user_assigned_identity.app.principal_id
}

# Rolle 'AcrPull' auf die Container Registry.
# Erlaubt der UAMI `docker pull` ohne Username/Password — `az acr login` via MSI.
resource "azurerm_role_assignment" "uami_acr" {
  scope                = azurerm_container_registry.this.id
  role_definition_name = "AcrPull"
  principal_id         = azurerm_user_assigned_identity.app.principal_id
}
