package com.catenax.gpdm.repository

import com.catenax.gpdm.entity.IssuingBody
import org.springframework.data.repository.PagingAndSortingRepository

interface IssuingBodyRepository : PagingAndSortingRepository<IssuingBody, Long> {
    fun findByTechnicalKey(key: String): IssuingBody?
    fun findByTechnicalKeyIn(keys: Set<String>): Set<IssuingBody>
}