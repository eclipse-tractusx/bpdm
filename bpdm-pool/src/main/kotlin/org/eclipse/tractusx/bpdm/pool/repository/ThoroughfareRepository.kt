package org.eclipse.tractusx.bpdm.pool.repository

import org.eclipse.tractusx.bpdm.pool.entity.Thoroughfare
import org.springframework.data.repository.PagingAndSortingRepository

interface ThoroughfareRepository : PagingAndSortingRepository<Thoroughfare, Long> {
}