# Task 1.2: DB 스키마 + Flyway
 
## INPUT
- adlex-api/src/main/resources/application.yml
 
## OUTPUT 경로
- adlex-api/src/main/resources/db/migration/V1__init_schema.sql
- adlex-api/src/main/resources/db/migration/V2__seed_rules.sql
- adlex-api/src/main/kotlin/com/adlex/domain/entity/Tenant.kt
- adlex-api/src/main/kotlin/com/adlex/domain/entity/ApiKey.kt
- adlex-api/src/main/kotlin/com/adlex/domain/entity/Rule.kt
- adlex-api/src/main/kotlin/com/adlex/domain/entity/CheckLog.kt
- adlex-api/src/main/kotlin/com/adlex/domain/repository/{각 Repository}.kt
 
## 상세 스펙
V1: tenants, api_keys, rules, check_logs(파티션) + 인덱스
V2: 7개 규칙 시드 (개발계획서 참조)
 
## 완료 조건
.\gradlew.bat bootRun → Flyway 성공
 
## 커밋
feat(db): add initial schema and seed compliance rules
