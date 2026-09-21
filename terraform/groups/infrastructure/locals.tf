# Define all hardcoded local variable and local variables looked up from data resources
locals {
  stack_name                = "filing-core" # this must match the stack name the service deploys into
  name_prefix               = "${local.stack_name}-${var.environment}"
  service_name              = "presenters-api" # testing service name
  container_port            = "8080" # default node port required here until prod docker container is built allowing port change via env var
  docker_repo               = "presenters-api"
  lb_listener_rule_priority = 14
  lb_listener_paths         = [
    "/presenters*"
  ]
  healthcheck_path          = "/presenters/health" # healthcheck path for presenters api
  healthcheck_matcher       = "200"

  kms_alias       = "alias/${var.aws_profile}/environment-services-kms"
  service_secrets = jsondecode(data.vault_generic_secret.service_secrets.data_json)

  application_subnet_pattern = local.stack_secrets["application_subnet_pattern"]
  public_subnet_pattern      = local.stack_secrets["public_subnet_pattern"]

  vpc_name = local.service_secrets["vpc_name"]

  parameter_store_secrets = {
    "account_url"         = local.service_secrets["account_url"]
    "api_url"             = local.service_secrets["api_url"]
    "cache_server"        = local.service_secrets["cache_server"]
    "cdn_host"            = local.service_secrets["cdn_host"]
    "chs_api_key"         = local.service_secrets["chs_api_key"]
    "chs_monitor_gui_url" = local.service_secrets["chs_monitor_gui_url"]
    "chs_url"             = local.service_secrets["chs_url"]
    "cookie_domain"       = local.service_secrets["cookie_domain"]
    "cookie_secret"       = local.service_secrets["cookie_secret"]
    "vpc_name"            = local.service_secrets["vpc_name"]
  }

  # create a map of secret name => secret arn to pass into ecs service module
  # using the trimprefix function to remove the prefixed path from the secret name
  secrets_arn_map = {
    for sec in data.aws_ssm_parameter.secret :
    trimprefix(sec.name, "/${local.name_prefix}/") => sec.arn
  }

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
