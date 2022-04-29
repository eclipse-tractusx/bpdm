package com.catenax.gpdm.repository

import com.catenax.gpdm.entity.LegalForm
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository

interface LegalFormRepository : PagingAndSortingRepository<LegalForm, Long> {
    fun findByTechnicalKey(key: String): LegalForm?
    fun findByTechnicalKeyIn(keys: Set<String>): Set<LegalForm>

    @Query("SELECT DISTINCT l FROM LegalForm l LEFT JOIN FETCH l.categories WHERE l IN :legalForms")
    fun joinCategories(legalForms: Set<LegalForm>): Set<LegalForm>

}