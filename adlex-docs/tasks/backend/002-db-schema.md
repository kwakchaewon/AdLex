# Task 1.2: DB 스키마 + Flyway

공통 타입: adlex-docs/shared-types.md

## INPUT
- adlex-api/src/main/resources/application.yml

## OUTPUT 경로
- adlex-api/src/main/resources/db/migration/V1__init_schema.sql
- adlex-api/src/main/resources/db/migration/V2__seed_rules.sql
- adlex-api/src/main/kotlin/com/adlex/domain/entity/BaseEntity.kt
- adlex-api/src/main/kotlin/com/adlex/domain/entity/Tenant.kt
- adlex-api/src/main/kotlin/com/adlex/domain/entity/ApiKey.kt
- adlex-api/src/main/kotlin/com/adlex/domain/entity/Rule.kt
- adlex-api/src/main/kotlin/com/adlex/domain/entity/CheckLog.kt
- adlex-api/src/main/kotlin/com/adlex/domain/repository/TenantRepository.kt
- adlex-api/src/main/kotlin/com/adlex/domain/repository/ApiKeyRepository.kt
- adlex-api/src/main/kotlin/com/adlex/domain/repository/RuleRepository.kt
- adlex-api/src/main/kotlin/com/adlex/domain/repository/CheckLogRepository.kt

## 상세 스펙

### V1__init_schema.sql
4개 테이블 생성. 컬럼 정의는 shared-types.md §2 참조.
- `tenants`: email UNIQUE, plan DEFAULT 'FREE', monthly_quota DEFAULT 100, monthly_used DEFAULT 0
- `api_keys`: key_hash UNIQUE, tenant_id FK → tenants(id) ON DELETE CASCADE
- `rules`: code UNIQUE, channel은 CSV 문자열 (e.g. 'SMS,KAKAO')
- `check_logs`: PARTITION BY RANGE(created_at) 월별. violations는 JSONB

인덱스:
```sql
CREATE INDEX idx_api_keys_hash ON api_keys(key_hash);
CREATE INDEX idx_api_keys_tenant ON api_keys(tenant_id);
CREATE INDEX idx_check_logs_tenant_created ON check_logs(tenant_id, created_at);
CREATE INDEX idx_rules_active ON rules(active) WHERE active = true;
```

### V2__seed_rules.sql
7개 규칙 INSERT. shared-types.md §4 Seed Rules 표 참조.
config 컬럼: JSONB. 예: `{"matchMode": "REQUIRE"}`, `{"field": "senderName"}`, `{"denyStart": "21:00", "denyEnd": "08:00", "timezone": "Asia/Seoul"}`

### JPA Entities
- `BaseEntity` (@MappedSuperclass): id(@Id @GeneratedValue IDENTITY), createdAt(@CreationTimestamp), updatedAt(@UpdateTimestamp)
- 각 Entity는 BaseEntity 상속. @Table(name="..."), @Column, @Enumerated(EnumType.STRING)
- `Rule.channel`: String으로 저장, 헬퍼 메서드 `fun channels(): List<Channel>` 로 파싱
- `CheckLog.violations`: String (JSONB), @Column(columnDefinition = "jsonb")

### Enum 클래스
- `com.adlex.domain.entity.Plan` enum (shared-types.md §1)
- `com.adlex.domain.entity.ApiKeyStatus` enum

### Repository
- `TenantRepository`: findByEmail(email: String): Tenant?
- `ApiKeyRepository`: findByKeyHash(keyHash: String): ApiKey?, findAllByTenantId(tenantId: Long): List<ApiKey>
- `RuleRepository`: findAllByActiveTrue(): List<Rule>
- `CheckLogRepository`: JpaRepository<CheckLog, Long> (추후 커스텀 쿼리 추가)

## 완료 조건
.\gradlew.bat bootRun → Flyway 마이그레이션 성공, 테이블 4개 + 시드 7건 확인

## 커밋
feat(db): add initial schema and seed compliance rules
