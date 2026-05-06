output "bucket_names" {
  value       = { for k, b in oci_objectstorage_bucket.this : k => b.name }
  description = "Map: uploads|backups → konkreter Bucket-Name"
}

output "namespace" {
  value = var.namespace
}
