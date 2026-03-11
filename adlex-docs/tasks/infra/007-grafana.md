# Task 4.2: Grafana 대시보드

## OUTPUT 경로
- adlex-api/build.gradle.kts (micrometer-prometheus 추가)
- adlex-api/.../infra/config/MetricsConfig.kt
- adlex-infra/grafana/dashboard.json

## 상세 스펙
Micrometer + Prometheus.
커스텀 메트릭: adlex.check.total, adlex.check.duration, adlex.check.violations, adlex.cache.hit, adlex.ratelimit.exceeded.

## 완료 조건
/actuator/prometheus 메트릭 노출

## 커밋
chore(infra): add Prometheus metrics and Grafana dashboard
