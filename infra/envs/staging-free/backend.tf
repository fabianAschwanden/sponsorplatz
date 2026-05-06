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

  # S3-kompatibler Backend gegen OCI Object Storage.
  # Bucket-Name + Endpoint kommen aus dem Bootstrap-Modul.
  # AWS-Creds (Customer-Secret-Key + Endpoint) via ENV setzen — siehe README.
  backend "s3" {
    bucket                      = "sponsorplatz-tfstate-staging-free"
    key                         = "staging-free/terraform.tfstate"
    region                      = "eu-zurich-1"
    skip_credentials_validation = true
    skip_metadata_api_check     = true
    skip_region_validation      = true
    skip_requesting_account_id  = true
    use_path_style              = true
  }
}
