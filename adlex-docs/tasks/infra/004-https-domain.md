# Task 2.4: HTTPS + 도메인 설정

## INPUT
- adlex-infra/terraform/alb.tf

## OUTPUT 경로
- adlex-infra/terraform/route53.tf (수정)
- adlex-infra/terraform/acm.tf (수정)

## 상세 스펙
Route53 도메인: api.adlex.kr, app.adlex.kr.
ACM 인증서: *.adlex.kr.
ALB HTTPS 리스너 + HTTP→HTTPS 리다이렉트.

## 완료 조건
terraform plan 성공

## 커밋
chore(infra): configure HTTPS and domain routing
