package com.catenax.gpdm.repository

import com.catenax.gpdm.entity.Premise
import org.springframework.data.repository.PagingAndSortingRepository

interface PremiseRepository : PagingAndSortingRepository<Premise, Long> {
}