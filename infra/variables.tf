variable "aws_region" {
  description = "Región de AWS donde se despliega la infraestructura"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Nombre base para etiquetar los recursos"
  type        = string
  default     = "franquicias-api"
}

variable "db_name" {
  description = "Nombre de la base de datos"
  type        = string
  default     = "franquicias"
}

variable "db_username" {
  description = "Usuario de la base de datos"
  type        = string
  default     = "franquicias"
}

variable "db_password" {
  description = "Password de la base de datos (sensible, pasar por tfvars o TF_VAR_db_password)"
  type        = string
  sensitive   = true
}

variable "container_image" {
  description = "URI completa de la imagen en ECR (repo:tag) a desplegar. Vacío en el primer apply antes de hacer push."
  type        = string
  default     = ""
}
