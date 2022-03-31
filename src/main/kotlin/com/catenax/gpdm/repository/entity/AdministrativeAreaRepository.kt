package com.catenax.gpdm.repository.entity

import com.catenax.gpdm.entity.AdministrativeArea
import org.springframework.data.repository.PagingAndSortingRepository

interface AdministrativeAreaRepository : PagingAndSortingRepository<AdministrativeArea, Long> {
}