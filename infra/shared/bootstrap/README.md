# Bootstrap (einmalig, manuell)

Erzeugt die Voraussetzungen, damit die env-Module (`infra/envs/staging-free`, später `infra/envs/production`) ihren State im OCI Object Storage ablegen können, und legt die Service-Group für GitHub-Actions-Deployments an.

## Voraussetzungen

- OCI-Tenancy mit mindestens zwei Compartments: `shared` (für State-Buckets), `sponsorplatz-staging` (für die App)
- OCI-User mit API-Key, der temporär `Administrator`-Rechte hat — **nach Bootstrap wieder entziehen**
- Object-Storage-Namespace bekannt (`oci os ns get`)
- Terraform 1.6+

## Ablauf

```bash
cd infra/shared/bootstrap

cat > terraform.tfvars <<EOF
tenancy_ocid             = "ocid1.tenancy.oc1..xxx"
compartment_ocid         = "ocid1.compartment.oc1..xxx"  # shared-compartment
object_storage_namespace = "myocinamespace"
EOF

terraform init
terraform plan
terraform apply
```

Das `apply` legt an:

- State-Buckets `sponsorplatz-tfstate-staging-free` + `sponsorplatz-tfstate-production` (versioned, NoPublicAccess)
- IAM-Group `sponsorplatz-cicd-deployer` mit minimalen Policies (OCIR-Push, Object-Storage)
- Output `tfstate_backend_endpoint` — den in den env-Modulen als `AWS_S3_ENDPOINT` exportieren

## Anschliessend

1. Service-User `github-actions-deploy` in der OCI-Console anlegen, in Group `sponsorplatz-cicd-deployer` hängen, API-Key + Auth-Token erzeugen
2. Bootstrap-User auf reduzierte Rechte zurücksetzen
3. `terraform init` in `infra/envs/staging-free` ausführen — der S3-Backend findet die Buckets jetzt vor

## State

`backend "local"` — der lokale State darf **NICHT** committet werden (`.gitignore` greift). Bei Tenant-Migration oder Restore wird der Bootstrap erneut ausgeführt.
