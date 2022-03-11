package com.catenax.gpdm.repository.entity

import com.catenax.gpdm.entity.Name
import org.springframework.data.repository.PagingAndSortingRepository

interface NameRepository : PagingAndSortingRepository<Name, Long> {
}