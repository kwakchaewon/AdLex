package com.adlex.domain.repository

import com.adlex.domain.entity.CheckLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CheckLogRepository : JpaRepository<CheckLog, Long>
// 추후 Task 3.4 (히스토리 API)에서 커스텀 쿼리 추가 예정
