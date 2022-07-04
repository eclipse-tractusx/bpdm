package org.eclipse.tractusx.bpdm.pool.repository

import org.eclipse.tractusx.bpdm.pool.entity.IdentifierType
import org.springframework.data.repository.PagingAndSortingRepository

interface IdentifierTypeRepository : PagingAndSortingRepository<IdentifierType, Long> {
    fun findByTechnicalKey(key: String): IdentifierType?
    fun findByTechnicalKeyIn(technicalKeys: Set<String>): Set<IdentifierType>
}