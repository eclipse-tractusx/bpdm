package com.catenax.gpdm.repository.entity

import com.catenax.gpdm.entity.Classification
import org.springframework.data.repository.PagingAndSortingRepository

interface ClassificationRepository : PagingAndSortingRepository<Classification, Long> {
}