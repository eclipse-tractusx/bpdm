package com.catenax.gpdm.util

import com.catenax.gpdm.dto.response.*
import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.catenax.gpdm.dto.response.type.TypeKeyNameUrlDto
import com.catenax.gpdm.dto.response.type.TypeNameUrlDto
import com.catenax.gpdm.entity.*
import com.neovisionaries.i18n.CountryCode
import com.neovisionaries.i18n.LanguageCode
import java.time.ZoneOffset

/**
 * Test values for response DTOs
 * Numbered values should match with @see CdqValues numbered values for easier testing
 */
object ResponseValues {

    val nameType1 = TypeKeyNameUrlDto(NameType.OTHER, NameType.OTHER.getTypeName(), NameType.OTHER.getUrl())

    val language1 = TypeKeyNameDto(LanguageCode.undefined, LanguageCode.undefined.getName())

    val characterSet1 = TypeKeyNameDto(CharacterSet.UNDEFINED, CharacterSet.UNDEFINED.getTypeName())

    val country1 = TypeKeyNameDto(CountryCode.UNDEFINED, CountryCode.UNDEFINED.getName())

    val idTypeCdq = TypeKeyNameUrlDto("CDQID", "CDQ Identifier", "")
    val issuerCdq = TypeKeyNameUrlDto("CDQ", "CDQ AG", "")
    val statusCdq = TypeKeyNameDto("CDQ_IMPORTED", "Imported from CDQ but not synchronized")

    val idTypeBpn = TypeKeyNameUrlDto("BPN", "Business Partner Number", "")
    val issuerBpn = TypeKeyNameUrlDto("CATENAX", "Catena-X", "")
    val statusBpn = TypeKeyNameDto("UNKNOWN", "Unknown")


    val identifier1 = IdentifierResponse(CommonValues.uuid1, CdqValues.partnerId1, idTypeCdq, issuerCdq, statusCdq)
    val identifier2 = IdentifierResponse(CommonValues.uuid1, CommonValues.bpn1, idTypeBpn, issuerBpn, statusBpn)
    val identifier3 = IdentifierResponse(CommonValues.uuid1, CdqValues.partnerId2, idTypeCdq, issuerCdq, statusCdq)
    val identifier4 = IdentifierResponse(CommonValues.uuid1, CommonValues.bpn2, idTypeBpn, issuerBpn, statusBpn)
    val identifier5 = IdentifierResponse(CommonValues.uuid1, CdqValues.partnerId3, idTypeCdq, issuerCdq, statusCdq)
    val identifier6 = IdentifierResponse(CommonValues.uuid1, CommonValues.bpn3, idTypeBpn, issuerBpn, statusBpn)

    val name1 = NameResponse(uuid = CommonValues.uuid1, value = CommonValues.name1, type = nameType1, language = language1)
    val name2 = NameResponse(uuid = CommonValues.uuid1, value = CommonValues.name2, type = nameType1, language = language1)
    val name3 = NameResponse(uuid = CommonValues.uuid1, value = CommonValues.name3, type =  nameType1, language = language1)
    val name4 = NameResponse(uuid = CommonValues.uuid1, value = CommonValues.name4, type =  nameType1, language = language1)
    val name5 = NameResponse(uuid = CommonValues.uuid1, value = CommonValues.name5, type =  nameType1, language = language1)

    val legalFormDoc1 = LegalFormResponse(technicalKey = CommonValues.legalFormTechnicalKey1, name = CommonValues.legalFormName1, language = language1)
    val legalFormDoc2 = LegalFormResponse(technicalKey = CommonValues.legalFormTechnicalKey2, name = CommonValues.legalFormName2, language = language1)
    val legalFormDoc3 = LegalFormResponse(technicalKey = CommonValues.legalFormTechnicalKey3, name = CommonValues.legalFormName3, language = language1)

    val statusType1 = TypeKeyNameUrlDto(BusinessStatusType.ACTIVE, BusinessStatusType.ACTIVE.getTypeName(), BusinessStatusType.ACTIVE.getUrl())
    val statusType2 = TypeKeyNameUrlDto(BusinessStatusType.DISSOLVED, BusinessStatusType.DISSOLVED.getTypeName(), BusinessStatusType.DISSOLVED.getUrl())
    val statusType3 = TypeKeyNameUrlDto(BusinessStatusType.INSOLVENCY, BusinessStatusType.INSOLVENCY.getTypeName(), BusinessStatusType.INSOLVENCY.getUrl())

    val status1 = BusinessStatusResponse(CommonValues.uuid1, CommonValues.statusDenotation1, CommonValues.statusValidFrom1, null, statusType1)
    val status2 = BusinessStatusResponse(CommonValues.uuid1, CommonValues.statusDenotation2, CommonValues.statusValidFrom2, null, statusType2)
    val status3 = BusinessStatusResponse(CommonValues.uuid1, CommonValues.statusDenotation3, CommonValues.statusValidFrom3, null, statusType3)

    val classificationType = TypeNameUrlDto(CommonValues.classificationType, "")

    val classification1 = ClassificationResponse(CommonValues.uuid1, CommonValues.classification1, null, classificationType)
    val classification2 = ClassificationResponse(CommonValues.uuid1, CommonValues.classification2, null, classificationType)
    val classification3 = ClassificationResponse(CommonValues.uuid1, CommonValues.classification3, null, classificationType)
    val classification4 = ClassificationResponse(CommonValues.uuid1, CommonValues.classification4, null, classificationType)
    val classification5 = ClassificationResponse(CommonValues.uuid1, CommonValues.classification5, null, classificationType)

    val adminAreaType1 = TypeKeyNameUrlDto(AdministrativeAreaType.OTHER, AdministrativeAreaType.OTHER.getTypeName(), AdministrativeAreaType.OTHER.getUrl())

    val adminArea1 = AdministrativeAreaResponse(uuid = CommonValues.uuid1, value = CommonValues.adminArea1,  type = adminAreaType1, language = language1)
    val adminArea2 = AdministrativeAreaResponse(uuid = CommonValues.uuid1, value = CommonValues.adminArea2, type = adminAreaType1, language = language1)
    val adminArea3 = AdministrativeAreaResponse(uuid = CommonValues.uuid1, value = CommonValues.adminArea3, type = adminAreaType1, language = language1)
    val adminArea4 = AdministrativeAreaResponse(uuid = CommonValues.uuid1, value = CommonValues.adminArea4, type = adminAreaType1, language = language1)
    val adminArea5 = AdministrativeAreaResponse(uuid = CommonValues.uuid1, value = CommonValues.adminArea5, type = adminAreaType1, language = language1)

    val postCodeType1 = TypeKeyNameUrlDto(PostCodeType.OTHER, PostCodeType.OTHER.getTypeName(), PostCodeType.OTHER.getUrl())

    val postCode1 = PostCodeResponse(CommonValues.uuid1,CommonValues.postCode1, postCodeType1)
    val postCode2 = PostCodeResponse(CommonValues.uuid1,CommonValues.postCode2, postCodeType1)
    val postCode3 = PostCodeResponse(CommonValues.uuid1,CommonValues.postCode3, postCodeType1)
    val postCode4 = PostCodeResponse(CommonValues.uuid1,CommonValues.postCode4, postCodeType1)
    val postCode5 = PostCodeResponse(CommonValues.uuid1,CommonValues.postCode5, postCodeType1)

    val localityType1 = TypeKeyNameUrlDto(LocalityType.OTHER, LocalityType.OTHER.getTypeName(), LocalityType.OTHER.getUrl())

    val locality1 = LocalityResponse(CommonValues.uuid1, CommonValues.locality1, null, localityType1, language1)
    val locality2 = LocalityResponse(CommonValues.uuid1, CommonValues.locality2, null, localityType1, language1)
    val locality3 = LocalityResponse(CommonValues.uuid1, CommonValues.locality3, null, localityType1, language1)
    val locality4 = LocalityResponse(CommonValues.uuid1, CommonValues.locality4, null, localityType1, language1)
    val locality5 = LocalityResponse(CommonValues.uuid1, CommonValues.locality5, null, localityType1, language1)

    val thoroughfareType1 = TypeKeyNameUrlDto(ThoroughfareType.OTHER, ThoroughfareType.OTHER.getTypeName(), ThoroughfareType.OTHER.getUrl())

    val thoroughfare1 = ThoroughfareResponse(uuid = CommonValues.uuid1, value = CommonValues.thoroughfare1, type = thoroughfareType1, language =  language1)
    val thoroughfare2 = ThoroughfareResponse(uuid = CommonValues.uuid1, value = CommonValues.thoroughfare2, type = thoroughfareType1, language = language1)
    val thoroughfare3 = ThoroughfareResponse(uuid = CommonValues.uuid1, value = CommonValues.thoroughfare3, type = thoroughfareType1, language = language1)
    val thoroughfare4 = ThoroughfareResponse(uuid = CommonValues.uuid1, value = CommonValues.thoroughfare4, type = thoroughfareType1, language = language1)
    val thoroughfare5 = ThoroughfareResponse(uuid = CommonValues.uuid1, value = CommonValues.thoroughfare5, type = thoroughfareType1, language = language1)

    val premiseType1 = TypeKeyNameUrlDto(PremiseType.OTHER, PremiseType.OTHER.getTypeName(), PremiseType.OTHER.getUrl())

    val premise1 = PremiseResponse(uuid = CommonValues.uuid1, value = CommonValues.premise1, type = premiseType1, language = language1)
    val premise2 = PremiseResponse(uuid = CommonValues.uuid1, value = CommonValues.premise2, type = premiseType1, language = language1)
    val premise3 = PremiseResponse(uuid = CommonValues.uuid1, value = CommonValues.premise3, type = premiseType1, language = language1)
    val premise4 = PremiseResponse(uuid = CommonValues.uuid1, value = CommonValues.premise4, type = premiseType1, language = language1)
    val premise5 = PremiseResponse(uuid = CommonValues.uuid1, value = CommonValues.premise5, type = premiseType1, language = language1)

    val postalDeliveryPointType1 = TypeKeyNameUrlDto(PostalDeliveryPointType.OTHER, PostalDeliveryPointType.OTHER.getTypeName(), PostalDeliveryPointType.OTHER.getUrl())

    val postalDeliveryPoint1 = PostalDeliveryPointResponse(uuid = CommonValues.uuid1, value = CommonValues.postalDeliveryPoint1, type = postalDeliveryPointType1, language = language1)
    val postalDeliveryPoint2 = PostalDeliveryPointResponse(uuid = CommonValues.uuid1, value = CommonValues.postalDeliveryPoint2, type = postalDeliveryPointType1, language = language1)
    val postalDeliveryPoint3 = PostalDeliveryPointResponse(uuid = CommonValues.uuid1, value = CommonValues.postalDeliveryPoint3, type = postalDeliveryPointType1, language = language1)
    val postalDeliveryPoint4 = PostalDeliveryPointResponse(uuid = CommonValues.uuid1, value = CommonValues.postalDeliveryPoint4, type = postalDeliveryPointType1, language = language1)
    val postalDeliveryPoint5 = PostalDeliveryPointResponse(uuid = CommonValues.uuid1, value = CommonValues.postalDeliveryPoint5, type = postalDeliveryPointType1, language = language1)

    val version1 = AddressVersionResponse(characterSet1, language1)

    val address1 = AddressResponse(
        uuid = CommonValues.uuid1,
        bpn = CommonValues.bpnA1,
        name = CommonValues.addressName1,
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
        uuid = CommonValues.uuid1,
        bpn = CommonValues.bpnA2,
        name = CommonValues.addressName2,
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
        uuid = CommonValues.uuid1,
        bpn = CommonValues.bpnA3,
        name = CommonValues.addressName3,
        version = version1,
        country = country1,
        administrativeAreas = listOf(adminArea5),
        postCodes = listOf(postCode5),
        localities = listOf(locality5),
        thoroughfares = listOf(thoroughfare5),
        premises = listOf(premise5),
        postalDeliveryPoints = listOf(postalDeliveryPoint5)
    )

    val businessPartner1 = BusinessPartnerResponse(
        bpn = CommonValues.bpn1,
        identifiers = listOf(identifier1, identifier2),
        names = listOf(name1, name2),
        legalForm = legalFormDoc1,
        status = status1,
        profileClassifications = listOf(classification1, classification2),
        addresses = listOf(address1),
        currentness = CdqValues.createdTime1.toInstant(ZoneOffset.UTC)
    )

    val businessPartner2 = BusinessPartnerResponse(
        bpn = CommonValues.bpn2,
        identifiers = listOf(identifier3, identifier4),
        names = listOf(name3, name4),
        legalForm = legalFormDoc2,
        status = status2,
        profileClassifications = listOf(classification3, classification4),
        addresses = listOf(address2),
        currentness = CdqValues.createdTime1.toInstant(ZoneOffset.UTC)
    )

    val businessPartner3 = BusinessPartnerResponse(
        bpn = CommonValues.bpn3,
        identifiers = listOf(identifier5, identifier6),
        names = listOf(name5),
        legalForm = legalFormDoc3,
        status = status3,
        profileClassifications = listOf(classification5),
        addresses = listOf(address3),
        currentness = CdqValues.createdTime1.toInstant(ZoneOffset.UTC)
    )
}