package org.eclipse.tractusx.bpdm.pool.repository

import org.eclipse.tractusx.bpdm.pool.entity.IdentifierStatus
import org.springframework.data.repository.PagingAndSortingRepository

interface IdentifierStatusRepository : PagingAndSortingRepository<IdentifierStatus, Long> {
    fun findByTechnicalKey(key: String): IdentifierStatus?
    fun findByTechnicalKeyIn(technicalKeys: Set<String>): Set<IdentifierStatus>
}