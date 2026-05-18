output "vm_id" {
  value = azurerm_linux_virtual_machine.app.id
}

output "vm_public_ip" {
  value       = azurerm_public_ip.app.ip_address
  description = "Public IP der App-VM"
}

output "ssh_command" {
  value = "ssh sponsoradmin@${azurerm_public_ip.app.ip_address}"
}

output "app_url" {
  value = "https://${var.domain}"
}

output "acr_login_server" {
  value       = azurerm_container_registry.this.login_server
  description = "ACR-FQDN für `docker push` (z.B. sponsorplatzazurestagingacr.azurecr.io)"
}

output "postgres_fqdn" {
  value       = azurerm_postgresql_flexible_server.this.fqdn
  description = "Private DNS Name des Flex-Servers (nur aus dem VNet erreichbar)"
}

output "storage_account_name" {
  value = azurerm_storage_account.this.name
}

output "account_url" {
  value = "https://${azurerm_storage_account.this.name}.blob.core.windows.net"
}

output "container_uploads" {
  value = azurerm_storage_container.uploads.name
}

output "container_backups" {
  value = azurerm_storage_container.backups.name
}

output "uami_client_id" {
  value       = azurerm_user_assigned_identity.app.client_id
  description = "Client-ID der User-Assigned Managed Identity (für `az login --identity --client-id`)"
}

# ── Werte für Pipeline (GitHub Variables/Secrets) ───────────────────────────

output "vm_public_ip_for_github" {
  value       = azurerm_public_ip.app.ip_address
  description = "→ GitHub Repository Variable: AZURE_VM_IP"
}

output "acr_login_server_for_github" {
  value       = azurerm_container_registry.this.login_server
  description = "→ GitHub Repository Variable: ACR_LOGIN_SERVER"
}

output "rendered_cloud_init_path" {
  value       = abspath(local_file.cloud_init_rendered.filename)
  description = "Pfad zur gerenderten cloud-init.yaml (Debug)"
}
