variable "compartment_ocid" {
  type = string
}

variable "env_name" {
  type        = string
  description = "z.B. staging-free, production — wird Teil des Bucket-Namens"
}

variable "namespace" {
  type        = string
  description = "Object-Storage-Namespace des Tenants (`oci os ns get`)"
}

variable "tags" {
  type    = map(string)
  default = {}
}
