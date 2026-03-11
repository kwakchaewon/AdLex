# Task 4.3: CloudWatch Alarm + Slack 알림

## OUTPUT 경로
- adlex-infra/terraform/cloudwatch.tf
- adlex-infra/terraform/sns.tf

## 상세 스펙
ECS CPU/Memory, ALB 5xx, RDS CPU/Storage 알람.
SNS → Slack #adlex-alerts 채널 연동.

## 완료 조건
terraform plan 성공

## 커밋
chore(infra): configure CloudWatch alarms and Slack notifications
