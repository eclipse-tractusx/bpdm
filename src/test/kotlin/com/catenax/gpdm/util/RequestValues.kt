package com.catenax.gpdm.util

import com.catenax.gpdm.dto.request.*

object RequestValues {
    val premiseRequest1 = PremiseRequest(
        value = CommonValues.premise6
    )

    val addressRequest1 = AddressRequest(
        bpn = null,
        premises = listOf(premiseRequest1)
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
        sites = listOf(siteRequest2)
    )

    val businessPartnerRequest3 = BusinessPartnerRequest(
        bpn = null,
        legalForm = null,
        status = null,
        names = listOf(NameRequest(value = CommonValues.name8, shortName = null)),
        sites = listOf(siteRequest3)
    )
}