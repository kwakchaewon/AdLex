# Task 2.2: AWS 인프라 구성 (Terraform)

## OUTPUT 경로
- adlex-infra/terraform/main.tf
- adlex-infra/terraform/variables.tf
- adlex-infra/terraform/outputs.tf
- adlex-infra/terraform/vpc.tf
- adlex-infra/terraform/ecs.tf
- adlex-infra/terraform/rds.tf
- adlex-infra/terraform/elasticache.tf
- adlex-infra/terraform/alb.tf
- adlex-infra/terraform/route53.tf
- adlex-infra/terraform/acm.tf
- adlex-infra/terraform/security-groups.tf

## 상세 스펙
VPC(Public 2 + Private 2), NAT Gateway.
ECR: adlex, ECS Fargate(0.5vCPU/1GB).
RDS PostgreSQL(t4g.micro), ElastiCache Redis(t4g.micro).
ALB(HTTPS:443→ECS:8080), Route53(api.adlex.kr), ACM(*.adlex.kr).
Security Groups: ALB→ECS→RDS/Redis 체인.

## 완료 조건
terraform plan 성공

## 커밋
chore(infra): add Terraform AWS infrastructure
