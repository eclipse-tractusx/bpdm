package com.catenax.gpdm.repository

import com.catenax.gpdm.entity.BusinessPartner
import org.springframework.data.repository.PagingAndSortingRepository
import java.util.*

interface BusinessPartnerRepository : PagingAndSortingRepository<BusinessPartner, Long>{
    fun findByBpn(bpn: String) : BusinessPartner?
}