# Task 6.3: 멀티유저 (백엔드)

## INPUT
- adlex-api/.../domain/entity/Tenant.kt

## OUTPUT 경로
- adlex-api/.../domain/entity/TenantMember.kt
- adlex-api/.../domain/repository/TenantMemberRepository.kt
- adlex-api/.../api/controller/MemberController.kt
- adlex-api/.../domain/service/MemberService.kt
- adlex-api/src/main/resources/db/migration/V6__multi_user.sql

## 상세 스펙
조직 내 역할 분리: OWNER/ADMIN/MEMBER.
팀원 초대/관리. 역할별 권한 제어.

## 완료 조건
.\gradlew.bat build

## 커밋
feat(api): implement multi-user team management
