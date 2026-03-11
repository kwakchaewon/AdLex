# Task 1.7: 테스트 코드

## INPUT
- adlex-api/.../engine/evaluator/*.kt
- adlex-api/.../api/controller/CheckController.kt

## OUTPUT 경로
- adlex-api/src/test/.../engine/evaluator/RegexEvaluatorTest.kt
- adlex-api/src/test/.../engine/evaluator/KeywordEvaluatorTest.kt
- adlex-api/src/test/.../engine/evaluator/TimeRangeEvaluatorTest.kt
- adlex-api/src/test/.../engine/evaluator/FieldPresentEvaluatorTest.kt
- adlex-api/src/test/.../domain/service/ComplianceCheckServiceTest.kt
- adlex-api/src/test/.../api/controller/CheckControllerIntegrationTest.kt
- adlex-api/src/test/resources/application-test.yml

## 상세 스펙
단위 테스트 (MockK): 각 Evaluator + ComplianceCheckService.
통합 테스트 (@SpringBootTest + H2):
- 정상 메시지 → compliant
- (광고) 미표시 → violation
- 080 미표시 → violation
- 야간 시간 → violation
- 복합 위반 → count=3
- 인증 실패 → 401
- Rate Limit → 429

## 완료 조건
.\gradlew.bat test 전체 통과

## 커밋
test(api): add unit and integration tests
