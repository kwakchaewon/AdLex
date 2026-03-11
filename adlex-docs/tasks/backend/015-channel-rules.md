# Task 5.3: 채널별 특화 규칙

## INPUT
- adlex-api/.../domain/entity/Rule.kt

## OUTPUT 경로
- adlex-api/src/main/resources/db/migration/V4__add_channel_rules.sql
- adlex-api/.../engine/evaluator 수정 (채널별 로직)

## 상세 스펙
카카오: 08:00~20:50 시간 제한.
이메일: 제목 (광고) 표기, 수신거부 링크 필수.
V4__add_channel_rules.sql로 채널별 규칙 시드.

## 완료 조건
.\gradlew.bat test

## 커밋
feat(engine): add channel-specific compliance rules
