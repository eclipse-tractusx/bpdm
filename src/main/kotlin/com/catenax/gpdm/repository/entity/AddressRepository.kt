package com.catenax.gpdm.repository.entity

import com.catenax.gpdm.entity.Address
import org.springframework.data.repository.PagingAndSortingRepository

interface AddressRepository : PagingAndSortingRepository<Address, Long> {
}