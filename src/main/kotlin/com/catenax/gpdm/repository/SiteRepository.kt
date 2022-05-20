package com.catenax.gpdm.repository

import com.catenax.gpdm.entity.Site
import org.springframework.data.repository.PagingAndSortingRepository

interface SiteRepository : PagingAndSortingRepository<Site, Long>