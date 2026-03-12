# Task 1.7: 테스트 코드

공통 타입: adlex-docs/shared-types.md

## 의존성
- backend/004 (CheckController, DTO)
- backend/006 (ApiKeyAuthFilter, RateLimiter)

## INPUT
- adlex-api/.../engine/evaluator/*.kt
- adlex-api/.../api/controller/CheckController.kt
- adlex-api/.../infra/security/ApiKeyAuthFilter.kt
- adlex-api/.../infra/cache/RateLimiter.kt

## OUTPUT 경로
- adlex-api/src/test/.../engine/evaluator/RegexEvaluatorTest.kt
- adlex-api/src/test/.../engine/evaluator/KeywordEvaluatorTest.kt
- adlex-api/src/test/.../engine/evaluator/TimeRangeEvaluatorTest.kt
- adlex-api/src/test/.../engine/evaluator/FieldPresentEvaluatorTest.kt
- adlex-api/src/test/.../domain/service/ComplianceCheckServiceTest.kt
- adlex-api/src/test/.../api/controller/CheckControllerIntegrationTest.kt
- adlex-api/src/test/resources/application-test.yml

## 상세 스펙

### application-test.yml
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    database-platform: org.hibernate.dialect.H2Dialect
  flyway:
    enabled: false
  data:
    redis:
      host: localhost
      port: 6379

adlex:
  jwt:
    secret: test-secret-key-for-unit-testing-minimum-256-bits-long
```
Redis: 테스트에서 MockK로 RedisTemplate mock 처리

### 단위 테스트 (MockK, JUnit 5)

**RegexEvaluatorTest:**
- REQUIRE + 매칭: `"(광고) 봄 세일"` → null (위반 없음)
- REQUIRE + 미매칭: `"봄 세일 50% 할인"` → EvaluationResult (AD_LABEL 위반)
- target=subject: options에서 subject 검사

**KeywordEvaluatorTest:**
- DENY + 키워드 포함: `"100% 확실한 효과"` → violation
- DENY + 키워드 미포함: `"좋은 제품입니다"` → null

**TimeRangeEvaluatorTest:**
- deny 21:00-08:00, scheduledAt=22:00 KST → violation
- deny 21:00-08:00, scheduledAt=10:00 KST → null
- scheduledAt=null → Instant.now() 사용 (테스트에서 Clock mock)

**FieldPresentEvaluatorTest:**
- field=senderName, sender.name="회사" → null
- field=senderName, sender=null → violation
- field=unsubscribeUrl, options에 없음 → violation

**ComplianceCheckServiceTest:**
- Mock: RuleRegistry, CheckLogRepository, List<RuleEvaluator>
- 정상 메시지 → compliant=true, violations=[]
- 위반 1건 → compliant=false, violationCount=1
- skipRules 적용 → 해당 규칙 스킵 확인
- CheckLog 저장 호출 verify

### 통합 테스트 (@SpringBootTest, @AutoConfigureMockMvc, @ActiveProfiles("test"))

**CheckControllerIntegrationTest:**
- @TestConfiguration에서 RedisTemplate MockBean 제공

테스트 시나리오:
```
1. 정상 SMS: "(광고) 봄 세일! 080-1234-5678 수신거부" → 200, compliant=true
2. 광고 미표시: "봄 세일 50% 할인" channel=SMS → 200, compliant=false, violations contains AD_LABEL
3. 080 미표시: "(광고) 봄 세일" channel=SMS → violation OPT_OUT_080
4. 야간 발송: scheduledAt=22:00 KST, channel=SMS → violation NIGHT_SMS
5. 복합 위반: 광고 미표시 + 080 미표시 + 야간 → violationCount=3
6. 정상 이메일: subject에 (광고), unsubscribeUrl 있음 → compliant=true
7. 인증 실패: X-API-Key 헤더 없이 호출 → 401
8. Batch: 3건 요청 → results.size=3, summary 검증
```

API Key 인증 우회: 테스트에서 SecurityContext에 직접 Authentication 설정하거나 @WithMockUser + 커스텀 설정

## 완료 조건
.\gradlew.bat test 전체 통과 (0 failures)

## 커밋
test(api): add unit and integration tests
