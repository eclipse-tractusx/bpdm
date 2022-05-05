package com.catenax.gpdm.repository.entity

import com.catenax.gpdm.entity.BusinessStatus
import org.springframework.data.repository.PagingAndSortingRepository

interface BusinessStatusRepository : PagingAndSortingRepository<BusinessStatus, Long> {
}