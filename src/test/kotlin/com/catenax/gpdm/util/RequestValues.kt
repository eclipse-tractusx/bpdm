package com.catenax.gpdm.util

import com.catenax.gpdm.dto.request.AddressRequest
import com.catenax.gpdm.dto.request.BusinessPartnerRequest
import com.catenax.gpdm.dto.request.NameRequest
import com.catenax.gpdm.dto.request.SiteRequest

object RequestValues {
    val addressRequest1 = AddressRequest(
        bpn = null,
        name = CommonValues.addressName1
    )

    val addressRequest2 = AddressRequest(
        bpn = null,
        name = CommonValues.addressName2
    )

    val addressRequest3 = AddressRequest(
        bpn = null,
        name = CommonValues.addressName3
    )

    val siteRequest1 = SiteRequest(
        bpn = null,
        name = CommonValues.siteName1,
        addresses = listOf(addressRequest1)
    )

    val siteRequest2 = SiteRequest(
        bpn = null,
        name = CommonValues.siteName2
    )

    val siteRequest3 = SiteRequest(
        bpn = null,
        name = CommonValues.siteName3
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
        sites = listOf(siteRequest2),
        addresses = listOf(addressRequest2)
    )

    val businessPartnerRequest3 = BusinessPartnerRequest(
        bpn = null,
        legalForm = null,
        status = null,
        names = listOf(NameRequest(value = CommonValues.name8, shortName = null)),
        sites = listOf(siteRequest3),
        addresses = listOf(addressRequest3)
    )
}