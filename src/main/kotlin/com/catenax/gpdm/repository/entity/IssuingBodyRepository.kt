package com.catenax.gpdm.repository.entity

import com.catenax.gpdm.entity.IssuingBody
import org.springframework.data.repository.PagingAndSortingRepository

interface IssuingBodyRepository : PagingAndSortingRepository<IssuingBody, Long> {
    fun findByTechnicalKey(key: String): IssuingBody?
    fun findByTechnicalKeyIn(keys: Set<String>): Set<IssuingBody>
}