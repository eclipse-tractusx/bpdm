package org.eclipse.tractusx.bpdm.pool.repository

import org.eclipse.tractusx.bpdm.pool.entity.IssuingBody
import org.springframework.data.repository.PagingAndSortingRepository

interface IssuingBodyRepository : PagingAndSortingRepository<IssuingBody, Long> {
    fun findByTechnicalKey(key: String): IssuingBody?
    fun findByTechnicalKeyIn(keys: Set<String>): Set<IssuingBody>
}