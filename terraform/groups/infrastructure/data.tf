data "vault_generic_secret" "service_secrets" {
  path = "applications/${var.aws_profile}/${var.environment}/${local.stack_name}"
}

data "aws_kms_key" "stack_configs" {
  key_id = local.kms_alias
}

data "aws_subnets" "application" {
  filter {
    name   = "tag:Name"
    values = [local.application_subnet_pattern]
  }
  filter {
    name   = "tag:NetworkType"
    values = ["private"]
  }
}

data "aws_subnet" "application" {
  for_each = toset(data.aws_subnets.application.ids)
  id       = each.value
}

data "aws_subnets" "public" {
  filter {
    name   = "tag:NetworkType"
    values = ["public"]
  }
  filter {
    name   = "tag:Name"
    values = [local.public_subnet_pattern]
  }
}

data "aws_subnet" "public" {
  for_each = toset(data.aws_subnets.public.ids)
  id       = each.value
}

data "aws_vpc" "vpc" {
  filter {
    name   = "tag:Name"
    values = [local.vpc_name]
  }
}
