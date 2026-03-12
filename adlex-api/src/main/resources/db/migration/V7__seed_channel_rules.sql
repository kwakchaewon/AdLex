-- V7: 채널별 규칙 세분화 시드 데이터
-- LENGTH_CHECK, CONTENT_RATIO 타입 규칙 추가 (Phase 3.12)

-- ─────────────────────────────────────────────────────────────────────────────
-- SMS 길이 제한 (정보통신망법 / KISA SMS 발송 기준: 90바이트 EUC-KR)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO rules (code, name, description, type, channel, severity, pattern, config, legal_basis, active)
VALUES (
    'CH_SMS_LEN_001',
    'SMS 메시지 길이 제한',
    'SMS 메시지는 EUC-KR 기준 90바이트(한글 45자·영문 90자)를 초과할 수 없습니다.',
    'LENGTH_CHECK',
    'SMS',
    'HIGH',
    NULL,
    '{"maxBytes": 90, "encoding": "EUC-KR"}',
    '전기통신사업법 시행령 제2조 (문자메시지 발송 기준)',
    true
)
ON CONFLICT (code) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- KAKAO 길이 제한 (카카오 알림톡/친구톡 최대 1,000자)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO rules (code, name, description, type, channel, severity, pattern, config, legal_basis, active)
VALUES (
    'CH_KAKAO_LEN_001',
    '카카오 메시지 길이 제한',
    '카카오 알림톡·친구톡 메시지는 1,000자를 초과할 수 없습니다.',
    'LENGTH_CHECK',
    'KAKAO',
    'HIGH',
    NULL,
    '{"maxChars": 1000}',
    '카카오 알림톡 정책 (카카오비즈메시지 가이드)',
    true
)
ON CONFLICT (code) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- EMAIL 제목 길이 제한 (관행 기준 200자)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO rules (code, name, description, type, channel, severity, pattern, config, legal_basis, active)
VALUES (
    'CH_EMAIL_LEN_001',
    '이메일 제목 길이 권고',
    '이메일 제목이 200자를 초과하면 스팸 필터에 걸릴 위험이 높습니다.',
    'LENGTH_CHECK',
    'EMAIL',
    'LOW',
    NULL,
    '{"maxChars": 200, "checkSubject": true}',
    NULL,
    true
)
ON CONFLICT (code) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- 과장 표현 밀도 제한 — 최상급 표현 (전 채널)
-- 표시·광고의 공정화에 관한 법률 제3조 (부당한 표시·광고 행위의 금지)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO rules (code, name, description, type, channel, severity, pattern, config, legal_basis, active)
VALUES (
    'CH_ALL_RATIO_001',
    '최상급 과장 표현 과다',
    '최고·최상·1등 등 최상급 표현이 3회 초과 사용된 경우 과장광고로 간주될 수 있습니다.',
    'CONTENT_RATIO',
    'SMS,KAKAO,EMAIL',
    'MEDIUM',
    '최고,최상,1등,최저가,최고급,최상급,독보적,압도적,완벽한',
    '{"maxCount": 3, "keywords": "최고,최상,1등,최저가,최고급,최상급,독보적,압도적,완벽한"}',
    '표시·광고의 공정화에 관한 법률 제3조 제1항 제1호',
    true
)
ON CONFLICT (code) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- 과장 표현 밀도 제한 — 절대적 효과 표현 (전 채널)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO rules (code, name, description, type, channel, severity, pattern, config, legal_basis, active)
VALUES (
    'CH_ALL_RATIO_002',
    '절대적 효과 과장 표현',
    '완전무료·100% 효과·무조건 등 절대적 효과를 주장하는 표현이 과다 사용된 경우입니다.',
    'CONTENT_RATIO',
    'SMS,KAKAO,EMAIL',
    'HIGH',
    '완전무료,100% 효과,무조건,반드시,확실히,절대적,완전히 해결',
    '{"maxCount": 1, "keywords": "완전무료,100% 효과,무조건 효과,반드시 낫는,확실히 치료,절대적 효능"}',
    '표시·광고의 공정화에 관한 법률 제3조 제1항 제2호',
    true
)
ON CONFLICT (code) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- SMS 과대광고 키워드 밀도 — SMS 특화 (SMS 특성상 짧은 문자에 과장이 집중)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO rules (code, name, description, type, channel, severity, pattern, config, legal_basis, active)
VALUES (
    'CH_SMS_RATIO_001',
    'SMS 광고성 키워드 집중',
    'SMS 특성상 짧은 문자 내 광고성 키워드가 집중되면 스팸 및 과장광고 위험이 높습니다.',
    'CONTENT_RATIO',
    'SMS',
    'MEDIUM',
    '할인,이벤트,특가,혜택,무료,증정,선착순,한정,지금바로',
    '{"maxCount": 4, "keywords": "할인,이벤트,특가,혜택,무료,증정,선착순,한정,지금바로"}',
    '표시·광고의 공정화에 관한 법률 제3조',
    true
)
ON CONFLICT (code) DO NOTHING;
