package com.catenax.gpdm.repository

import com.catenax.gpdm.entity.Site
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository

interface SiteRepository : PagingAndSortingRepository<Site, Long> {
    @Query("SELECT DISTINCT s FROM Site s LEFT JOIN FETCH s.addresses WHERE s IN :sites")
    fun joinAddresses(sites: Set<Site>): Set<Site>

    @Query("SELECT s FROM Site s join s.partner p where p.bpn=:bpn")
    fun findByPartnerBpn(bpn: String, pageable: Pageable): Page<Site>
}