# Task 2.1: Docker
 
## OUTPUT 경로
- adlex-api/Dockerfile
- adlex-dashboard/Dockerfile
- adlex-dashboard/nginx.conf
 
## 상세 스펙
adlex-api: gradle:8.6-jdk21 → temurin:21-jre-alpine.
adlex-dashboard: node:20 → nginx:alpine. SPA fallback.
 
## 완료 조건
docker build -t adlex-api adlex-api/
 
## 커밋
chore(infra): add Dockerfiles for AdLex