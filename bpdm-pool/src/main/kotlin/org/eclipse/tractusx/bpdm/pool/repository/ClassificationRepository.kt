package org.eclipse.tractusx.bpdm.pool.repository

import org.eclipse.tractusx.bpdm.pool.entity.Classification
import org.springframework.data.repository.PagingAndSortingRepository

interface ClassificationRepository : PagingAndSortingRepository<Classification, Long> {
}