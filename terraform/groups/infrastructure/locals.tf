# Define all hardcoded local variable and local variables looked up from data resources
locals {
  stack_name                = "filing-core" # this must match the stack name the service deploys into
  name_prefix               = "${local.stack_name}-${var.environment}"
  service_name              = "presenters-api" # testing service name
  container_port            = "8080"
  docker_repo               = "presenters-api"
  lb_listener_rule_priority = 14
  lb_listener_paths         = [
    "/presenters*"
  ]
  healthcheck_path          = "/presenters/healthcheck" # healthcheck path for presenters api
  healthcheck_matcher       = "200"
  vpc_name                  = local.stack_secrets["vpc_name"]

  kms_alias       = "alias/${var.aws_profile}/environment-services-kms"
  service_secrets = jsondecode(data.vault_generic_secret.service_secrets.data_json)

  application_subnet_pattern = local.stack_secrets["application_subnet_pattern"]
  public_subnet_pattern      = local.stack_secrets["public_subnet_pattern"]

  global_secret_list = flatten([for key, value in local.global_secrets_arn_map :
    { "name" = upper(key), "valueFrom" = value }
  ])

  service_secret_list = flatten([for key, value in local.service_secrets_arn_map :
    { "name" = upper(key), "valueFrom" = value }
  ])

  ssm_global_version_map = [
    for sec in data.aws_ssm_parameter.global_secret : {
      name = "GLOBAL_${var.ssm_version_prefix}${replace(upper(basename(sec.name)), "-", "_")}", value = sec.version
    }
  ]

  service_secrets_arn_map = {
    for sec in module.secrets.secrets :
    trimprefix(sec.name, "/${local.service_name}-${var.environment}/") => sec.arn
  }

  # Dapperdox secrets to go in list
  task_secrets = concat(local.service_secret_list, local.global_secret_list, [])

  task_environment = concat(local.ssm_global_version_map, local.ssm_service_version_map, [
    { "name" : "PORT", "value" : local.container_port },
    { "name" : "LOGLEVEL", "value" : var.log_level }
  ])

  # get eric secrets from global secrets map
  eric_secrets = [
    { "name" : "API_KEY", "valueFrom" : local.global_secrets_arn_map.eric_api_key },
    { "name" : "AES256_KEY", "valueFrom" : local.global_secrets_arn_map.eric_aes256_key }
  ]
  eric_environment_filename = "eric.env"
}
