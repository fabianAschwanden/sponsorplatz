terraform {
  required_version = ">= 1.6"
  required_providers {
    oci = {
      source  = "oracle/oci"
      version = "~> 6.0"
    }
  }

  # Bootstrap läuft mit LOKALEM State.
  # Erst NACH diesem `apply` existieren die State-Buckets, gegen die die
  # env-Module ihren remote-State legen können.
  backend "local" {}
}

variable "tenancy_ocid" {
  type = string
}

variable "compartment_ocid" {
  type        = string
  description = "Compartment, in dem State-Bucket + CICD-Identity leben (z.B. shared-compartment)"
}

variable "region" {
  type    = string
  default = "eu-zurich-1"
}

variable "object_storage_namespace" {
  type        = string
  description = "Object-Storage-Namespace des Tenants (`oci os ns get`)"
}

variable "github_repo" {
  type    = string
  default = "fabianAschwanden/sponsorplatz"
}

provider "oci" {
  region       = var.region
  tenancy_ocid = var.tenancy_ocid
}

# ── Terraform-State-Bucket (versioniert, nicht öffentlich) ───────────────────
resource "oci_objectstorage_bucket" "tfstate" {
  for_each       = toset(["staging-free", "production"])
  compartment_id = var.compartment_ocid
  namespace      = var.object_storage_namespace
  name           = "sponsorplatz-tfstate-${each.value}"
  access_type    = "NoPublicAccess"
  versioning     = "Enabled"
  storage_tier   = "Standard"

  freeform_tags = {
    project    = "sponsorplatz"
    purpose    = "terraform-state"
    managed_by = "terraform"
  }
}

# ── CICD-Service-Group (für GitHub-Actions-Deployments) ──────────────────────
# Der Service-User wird nach diesem Apply manuell in der Console angelegt
# und in diese Group gehängt (Identity Domain → Users → "github-actions-deploy"
# + API-Key). Native GitHub-OIDC-Federation ist aufwändig (OAuth Client App in
# der Identity Domain mit JWT-Bearer-Trust) — Phase-3-Scope: API-Key.
resource "oci_identity_group" "cicd" {
  compartment_id = var.tenancy_ocid
  name           = "sponsorplatz-cicd-deployer"
  description    = "GitHub-Actions-Service-User für Deploy/Push/Terraform-Apply (${var.github_repo})"
}

resource "oci_identity_policy" "cicd_policy" {
  compartment_id = var.tenancy_ocid
  name           = "sponsorplatz-cicd-policy"
  description    = "Erlaubt CICD-Group: OCIR-Push + Object-Storage-Verwaltung + Terraform-Apply"
  statements = [
    "Allow group ${oci_identity_group.cicd.name} to manage repos in tenancy",
    "Allow group ${oci_identity_group.cicd.name} to manage object-family in compartment id ${var.compartment_ocid}",
    "Allow group ${oci_identity_group.cicd.name} to read secret-family in compartment id ${var.compartment_ocid}",
  ]
}

output "tfstate_bucket_names" {
  value = { for k, b in oci_objectstorage_bucket.tfstate : k => b.name }
}

output "tfstate_backend_endpoint" {
  value       = "https://${var.object_storage_namespace}.compat.objectstorage.${var.region}.oraclecloud.com"
  description = "S3-kompatibler Endpoint — in env-Modulen als AWS_S3_ENDPOINT setzen"
}

output "cicd_group_id" {
  value       = oci_identity_group.cicd.id
  description = "OCID der CICD-Group — Service-User (manuell in Console) hier hinzufügen"
}

output "cicd_group_name" {
  value = oci_identity_group.cicd.name
}
