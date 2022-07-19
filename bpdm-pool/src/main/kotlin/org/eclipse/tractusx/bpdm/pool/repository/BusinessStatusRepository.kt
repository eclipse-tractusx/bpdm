package org.eclipse.tractusx.bpdm.pool.repository

import org.eclipse.tractusx.bpdm.pool.entity.BusinessStatus
import org.springframework.data.repository.PagingAndSortingRepository

interface BusinessStatusRepository : PagingAndSortingRepository<BusinessStatus, Long> {
}