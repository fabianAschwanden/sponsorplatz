output "vm_id" {
  value = oci_core_instance.app.id
}

output "vm_public_ip" {
  value       = oci_core_instance.app.public_ip
  description = "Public IP der App-VM"
}

output "ssh_command" {
  value = "ssh opc@${oci_core_instance.app.public_ip}"
}

output "app_url" {
  value = "https://${var.domain}"
}

output "app_url_fallback" {
  # nip.io-Wildcard-DNS — funktioniert auch ohne A-Record auf var.domain
  value = "https://${replace(oci_core_instance.app.public_ip, ".", "-")}.nip.io"
}

output "bucket_names" {
  value = module.storage.bucket_names
}

# ── Werte für Pipeline (GitHub Variables) ───────────────────────────────────

output "vm_public_ip_for_github" {
  value       = oci_core_instance.app.public_ip
  description = "→ GitHub Repository Variable: STAGING_FREE_VM_IP"
}

output "namespace_for_github" {
  value       = var.object_storage_namespace
  description = "→ GitHub Repository Variable: OCIR_NAMESPACE"
}

output "rendered_cloud_init_path" {
  value       = abspath(local_file.cloud_init_rendered.filename)
  description = "Pfad zur gerenderten cloud-init.yaml (Debug)"
}
