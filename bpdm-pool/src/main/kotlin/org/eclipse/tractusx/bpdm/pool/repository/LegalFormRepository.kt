package org.eclipse.tractusx.bpdm.pool.repository

import org.eclipse.tractusx.bpdm.pool.entity.LegalForm
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository

interface LegalFormRepository : PagingAndSortingRepository<LegalForm, Long> {
    fun findByTechnicalKey(key: String): LegalForm?
    fun findByTechnicalKeyIn(keys: Set<String>): Set<LegalForm>

    @Query("SELECT DISTINCT l FROM LegalForm l LEFT JOIN FETCH l.categories WHERE l IN :legalForms")
    fun joinCategories(legalForms: Set<LegalForm>): Set<LegalForm>

}