---
name: flyway-migration
description: DB 변경 시 Flyway SQL + JPA Entity 동시 생성. "DB 변경" 요청 시 사용.
---
# Flyway Migration
## V{N}__{desc}.sql + Entity.kt 동시 생성.
## PostgreSQL 16. UUID=gen_random_uuid(). SQL 변경 시 Entity 필수 동기화.