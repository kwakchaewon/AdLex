-- ============================================================
-- AdLex V2: Seed Compliance Rules (7개)
-- ============================================================

INSERT INTO rules (code, name, type, channel, severity, pattern, config, legal_basis, active) VALUES

('AD_LABEL',
 '(광고) 표기 의무',
 'REGEX', 'SMS,KAKAO', 'HIGH',
 '^\(광고\)',
 '{"matchMode": "REQUIRE"}',
 '정보통신망법 제50조 제1항 (광고성 정보 전송 시 표기 의무)',
 true),

('OPT_OUT_080',
 '080 수신거부 번호 표기',
 'REGEX', 'SMS', 'HIGH',
 '080-\d{3,4}-\d{4}',
 '{"matchMode": "REQUIRE"}',
 '정보통신망법 제50조의5 (수신거부의사 표시 방법 제공 의무)',
 true),

('OPT_OUT_LINK',
 '이메일 수신거부 링크',
 'FIELD_PRESENT', 'EMAIL', 'HIGH',
 NULL,
 '{"field": "unsubscribeUrl"}',
 '정보통신망법 제50조의5 (수신거부의사 표시 방법 제공 의무)',
 true),

('NIGHT_SMS',
 'SMS 야간 발송 제한',
 'TIME_RANGE', 'SMS', 'HIGH',
 NULL,
 '{"denyStart": "21:00", "denyEnd": "08:00", "timezone": "Asia/Seoul"}',
 '정보통신망법 제50조 제4항 (야간 광고성 정보 전송 제한)',
 true),

('NIGHT_KAKAO',
 '카카오 야간 발송 제한',
 'TIME_RANGE', 'KAKAO', 'HIGH',
 NULL,
 '{"denyStart": "20:50", "denyEnd": "08:00", "timezone": "Asia/Seoul"}',
 '카카오비즈메시지 운영정책 (야간 시간대 발송 제한)',
 true),

('EMAIL_SUBJECT',
 '이메일 제목 (광고) 표기',
 'REGEX', 'EMAIL', 'HIGH',
 '\(광고\)',
 '{"matchMode": "REQUIRE", "target": "subject"}',
 '정보통신망법 제50조 제1항 (광고성 정보 전송 시 표기 의무)',
 true),

('SENDER_ID',
 '발신자 정보 표기',
 'FIELD_PRESENT', 'SMS,KAKAO', 'MEDIUM',
 NULL,
 '{"field": "senderName"}',
 '표시·광고의 공정화에 관한 법률 제3조 (표시·광고의 공정화)',
 true);
