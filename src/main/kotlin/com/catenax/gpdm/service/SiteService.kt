package com.catenax.gpdm.service

import com.catenax.gpdm.dto.response.PageResponse
import com.catenax.gpdm.dto.response.SiteLegalEntityResponse
import com.catenax.gpdm.dto.response.SiteResponse
import com.catenax.gpdm.entity.Site
import com.catenax.gpdm.exception.BpdmNotFoundException
import com.catenax.gpdm.repository.BusinessPartnerRepository
import com.catenax.gpdm.repository.SiteRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class SiteService(
    private val siteRepository: SiteRepository,
    private val businessPartnerRepository: BusinessPartnerRepository
) {
    fun findByPartnerBpn(bpn: String, pageIndex: Int, pageSize: Int): PageResponse<SiteResponse> {
        if (!businessPartnerRepository.existsByBpn(bpn)) {
            throw BpdmNotFoundException("Business Partner", bpn)
        }

        val page = siteRepository.findByPartnerBpn(bpn, PageRequest.of(pageIndex, pageSize))
        fetchSiteDependencies(page.toSet())
        return page.toDto(page.content.map { it.toDto() })
    }

    fun findByBpn(bpn: String): SiteLegalEntityResponse {
        val site = siteRepository.findByBpn(bpn) ?: throw BpdmNotFoundException("Site", bpn)
        return site.toDtoWithLegalEntity()
    }

    private fun fetchSiteDependencies(sites: Set<Site>) {
        siteRepository.joinAddresses(sites)
    }
}