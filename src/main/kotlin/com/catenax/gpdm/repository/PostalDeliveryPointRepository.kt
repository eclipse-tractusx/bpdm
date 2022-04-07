package com.catenax.gpdm.repository.entity

import com.catenax.gpdm.entity.PostalDeliveryPoint
import org.springframework.data.repository.PagingAndSortingRepository

interface PostalDeliveryPointRepository : PagingAndSortingRepository<PostalDeliveryPoint, Long> {
}