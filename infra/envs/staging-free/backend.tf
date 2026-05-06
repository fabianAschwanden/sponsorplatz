terraform {
  required_version = ">= 1.6"
  required_providers {
    oci = {
      source  = "oracle/oci"
      version = "~> 6.0"
    }
    local = {
      source  = "hashicorp/local"
      version = "~> 2.5"
    }
  }

  # Lokaler State für ersten Apply — vermeidet S3-Backend-Setup
  # (Customer-Secret-Key + Endpoint). Migration auf S3 (OCI Object Storage)
  # via `terraform init -migrate-state`, sobald Bootstrap gelaufen ist.
  backend "local" {}
}
