package com.catenax.gpdm.repository

import com.catenax.gpdm.entity.BusinessPartner
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository

interface BusinessPartnerRepository : PagingAndSortingRepository<BusinessPartner, Long>{
    fun findByBpn(bpn: String) : BusinessPartner?
    @Query("SELECT DISTINCT i.partner FROM Identifier i WHERE i.type.technicalKey = ?1 AND i.value in ?2")
    fun findByIdentifiersContains(type: String, values: Collection<String>) : Set<BusinessPartner>
}