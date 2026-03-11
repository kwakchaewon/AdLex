---
name: spring-api-generator
description: Spring Boot REST API 생성. Controller+DTO+Service+Test 세트. "API 만들어줘" 등의 요청 시 사용. Spring Boot, Kotlin 작업이면 항상 참조.
---
# Spring API Generator
## 생성 파일 (도메인={Domain})
1. adlex-api/.../api/controller/{Domain}Controller.kt
2. adlex-api/.../api/dto/{Domain}Request.kt, {Domain}Response.kt
3. adlex-api/.../domain/service/{Domain}Service.kt
4. adlex-api/.../domain/repository/{Domain}Repository.kt (필요시)
5. adlex-api/src/test/.../domain/service/{Domain}ServiceTest.kt
(adlex-api/... = adlex-api/src/main/kotlin/com/adlex/)
## Controller: @RestController + @Tag + @Operation + @Valid
## DTO: data class + Validation + @Schema
## Service: @Service. BusinessException 패턴.
## Test: @SpringBootTest + MockMvc. 200/400/401 케이스.
## 토큰: 경로 탐색 금지. import 완전. 설명 최소.
