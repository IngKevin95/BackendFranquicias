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

variable "instance_type" {
  description = "Tipo de instancia EC2 (t3.micro para free tier en cuentas nuevas)"
  type        = string
  default     = "t3.micro"
}

variable "ssh_cidr" {
  description = "CIDR permitido para SSH (restringir a tu IP: x.x.x.x/32)"
  type        = string
  default     = "0.0.0.0/0"
}

variable "ssh_public_key" {
  description = "Clave publica SSH (contenido de tu .pub) para acceder a la instancia"
  type        = string
}

variable "repo_url" {
  description = "URL del repo git a clonar en la instancia"
  type        = string
  default     = "https://github.com/IngKevin95/BackendFranquicias.git"
}

variable "repo_branch" {
  description = "Branch a desplegar"
  type        = string
  default     = "develop"
}

variable "admin_username" {
  description = "Username del usuario ADMIN sembrado al primer arranque"
  type        = string
  default     = "admin"
}

variable "admin_password" {
  description = "Password del usuario ADMIN sembrado al primer arranque (sensible, pasar por tfvars o TF_VAR_admin_password)"
  type        = string
  sensitive   = true
}

variable "admin_email" {
  description = "Email del usuario ADMIN sembrado al primer arranque"
  type        = string
  default     = "admin@example.com"
}
