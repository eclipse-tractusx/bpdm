package com.catenax.gpdm.repository.entity

import com.catenax.gpdm.entity.Thoroughfare
import org.springframework.data.repository.PagingAndSortingRepository

interface ThoroughfareRepository : PagingAndSortingRepository<Thoroughfare, Long> {
}