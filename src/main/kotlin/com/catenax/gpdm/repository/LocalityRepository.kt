package com.catenax.gpdm.repository.entity

import com.catenax.gpdm.entity.Locality
import org.springframework.data.repository.PagingAndSortingRepository

interface LocalityRepository : PagingAndSortingRepository<Locality, Long> {
}