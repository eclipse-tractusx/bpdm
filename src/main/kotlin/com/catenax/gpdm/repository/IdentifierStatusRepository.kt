package com.catenax.gpdm.repository

import com.catenax.gpdm.entity.IdentifierStatus
import org.springframework.data.repository.PagingAndSortingRepository

interface IdentifierStatusRepository : PagingAndSortingRepository<IdentifierStatus, Long> {
    fun findByTechnicalKey(key: String): IdentifierStatus?
    fun findByTechnicalKeyIn(technicalKeys: Set<String>): Set<IdentifierStatus>
}