package com.catenax.gpdm.util

import com.catenax.gpdm.dto.request.BusinessPartnerRequest
import com.catenax.gpdm.dto.request.NameRequest
import com.catenax.gpdm.dto.request.SiteRequest

object RequestValues {
    val siteRequest1 = SiteRequest(
        bpn = null,
        name = CommonValues.siteName1
    )

    val siteRequest2 = SiteRequest(
        bpn = null,
        name = CommonValues.siteName2
    )

    val businessPartnerRequest1 = BusinessPartnerRequest(
        bpn = null,
        legalForm = null,
        status = null,
        names = listOf(NameRequest(value = CommonValues.name6, shortName = null)),
        sites = listOf(siteRequest1)
    )

    val businessPartnerRequest2 = BusinessPartnerRequest(
        bpn = null,
        legalForm = null,
        status = null,
        names = listOf(NameRequest(value = CommonValues.name7, shortName = null)),
        sites = listOf(siteRequest2)
    )
}