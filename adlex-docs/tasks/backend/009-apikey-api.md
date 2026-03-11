# Task 3.3: API Key 관리 API

## INPUT
- adlex-api/.../domain/service/ApiKeyService.kt

## OUTPUT 경로
- adlex-api/.../api/controller/ApiKeyController.kt
- adlex-api/.../api/dto/ApiKeyRequest.kt
- adlex-api/.../api/dto/ApiKeyResponse.kt

## 상세 스펙
GET /api/keys: 현재 테넌트의 API Key 목록.
POST /api/keys: 새 키 생성 (이름 필수). 생성 시 키 1회 노출.
PUT /api/keys/{id}: 이름/상태 수정.
DELETE /api/keys/{id}: 삭제 (최소 1개 유지).

## 완료 조건
.\gradlew.bat build && Swagger에서 CRUD 테스트

## 커밋
feat(api): implement API key management CRUD
