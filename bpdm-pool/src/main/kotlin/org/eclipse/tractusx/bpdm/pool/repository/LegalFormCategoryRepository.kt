package org.eclipse.tractusx.bpdm.pool.repository

import org.eclipse.tractusx.bpdm.pool.entity.LegalFormCategory
import org.springframework.data.repository.PagingAndSortingRepository

interface LegalFormCategoryRepository : PagingAndSortingRepository<LegalFormCategory, Long> {
    fun findByName(name: String): LegalFormCategory?
}