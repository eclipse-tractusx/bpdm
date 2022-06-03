package com.catenax.gpdm.repository

import com.catenax.gpdm.entity.Classification
import org.springframework.data.repository.PagingAndSortingRepository

interface ClassificationRepository : PagingAndSortingRepository<Classification, Long> {
}