package com.catenax.gpdm.repository

import com.catenax.gpdm.entity.IdentifierType
import org.springframework.data.repository.PagingAndSortingRepository

interface IdentifierTypeRepository : PagingAndSortingRepository<IdentifierType, Long> {
    fun findByTechnicalKey(key: String): IdentifierType?
    fun findByTechnicalKeyIn(technicalKeys: Set<String>): Set<IdentifierType>
}