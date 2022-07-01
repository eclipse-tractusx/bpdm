package org.eclipse.tractusx.bpdm.pool.repository

import org.eclipse.tractusx.bpdm.pool.entity.Locality
import org.springframework.data.repository.PagingAndSortingRepository

interface LocalityRepository : PagingAndSortingRepository<Locality, Long> {
}