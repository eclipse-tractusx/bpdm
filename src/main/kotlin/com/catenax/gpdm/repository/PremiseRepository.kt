package com.catenax.gpdm.repository.entity

import com.catenax.gpdm.entity.Premise
import org.springframework.data.repository.PagingAndSortingRepository

interface PremiseRepository : PagingAndSortingRepository<Premise, Long> {
}