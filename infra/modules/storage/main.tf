terraform {
  required_providers {
    oci = {
      source  = "oracle/oci"
      version = "~> 6.0"
    }
  }
}

# Bucket-Definition + optionale Lifecycle-Regeln.
#
# - uploads:  Medien-Assets (Logos, Cover). Behalten ohne Ablauf — Editor-
#             Verantwortung. Versioning schützt vor versehentlichem Überschreiben.
# - backups:  pg_dump-Dumps von BackupService. Nach 90 Tagen archivieren,
#             nach 365 Tagen löschen — passt zum 30-Tage-Aufbewahrungs-Default
#             im BackupService (lokal) und gibt zusätzlichen Cloud-Buffer.
locals {
  buckets = {
    uploads = { lifecycle_days_archive = null, lifecycle_days_delete = null }
    backups = { lifecycle_days_archive = 90, lifecycle_days_delete = 365 }
  }
}

resource "oci_objectstorage_bucket" "this" {
  for_each       = local.buckets
  compartment_id = var.compartment_ocid
  namespace      = var.namespace
  name           = "sponsorplatz-${each.key}-${var.env_name}"

  access_type  = "NoPublicAccess"
  storage_tier = "Standard"
  versioning   = "Enabled"

  freeform_tags = var.tags
}

resource "oci_objectstorage_object_lifecycle_policy" "this" {
  for_each  = { for k, v in local.buckets : k => v if v.lifecycle_days_delete != null }
  namespace = var.namespace
  bucket    = oci_objectstorage_bucket.this[each.key].name

  dynamic "rules" {
    for_each = each.value.lifecycle_days_archive == null ? [] : [1]
    content {
      name        = "archive-${each.key}"
      action      = "ARCHIVE"
      time_amount = each.value.lifecycle_days_archive
      time_unit   = "DAYS"
      is_enabled  = true
      target      = "objects"
    }
  }

  rules {
    name        = "delete-${each.key}"
    action      = "DELETE"
    time_amount = each.value.lifecycle_days_delete
    time_unit   = "DAYS"
    is_enabled  = true
    target      = "objects"
  }
}
