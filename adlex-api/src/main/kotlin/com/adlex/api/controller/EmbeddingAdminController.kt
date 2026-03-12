package com.adlex.api.controller

import com.adlex.infra.embedding.EmbeddingPipelineResult
import com.adlex.infra.embedding.EmbeddingPipelineService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Admin: Embedding", description = "임베딩 파이프라인 관리 (JWT 인증 필요)")
@RestController
@RequestMapping("/api/admin/embeddings")
class EmbeddingAdminController(
    private val pipelineService: EmbeddingPipelineService
) {

    @Operation(summary = "전체 임베딩 생성", description = "law_chunks + precedents 임베딩을 일괄 생성합니다.")
    @PostMapping("/generate")
    fun generateAll(): ResponseEntity<EmbeddingPipelineResult> =
        ResponseEntity.ok(pipelineService.generateAll())

    @Operation(summary = "법령 청크 임베딩 생성")
    @PostMapping("/generate/law-chunks")
    fun generateLawChunks(): ResponseEntity<EmbeddingPipelineResult> =
        ResponseEntity.ok(pipelineService.generateLawChunkEmbeddings())

    @Operation(summary = "판례 임베딩 생성")
    @PostMapping("/generate/precedents")
    fun generatePrecedents(): ResponseEntity<EmbeddingPipelineResult> =
        ResponseEntity.ok(pipelineService.generatePrecedentEmbeddings())
}
