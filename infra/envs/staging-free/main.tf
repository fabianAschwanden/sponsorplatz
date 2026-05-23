provider "oci" {
  region              = var.region
  tenancy_ocid        = var.tenancy_ocid
  auth                = "ApiKey"
  config_file_profile = "DEFAULT"
}

locals {
  env_name = "staging-free"
  tags = {
    project     = "sponsorplatz"
    environment = local.env_name
    managed_by  = "terraform"
  }
  # OCIR-Endpoint-Map entfernt (2026-05-22): wir pushen jetzt nach GHCR
  # (OCI Always-Free unterstützt OCIR-Push nicht mehr — 'Free tier account
  # is not supported'). Siehe cd-staging-free.yml.

  basis_url_effective = var.basis_url != "" ? var.basis_url : "https://${var.domain}"
}

# ── Image (Oracle Linux 9) ──────────────────────────────────────────────────
data "oci_core_images" "oracle_linux" {
  compartment_id           = var.compartment_ocid
  operating_system         = "Oracle Linux"
  operating_system_version = "9"
  shape                    = "VM.Standard.E5.Flex"
  state                    = "AVAILABLE"
  sort_by                  = "TIMECREATED"
  sort_order               = "DESC"
}

# ── Network: minimal — VCN + Public Subnet + Internet Gateway ────────────────
resource "oci_core_vcn" "main" {
  compartment_id = var.compartment_ocid
  cidr_blocks    = ["10.40.0.0/16"]
  display_name   = "sponsorplatz-vcn-${local.env_name}"
  dns_label      = "spfree"
  freeform_tags  = local.tags
}

resource "oci_core_internet_gateway" "main" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.main.id
  display_name   = "sponsorplatz-igw-${local.env_name}"
  enabled        = true
  freeform_tags  = local.tags
}

resource "oci_core_route_table" "public" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.main.id
  display_name   = "sponsorplatz-rt-public-${local.env_name}"
  route_rules {
    destination       = "0.0.0.0/0"
    destination_type  = "CIDR_BLOCK"
    network_entity_id = oci_core_internet_gateway.main.id
  }
  freeform_tags = local.tags
}

resource "oci_core_security_list" "public" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.main.id
  display_name   = "sponsorplatz-sl-${local.env_name}"

  egress_security_rules {
    destination = "0.0.0.0/0"
    protocol    = "all"
  }

  ingress_security_rules {
    description = "SSH"
    protocol    = "6"
    source      = "0.0.0.0/0"
    tcp_options {
      min = 22
      max = 22
    }
  }

  ingress_security_rules {
    description = "HTTP — Caddy für Let's-Encrypt-HTTP-01 + Redirect auf HTTPS"
    protocol    = "6"
    source      = "0.0.0.0/0"
    tcp_options {
      min = 80
      max = 80
    }
  }

  ingress_security_rules {
    description = "HTTPS — Caddy TLS-Termination"
    protocol    = "6"
    source      = "0.0.0.0/0"
    tcp_options {
      min = 443
      max = 443
    }
  }

  freeform_tags = local.tags
}

resource "oci_core_subnet" "public" {
  compartment_id             = var.compartment_ocid
  vcn_id                     = oci_core_vcn.main.id
  cidr_block                 = "10.40.1.0/24"
  display_name               = "sponsorplatz-subnet-${local.env_name}"
  prohibit_public_ip_on_vnic = false
  route_table_id             = oci_core_route_table.public.id
  security_list_ids          = [oci_core_security_list.public.id]
  freeform_tags              = local.tags
}

# ── Object Storage: Uploads + Backups ────────────────────────────────────────
module "storage" {
  source           = "../../modules/storage"
  compartment_ocid = var.compartment_ocid
  env_name         = local.env_name
  namespace        = var.object_storage_namespace
  tags             = local.tags
}

# ── VM (E5.Flex 1 OCPU / 4 GB — paid während Free Trial gratis, ~$30/Monat) ─
resource "oci_core_instance" "app" {
  compartment_id      = var.compartment_ocid
  availability_domain = var.availability_domain
  display_name        = "sponsorplatz-vm-${local.env_name}"
  shape               = "VM.Standard.E5.Flex"

  shape_config {
    ocpus         = 1
    memory_in_gbs = 4
  }

  source_details {
    source_type = "image"
    source_id   = data.oci_core_images.oracle_linux.images[0].id
  }

  create_vnic_details {
    subnet_id        = oci_core_subnet.public.id
    assign_public_ip = true
  }

  metadata = {
    ssh_authorized_keys = var.ssh_public_key
    user_data           = base64encode(local.cloud_init_content)
  }

  freeform_tags = local.tags

  # Image-Updates sollen die VM nicht neubauen — Updates kommen via CD-Pipeline
  # (`docker compose pull && up -d`).
  lifecycle {
    ignore_changes = [source_details[0].source_id, metadata["user_data"]]
  }
}

locals {
  cloud_init_content = templatefile("${path.module}/cloud-init.yaml.tftpl", {
    image_url         = var.image_url
    object_storage_ns = var.object_storage_namespace
    region            = var.region

    domain     = var.domain
    acme_email = var.acme_email
    basis_url  = local.basis_url_effective

    db_password    = var.db_password
    admin_email    = var.admin_email
    admin_password = var.admin_password

    smtp_host            = var.smtp_host
    smtp_port            = var.smtp_port
    smtp_user            = var.smtp_user
    smtp_password        = var.smtp_password
    mail_absender        = var.mail_absender
    mail_live            = var.mail_live
    mail_test_empfaenger = var.mail_test_empfaenger

    storage_provider = var.storage_provider
    bucket_uploads   = module.storage.bucket_names["uploads"]
    bucket_backups   = module.storage.bucket_names["backups"]
  })
}

# Gerenderte cloud-init zur Disk schreiben — Debug only (Klartext-PWs!).
resource "local_file" "cloud_init_rendered" {
  filename        = "${path.module}/cloud-init.rendered.yaml"
  file_permission = "0600"
  content         = local.cloud_init_content
}

# ── Dynamic Group + Policy: Object-Storage-Zugriff via Instance Principal ───
# Match-Regel auf das Compartment — die VM bekommt automatisch Lese-/
# Schreibrechte auf die Sponsorplatz-Buckets ohne API-Key.
resource "oci_identity_dynamic_group" "vm" {
  compartment_id = var.tenancy_ocid
  name           = "sponsorplatz-vm-${local.env_name}"
  description    = "Dynamic Group für VMs im sponsorplatz-staging-Compartment"
  matching_rule  = "ALL {instance.compartment.id = '${var.compartment_ocid}'}"
  freeform_tags  = local.tags
}

resource "oci_identity_policy" "vm_storage_access" {
  compartment_id = var.tenancy_ocid
  name           = "sponsorplatz-vm-storage-${local.env_name}"
  description    = "Erlaubt der App-VM, in die Sponsorplatz-Buckets zu lesen/schreiben"
  statements = [
    "Allow dynamic-group ${oci_identity_dynamic_group.vm.name} to manage object-family in compartment id ${var.compartment_ocid}"
  ]
  freeform_tags = local.tags
}

# ── Object-Storage-Service-Principal für Lifecycle-Policies ─────────────────
resource "oci_identity_policy" "objectstorage_lifecycle" {
  compartment_id = var.tenancy_ocid
  name           = "sponsorplatz-os-lifecycle-${local.env_name}"
  description    = "Erlaubt Object-Storage-Service, Lifecycle-Policies in den Sponsorplatz-Buckets auszuführen"
  statements = [
    "Allow service objectstorage-${var.region} to manage object-family in compartment id ${var.compartment_ocid}"
  ]
  freeform_tags = local.tags
}
