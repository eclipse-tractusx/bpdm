package com.catenax.gpdm.repository

import com.catenax.gpdm.entity.Site
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository

interface SiteRepository : PagingAndSortingRepository<Site, Long> {
    @Query("SELECT DISTINCT s FROM Site s LEFT JOIN FETCH s.addresses WHERE s IN :sites")
    fun joinAddresses(sites: Set<Site>): Set<Site>
}