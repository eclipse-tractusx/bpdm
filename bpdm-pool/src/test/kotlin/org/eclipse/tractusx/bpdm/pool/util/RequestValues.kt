package org.eclipse.tractusx.bpdm.pool.util

import com.neovisionaries.i18n.LanguageCode
import org.eclipse.tractusx.bpdm.common.dto.AddressDto
import org.eclipse.tractusx.bpdm.common.dto.NameDto
import org.eclipse.tractusx.bpdm.common.dto.PremiseDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameUrlDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeNameUrlDto
import org.eclipse.tractusx.bpdm.pool.dto.request.BusinessPartnerRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.LegalFormRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.SiteRequest

object RequestValues {

    val identifierType1 = TypeKeyNameUrlDto(CommonValues.identifierTypeTechnicalKey1, CommonValues.identiferTypeName1, CommonValues.identifierTypeUrl1)
    val identifierType2 = TypeKeyNameUrlDto(CommonValues.identifierTypeTechnicalKey2, CommonValues.identiferTypeName2, CommonValues.identifierTypeUrl2)
    val identifierType3 = TypeKeyNameUrlDto(CommonValues.identifierTypeTechnicalKey3, CommonValues.identiferTypeName3, CommonValues.identifierTypeUrl3)

    val identifierStatus1 = TypeKeyNameDto(CommonValues.identifierStatusKey1, CommonValues.identifierStatusName1)
    val identifierStatus2 = TypeKeyNameDto(CommonValues.identifierStatusKey2, CommonValues.identifierStatusName2)
    val identifierStatus3 = TypeKeyNameDto(CommonValues.identifierStatusKey3, CommonValues.identifierStatusName3)

    val issuingBody1 = TypeKeyNameUrlDto(CommonValues.issuingBodyKey1, CommonValues.issuingBodyName1, CommonValues.issuingBodyUrl1)
    val issuingBody2 = TypeKeyNameUrlDto(CommonValues.issuingBodyKey2, CommonValues.issuingBodyName2, CommonValues.issuingBodyUrl2)
    val issuingBody3 = TypeKeyNameUrlDto(CommonValues.issuingBodyKey3, CommonValues.issuingBodyName3, CommonValues.issuingBodyUrl3)

    val legalFormCategory1 = TypeNameUrlDto(CommonValues.legalFormCategoryName1, CommonValues.legalFormCategoryUrl1)
    val legalFormCategory2 = TypeNameUrlDto(CommonValues.legalFormCategoryName2, CommonValues.legalFormCategoryUrl2)
    val legalFormCategory3 = TypeNameUrlDto(CommonValues.legalFormCategoryName3, CommonValues.legalFormCategoryUrl3)

    val legalForm1 = LegalFormRequest(
        CommonValues.legalFormTechnicalKey1,
        CommonValues.legalFormName1,
        CommonValues.legalFormUrl1,
        CommonValues.legalFormAbbreviation1,
        LanguageCode.en
    )
    val legalForm2 = LegalFormRequest(
        CommonValues.legalFormTechnicalKey2,
        CommonValues.legalFormName2,
        CommonValues.legalFormUrl2,
        CommonValues.legalFormAbbreviation2,
        LanguageCode.de
    )
    val legalForm3 = LegalFormRequest(
        CommonValues.legalFormTechnicalKey3,
        CommonValues.legalFormName3,
        CommonValues.legalFormUrl3,
        CommonValues.legalFormAbbreviation3,
        LanguageCode.zh
    )


    val premiseRequest1 = PremiseDto(
        value = CommonValues.premise6
    )

    val addressRequest1 = AddressDto(
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
        names = listOf(NameDto(value = CommonValues.name6, shortName = null)),
        sites = listOf(siteRequest1)
    )

    val businessPartnerRequest2 = BusinessPartnerRequest(
        bpn = null,
        legalForm = null,
        status = null,
        names = listOf(NameDto(value = CommonValues.name7, shortName = null)),
        sites = listOf(siteRequest2)
    )

    val businessPartnerRequest3 = BusinessPartnerRequest(
        bpn = null,
        legalForm = null,
        status = null,
        names = listOf(NameDto(value = CommonValues.name8, shortName = null)),
        sites = listOf(siteRequest3)
    )
}