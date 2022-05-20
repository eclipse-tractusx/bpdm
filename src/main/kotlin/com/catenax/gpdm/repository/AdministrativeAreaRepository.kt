package com.catenax.gpdm.repository

import com.catenax.gpdm.entity.AdministrativeArea
import org.springframework.data.repository.PagingAndSortingRepository

interface AdministrativeAreaRepository : PagingAndSortingRepository<AdministrativeArea, Long> {
}