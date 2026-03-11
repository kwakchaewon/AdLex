# Task 5.6: SDK Python

## INPUT
- (없음 — API 스펙 기반)

## OUTPUT 경로
- adlex-sdk-python/adlex/__init__.py
- adlex-sdk-python/adlex/client.py
- adlex-sdk-python/setup.py

## 상세 스펙
pip install adlex-compliance. AdLexClient(api_key).
check(), batch_check(). 인증/재시도/타임아웃 내장.

## 완료 조건
pip install -e . && python -c "from adlex import AdLexClient"

## 커밋
feat(api): add Python SDK for AdLex
