package org.eclipse.tractusx.bpdm.pool.util

import com.neovisionaries.i18n.CountryCode
import com.neovisionaries.i18n.LanguageCode
import org.eclipse.tractusx.bpdm.common.dto.response.*
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameUrlDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeNameUrlDto
import org.eclipse.tractusx.bpdm.common.model.*
import org.eclipse.tractusx.bpdm.pool.dto.response.BusinessPartnerResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.SiteResponse
import java.time.ZoneOffset

/**
 * Test values for response DTOs
 * Numbered values should match with @see CdqValues numbered values for easier testing
 */
object ResponseValues {

    val nameType1 = TypeKeyNameUrlDto(NameType.OTHER, NameType.OTHER.getTypeName(), NameType.OTHER.getUrl())

    val language0 = TypeKeyNameDto(LanguageCode.undefined, LanguageCode.undefined.getName())
    val language1 = TypeKeyNameDto(LanguageCode.en, LanguageCode.en.getName())
    val language2 = TypeKeyNameDto(LanguageCode.de, LanguageCode.de.getName())
    val language3 = TypeKeyNameDto(LanguageCode.zh, LanguageCode.zh.getName())

    val characterSet1 = TypeKeyNameDto(CharacterSet.UNDEFINED, CharacterSet.UNDEFINED.getTypeName())

    val country1 = TypeKeyNameDto(CountryCode.UNDEFINED, CountryCode.UNDEFINED.getName())

    val idTypeCdq = TypeKeyNameUrlDto("CDQID", "CDQ Identifier", "")
    val issuerCdq = TypeKeyNameUrlDto("CDQ", "CDQ AG", "")
    val statusCdq = TypeKeyNameDto("CDQ_IMPORTED", "Imported from CDQ but not synchronized")

    val idTypeBpn = TypeKeyNameUrlDto("BPN", "Business Partner Number", "")
    val issuerBpn = TypeKeyNameUrlDto("CATENAX", "Catena-X", "")
    val statusBpn = TypeKeyNameDto("UNKNOWN", "Unknown")


    val identifier1 = IdentifierResponse(CdqValues.partnerId1, idTypeCdq, issuerCdq, statusCdq)
    val identifier2 = IdentifierResponse(CommonValues.bpn1, idTypeBpn, issuerBpn, statusBpn)
    val identifier3 = IdentifierResponse(CdqValues.partnerId2, idTypeCdq, issuerCdq, statusCdq)
    val identifier4 = IdentifierResponse(CommonValues.bpn2, idTypeBpn, issuerBpn, statusBpn)
    val identifier5 = IdentifierResponse(CdqValues.partnerId3, idTypeCdq, issuerCdq, statusCdq)
    val identifier6 = IdentifierResponse(CommonValues.bpn3, idTypeBpn, issuerBpn, statusBpn)

    val name1 = NameResponse(value = CommonValues.name1, type = nameType1, language = language0)
    val name2 = NameResponse(value = CommonValues.name2, type = nameType1, language = language0)
    val name3 = NameResponse(value = CommonValues.name3, type = nameType1, language = language0)
    val name4 = NameResponse(value = CommonValues.name4, type = nameType1, language = language0)
    val name5 = NameResponse(value = CommonValues.name5, type = nameType1, language = language0)

    val legalForm1 = LegalFormResponse(
        CommonValues.legalFormTechnicalKey1,
        CommonValues.legalFormName1,
        CommonValues.legalFormUrl1,
        CommonValues.legalFormAbbreviation1,
        language1
    )
    val legalForm2 = LegalFormResponse(
        CommonValues.legalFormTechnicalKey2,
        CommonValues.legalFormName2,
        CommonValues.legalFormUrl2,
        CommonValues.legalFormAbbreviation2,
        language2
    )
    val legalForm3 = LegalFormResponse(
        CommonValues.legalFormTechnicalKey3,
        CommonValues.legalFormName3,
        CommonValues.legalFormUrl3,
        CommonValues.legalFormAbbreviation3,
        language3
    )

    val statusType1 = TypeKeyNameUrlDto(BusinessStatusType.ACTIVE, BusinessStatusType.ACTIVE.getTypeName(), BusinessStatusType.ACTIVE.getUrl())
    val statusType2 = TypeKeyNameUrlDto(BusinessStatusType.DISSOLVED, BusinessStatusType.DISSOLVED.getTypeName(), BusinessStatusType.DISSOLVED.getUrl())
    val statusType3 = TypeKeyNameUrlDto(BusinessStatusType.INSOLVENCY, BusinessStatusType.INSOLVENCY.getTypeName(), BusinessStatusType.INSOLVENCY.getUrl())

    val status1 = BusinessStatusResponse(CommonValues.statusDenotation1, CommonValues.statusValidFrom1, null, statusType1)
    val status2 = BusinessStatusResponse(CommonValues.statusDenotation2, CommonValues.statusValidFrom2, null, statusType2)
    val status3 = BusinessStatusResponse(CommonValues.statusDenotation3, CommonValues.statusValidFrom3, null, statusType3)

    val classificationType = TypeNameUrlDto(CommonValues.classificationType, "")

    val classification1 = ClassificationResponse(CommonValues.classification1, null, classificationType)
    val classification2 = ClassificationResponse(CommonValues.classification2, null, classificationType)
    val classification3 = ClassificationResponse(CommonValues.classification3, null, classificationType)
    val classification4 = ClassificationResponse(CommonValues.classification4, null, classificationType)
    val classification5 = ClassificationResponse(CommonValues.classification5, null, classificationType)

    val adminAreaType1 = TypeKeyNameUrlDto(AdministrativeAreaType.OTHER, AdministrativeAreaType.OTHER.getTypeName(), AdministrativeAreaType.OTHER.getUrl())

    val adminArea1 = AdministrativeAreaResponse(value = CommonValues.adminArea1, type = adminAreaType1, language = language0)
    val adminArea2 = AdministrativeAreaResponse(value = CommonValues.adminArea2, type = adminAreaType1, language = language0)
    val adminArea3 = AdministrativeAreaResponse(value = CommonValues.adminArea3, type = adminAreaType1, language = language0)
    val adminArea4 = AdministrativeAreaResponse(value = CommonValues.adminArea4, type = adminAreaType1, language = language0)
    val adminArea5 = AdministrativeAreaResponse(value = CommonValues.adminArea5, type = adminAreaType1, language = language0)

    val postCodeType1 = TypeKeyNameUrlDto(PostCodeType.OTHER, PostCodeType.OTHER.getTypeName(), PostCodeType.OTHER.getUrl())

    val postCode1 = PostCodeResponse(CommonValues.postCode1, postCodeType1)
    val postCode2 = PostCodeResponse(CommonValues.postCode2, postCodeType1)
    val postCode3 = PostCodeResponse(CommonValues.postCode3, postCodeType1)
    val postCode4 = PostCodeResponse(CommonValues.postCode4, postCodeType1)
    val postCode5 = PostCodeResponse(CommonValues.postCode5, postCodeType1)

    val localityType1 = TypeKeyNameUrlDto(LocalityType.OTHER, LocalityType.OTHER.getTypeName(), LocalityType.OTHER.getUrl())

    val locality1 = LocalityResponse(CommonValues.locality1, null, localityType1, language0)
    val locality2 = LocalityResponse(CommonValues.locality2, null, localityType1, language0)
    val locality3 = LocalityResponse(CommonValues.locality3, null, localityType1, language0)
    val locality4 = LocalityResponse(CommonValues.locality4, null, localityType1, language0)
    val locality5 = LocalityResponse(CommonValues.locality5, null, localityType1, language0)

    val thoroughfareType1 = TypeKeyNameUrlDto(ThoroughfareType.OTHER, ThoroughfareType.OTHER.getTypeName(), ThoroughfareType.OTHER.getUrl())

    val thoroughfare1 = ThoroughfareResponse(value = CommonValues.thoroughfare1, type = thoroughfareType1, language = language0)
    val thoroughfare2 = ThoroughfareResponse(value = CommonValues.thoroughfare2, type = thoroughfareType1, language = language0)
    val thoroughfare3 = ThoroughfareResponse(value = CommonValues.thoroughfare3, type = thoroughfareType1, language = language0)
    val thoroughfare4 = ThoroughfareResponse(value = CommonValues.thoroughfare4, type = thoroughfareType1, language = language0)
    val thoroughfare5 = ThoroughfareResponse(value = CommonValues.thoroughfare5, type = thoroughfareType1, language = language0)

    val premiseType1 = TypeKeyNameUrlDto(PremiseType.OTHER, PremiseType.OTHER.getTypeName(), PremiseType.OTHER.getUrl())

    val premise1 = PremiseResponse(value = CommonValues.premise1, type = premiseType1, language = language0)
    val premise2 = PremiseResponse(value = CommonValues.premise2, type = premiseType1, language = language0)
    val premise3 = PremiseResponse(value = CommonValues.premise3, type = premiseType1, language = language0)
    val premise4 = PremiseResponse(value = CommonValues.premise4, type = premiseType1, language = language0)
    val premise5 = PremiseResponse(value = CommonValues.premise5, type = premiseType1, language = language0)
    val premise6 = PremiseResponse(value = CommonValues.premise6, type = premiseType1, language = language0)

    val postalDeliveryPointType1 =
        TypeKeyNameUrlDto(PostalDeliveryPointType.OTHER, PostalDeliveryPointType.OTHER.getTypeName(), PostalDeliveryPointType.OTHER.getUrl())

    val postalDeliveryPoint1 =
        PostalDeliveryPointResponse(value = CommonValues.postalDeliveryPoint1, type = postalDeliveryPointType1, language = language0)
    val postalDeliveryPoint2 =
        PostalDeliveryPointResponse(value = CommonValues.postalDeliveryPoint2, type = postalDeliveryPointType1, language = language0)
    val postalDeliveryPoint3 =
        PostalDeliveryPointResponse(value = CommonValues.postalDeliveryPoint3, type = postalDeliveryPointType1, language = language0)
    val postalDeliveryPoint4 =
        PostalDeliveryPointResponse(value = CommonValues.postalDeliveryPoint4, type = postalDeliveryPointType1, language = language0)
    val postalDeliveryPoint5 =
        PostalDeliveryPointResponse(value = CommonValues.postalDeliveryPoint5, type = postalDeliveryPointType1, language = language0)

    val version1 = AddressVersionResponse(characterSet1, language0)

    val address1 = AddressResponse(
        bpn = CommonValues.bpnA1,
        version = version1,
        country = country1,
        administrativeAreas = listOf(adminArea1, adminArea2),
        postCodes = listOf(postCode1, postCode2),
        localities = listOf(locality1, locality2),
        thoroughfares = listOf(thoroughfare1, thoroughfare2),
        premises = listOf(premise1, premise2),
        postalDeliveryPoints = listOf(postalDeliveryPoint1, postalDeliveryPoint2)
    )

    val address2 = AddressResponse(
        bpn = CommonValues.bpnA2,
        version = version1,
        country = country1,
        administrativeAreas = listOf(adminArea3, adminArea4),
        postCodes = listOf(postCode3, postCode4),
        localities = listOf(locality3, locality4),
        thoroughfares = listOf(thoroughfare3, thoroughfare4),
        premises = listOf(premise3, premise4),
        postalDeliveryPoints = listOf(postalDeliveryPoint3, postalDeliveryPoint4)
    )

    val address3 = AddressResponse(
        bpn = CommonValues.bpnA3,
        version = version1,
        country = country1,
        administrativeAreas = listOf(adminArea5),
        postCodes = listOf(postCode5),
        localities = listOf(locality5),
        thoroughfares = listOf(thoroughfare5),
        premises = listOf(premise5),
        postalDeliveryPoints = listOf(postalDeliveryPoint5)
    )

    val address4 = AddressResponse(
        bpn = CommonValues.bpnA3,
        version = version1,
        country = country1,
        premises = listOf(premise6)
    )

    val site1 = SiteResponse(
        CommonValues.bpnS1,
        CommonValues.siteName1,
        listOf(address4)
    )

    val site2 = SiteResponse(
        CommonValues.bpnS2,
        CommonValues.siteName2,
    )

    val businessPartner1 = BusinessPartnerResponse(
        bpn = CommonValues.bpn1,
        identifiers = listOf(identifier1, identifier2),
        names = listOf(name1, name2),
        legalForm = legalForm1,
        status = status1,
        profileClassifications = listOf(classification1, classification2),
        currentness = CdqValues.createdTime1.toInstant(ZoneOffset.UTC)
    )

    val businessPartner2 = BusinessPartnerResponse(
        bpn = CommonValues.bpn2,
        identifiers = listOf(identifier3, identifier4),
        names = listOf(name3, name4),
        legalForm = legalForm2,
        status = status2,
        profileClassifications = listOf(classification3, classification4),
        currentness = CdqValues.createdTime1.toInstant(ZoneOffset.UTC)
    )

    val businessPartner3 = BusinessPartnerResponse(
        bpn = CommonValues.bpn3,
        identifiers = listOf(identifier5, identifier6),
        names = listOf(name5),
        legalForm = legalForm3,
        status = status3,
        profileClassifications = listOf(classification5),
        currentness = CdqValues.createdTime1.toInstant(ZoneOffset.UTC)
    )


}