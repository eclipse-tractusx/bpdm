package com.catenax.gpdm.repository

import com.catenax.gpdm.entity.LegalForm
import org.springframework.data.repository.PagingAndSortingRepository

interface LegalFormRepository : PagingAndSortingRepository<LegalForm, Long> {
    fun findByTechnicalKey(key: String): LegalForm?
    fun findByTechnicalKeyIn(keys: Set<String>): Set<LegalForm>
}