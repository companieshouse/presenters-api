# ------------------------------------------------------------------------------
# Environment
# ------------------------------------------------------------------------------
variable "environment" {
  type        = string
  description = "The environment name, defined in environments vars."
}
variable "aws_region" {
  default     = "eu-west-2"
  type        = string
  description = "The AWS region for deployment."
}
variable "aws_profile" {
  default     = "development-eu-west-2"
  type        = string
  description = "The AWS profile to use for deployment."
}

# ------------------------------------------------------------------------------
# Docker Container
# ------------------------------------------------------------------------------
variable "docker_registry" {
  type        = string
  description = "The FQDN of the Docker registry."
}

# ------------------------------------------------------------------------------
# Service performance and scaling configs
# ------------------------------------------------------------------------------
variable "desired_task_count" {
  type        = number
  description = "The desired ECS task count for this service"
  default     = 1 # defaulted low for dev environments, override for production
}
variable "required_cpus" {
  type        = number
  description = "The required cpu resource for this service. 1024 here is 1 vCPU"
  default     = 256 # defaulted low for dev environments, override for production
}
variable "required_memory" {
  type        = number
  description = "The required memory for this service"
  default     = 512 # defaulted low for dev environments, override for production
}
variable "eric_cpus" {
  type        = number
  description = "The required cpu resource for the eric reverse proxy sidecar. Combined with required_cpus this must equal a valid Fargate task cpu value"
  default     = 256 # override for staging/live if needed to keep required_cpus + eric_cpus at a valid Fargate total
}
variable "eric_memory" {
  type        = number
  description = "The required memory for the eric reverse proxy sidecar. Combined with required_memory this must equal a valid Fargate task memory value"
  default     = 512 # override for staging/live if needed to keep required_memory + eric_memory at a valid Fargate total
}

# ------------------------------------------------------------------------------
# Service environment variable configs
# ------------------------------------------------------------------------------
variable "presenters_api_version" {
  type        = string
  description = "The version of the presenters api container to run."
}
variable "eric_version" {
  type        = string
  description = "The version of the eric container to run."
}
variable "log_level" {
  default     = "info"
  type        = string
  description = "The log level for services to use: trace, debug, info or error"
}
variable "ssm_version_prefix" {
  type        = string
  description = "String to use as a prefix to the names of the variables containing variables and secrets version."
  default     = "SSM_VERSION_"
}
