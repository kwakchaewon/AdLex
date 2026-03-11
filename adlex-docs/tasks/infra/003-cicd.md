# Task 2.3: CI/CD 파이프라인 (GitHub Actions)

## OUTPUT 경로
- .github/workflows/deploy.yml

## 상세 스펙
on: push branches [main].
steps: checkout → java 21 → test → bootJar → ECR login → docker build/push → ECS update.
Secrets: AWS credentials, ECR, ECS, JWT_SECRET, DB_PASSWORD.

## 완료 조건
GitHub Actions 워크플로우 문법 검증

## 커밋
chore(infra): add GitHub Actions CI/CD pipeline
