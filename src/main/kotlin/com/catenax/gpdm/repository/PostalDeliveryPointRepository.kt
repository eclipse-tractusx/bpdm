package com.catenax.gpdm.repository

import com.catenax.gpdm.entity.PostalDeliveryPoint
import org.springframework.data.repository.PagingAndSortingRepository

interface PostalDeliveryPointRepository : PagingAndSortingRepository<PostalDeliveryPoint, Long> {
}