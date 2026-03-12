-- ============================================================
-- AdLex V4: pgvector 확장 + 법령 청크 + 판례 테이블
-- RAG (Retrieval-Augmented Generation) 인프라
-- ============================================================

-- pgvector 확장 활성화
CREATE EXTENSION IF NOT EXISTS vector;

-- 법령 청크 테이블
-- 법령 조문을 청크 단위로 저장. embedding은 코사인 유사도 검색용.
CREATE TABLE law_chunks (
    id            BIGSERIAL     PRIMARY KEY,
    law_name      VARCHAR(200)  NOT NULL,           -- 법령명 (예: 표시광고법)
    article_no    VARCHAR(50),                       -- 조항 번호 (예: 제3조제1항)
    article_title VARCHAR(200),                      -- 조항 제목
    content       TEXT          NOT NULL,            -- 조문 내용 (청크)
    embedding     vector(1536),                      -- 임베딩 벡터 (text-embedding-3-small)
    law_date      DATE,                              -- 법령 시행일
    source_url    TEXT,                              -- 국가법령정보센터 원문 URL
    active        BOOLEAN       NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- 판례/시정조치 사례 테이블
-- 공정위·방통위 등 시정조치 사례. 유사 판례 검색에 사용.
CREATE TABLE precedents (
    id          BIGSERIAL     PRIMARY KEY,
    case_no     VARCHAR(100),                        -- 사건 번호
    authority   VARCHAR(100)  NOT NULL,              -- 처분 기관 (예: 공정거래위원회)
    title       VARCHAR(500)  NOT NULL,              -- 사건 제목
    summary     TEXT,                                -- 요약
    content     TEXT          NOT NULL,              -- 상세 내용 (청크)
    embedding   vector(1536),                        -- 임베딩 벡터
    decided_at  DATE,                                -- 결정일
    active      BOOLEAN       NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- IVFFlat 인덱스 (근사 최근접 이웃 검색)
-- 데이터 없는 상태에서 인덱스 생성 가능 (데이터 삽입 후 VACUUM ANALYZE 권장)
CREATE INDEX idx_law_chunks_embedding  ON law_chunks  USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
CREATE INDEX idx_precedents_embedding  ON precedents  USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- 일반 인덱스
CREATE INDEX idx_law_chunks_active     ON law_chunks(active)   WHERE active = true;
CREATE INDEX idx_law_chunks_law_name   ON law_chunks(law_name);
CREATE INDEX idx_precedents_active     ON precedents(active)   WHERE active = true;
CREATE INDEX idx_precedents_authority  ON precedents(authority);
