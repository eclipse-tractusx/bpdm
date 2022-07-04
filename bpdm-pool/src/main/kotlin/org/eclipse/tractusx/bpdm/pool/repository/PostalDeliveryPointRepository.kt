package org.eclipse.tractusx.bpdm.pool.repository

import org.eclipse.tractusx.bpdm.pool.entity.PostalDeliveryPoint
import org.springframework.data.repository.PagingAndSortingRepository

interface PostalDeliveryPointRepository : PagingAndSortingRepository<PostalDeliveryPoint, Long> {
}