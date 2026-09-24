provider "aws" {
  region = var.aws_region
}

terraform {
  backend "s3" {
  }
  required_version = ">= 1.3, < 2.0.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 6.0, < 7.0"
    }

    vault = {
      source  = "hashicorp/vault"
      version = ">= 5.0, < 6.0"
    }
  }
}

module "ecs_service" {
  source = "git@github.com:companieshouse/terraform-modules//aws/ecs/ecs-service?ref=1.0.427"

  # Environmental configuration
  environment             = var.environment
  aws_region              = var.aws_region
  aws_profile             = var.aws_profile
  vpc_id                  = data.aws_vpc.vpc.id
  ecs_cluster_id          = data.aws_ecs_cluster.ecs_cluster.id
  task_execution_role_arn = data.aws_iam_role.ecs_cluster_iam_role.arn

  # Load balancer configuration
  lb_listener_arn           = data.aws_lb_listener.service_lb_listener.arn
  lb_listener_rule_priority = local.lb_listener_rule_priority
  lb_listener_paths         = local.lb_listener_paths
  multilb_setup             = true
  multilb_listeners = {
    "private-api-lb" = {
      listener_arn      = data.aws_lb_listener.internal_service_lb_listener.arn
      load_balancer_arn = data.aws_lb.internal_service_lb.arn
    }
    "public-api-lb" = {
      listener_arn      = data.aws_lb_listener.service_lb_listener.arn
      load_balancer_arn = data.aws_lb.service_lb.arn
    }
  }
  healthcheck_path          = local.healthcheck_path
  healthcheck_matcher       = local.healthcheck_matcher

  # Docker container details
  docker_registry   = var.docker_registry
  docker_repo       = local.docker_repo
  container_version = var.presenters_api_version
  container_port    = local.container_port

  # Service configuration
  service_name = local.service_name
  name_prefix  = local.name_prefix

  # Service Healthcheck configuration

  # Service performance and scaling configs
  use_fargate        = true
  desired_task_count = var.desired_task_count
  required_cpus      = var.required_cpus
  required_memory    = var.required_memory

  # Service environment variable and secret configs
  task_environment          = local.task_environment
  task_secrets              = local.task_secrets
  app_environment_filename  = local.app_environment_filename
  eric_port                 = local.eric_port
  eric_secrets              = local.eric_secrets
  eric_version              = local.eric_version
  eric_environment_filename = local.eric_environment_filename
  use_eric_reverse_proxy    = true
  use_set_environment_files = true

}

module "secrets" {
  source = "git@github.com:companieshouse/terraform-modules//aws/parameter-store?ref=1.0.427"

  name_prefix = "${local.service_name}-${var.environment}"
  kms_key_id  = data.aws_kms_key.stack_configs.id
  secrets     = nonsensitive(local.service_secrets)
}
