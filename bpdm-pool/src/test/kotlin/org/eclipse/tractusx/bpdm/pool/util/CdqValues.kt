package org.eclipse.tractusx.bpdm.pool.util

import org.eclipse.tractusx.bpdm.pool.component.cdq.dto.*
import com.neovisionaries.i18n.LanguageCode
import java.time.LocalDateTime

/**
 * Test values for CDQ DTOs
 * Numbered values should match with @see ResponseValues numbered values for easier testing
 */
object CdqValues {

    val partnerId1 = "cdq-1"
    val partnerId2 = "cdq-2"
    val partnerId3 = "cdq-3"

    val datasource1 = "datasource-1"

    val createdTime1 = LocalDateTime.of(2020, 1, 1, 1, 1)

    val language1 = LanguageCdq(LanguageCode.en, LanguageCode.en.getName())
    val language2 = LanguageCdq(LanguageCode.de, LanguageCode.de.getName())
    val language3 = LanguageCdq(LanguageCode.zh, LanguageCode.zh.getName())

    val name1 = NameCdq(CommonValues.name1)
    val name2 = NameCdq(CommonValues.name2)
    val name3 = NameCdq(CommonValues.name3)
    val name4 = NameCdq(CommonValues.name4)
    val name5 = NameCdq(CommonValues.name5)

    val legalForm1 = LegalFormCdq(
        CommonValues.legalFormName1,
        CommonValues.legalFormUrl1,
        CommonValues.legalFormTechnicalKey1,
        CommonValues.legalFormAbbreviation1,
        language1
    )
    val legalForm2 = LegalFormCdq(
        CommonValues.legalFormName2,
        CommonValues.legalFormUrl2,
        CommonValues.legalFormTechnicalKey2,
        CommonValues.legalFormAbbreviation2,
        language2
    )
    val legalForm3 = LegalFormCdq(
        CommonValues.legalFormName3,
        CommonValues.legalFormUrl3,
        CommonValues.legalFormTechnicalKey3,
        CommonValues.legalFormAbbreviation3,
        language3
    )

    val statusType1 = TypeKeyNameUrlCdq(CommonValues.statusType1)
    val statusType2 = TypeKeyNameUrlCdq(CommonValues.statusType2)
    val statusType3 = TypeKeyNameUrlCdq(CommonValues.statusType3)

    val status1 = BusinessPartnerStatusCdq(statusType1, CommonValues.statusDenotation1, CommonValues.statusValidFrom1)
    val status2 = BusinessPartnerStatusCdq(statusType2, CommonValues.statusDenotation2, CommonValues.statusValidFrom2)
    val status3 = BusinessPartnerStatusCdq(statusType3, CommonValues.statusDenotation3, CommonValues.statusValidFrom3)

    val classificationType = TypeKeyNameUrlCdq(CommonValues.classificationType)

    val classification1 = ClassificationCdq(value = CommonValues.classification1, type = classificationType)
    val classification2 = ClassificationCdq(value = CommonValues.classification2, type = classificationType)
    val classification3 = ClassificationCdq(value = CommonValues.classification3, type = classificationType)
    val classification4 = ClassificationCdq(value = CommonValues.classification4, type = classificationType)
    val classification5 = ClassificationCdq(value = CommonValues.classification5, type = classificationType)

    val profile1 = PartnerProfileCdq(classifications = listOf(classification1, classification2))
    val profile2 = PartnerProfileCdq(classifications = listOf(classification3, classification4))
    val profile3 = PartnerProfileCdq(classifications = listOf(classification5))

    val addressId1 = "address-cdq-1"
    val addressId2 = "address-cdq-2"
    val addressId3 = "address-cdq-3"

    val adminArea1 = AdministrativeAreaCdq(CommonValues.adminArea1)
    val adminArea2 = AdministrativeAreaCdq(CommonValues.adminArea2)
    val adminArea3 = AdministrativeAreaCdq(CommonValues.adminArea3)
    val adminArea4 = AdministrativeAreaCdq(CommonValues.adminArea4)
    val adminArea5 = AdministrativeAreaCdq(CommonValues.adminArea5)

    val postCode1 = PostCodeCdq(CommonValues.postCode1)
    val postCode2 = PostCodeCdq(CommonValues.postCode2)
    val postCode3 = PostCodeCdq(CommonValues.postCode3)
    val postCode4 = PostCodeCdq(CommonValues.postCode4)
    val postCode5 = PostCodeCdq(CommonValues.postCode5)

    val locality1 = LocalityCdq(value = CommonValues.locality1)
    val locality2 = LocalityCdq(value = CommonValues.locality2)
    val locality3 = LocalityCdq(value = CommonValues.locality3)
    val locality4 = LocalityCdq(value = CommonValues.locality4)
    val locality5 = LocalityCdq(value = CommonValues.locality5)

    val thoroughfare1 = ThoroughfareCdq(value = CommonValues.thoroughfare1)
    val thoroughfare2 = ThoroughfareCdq(value = CommonValues.thoroughfare2)
    val thoroughfare3 = ThoroughfareCdq(value = CommonValues.thoroughfare3)
    val thoroughfare4 = ThoroughfareCdq(value = CommonValues.thoroughfare4)
    val thoroughfare5 = ThoroughfareCdq(value = CommonValues.thoroughfare5)

    val premise1 = PremiseCdq(value = CommonValues.premise1)
    val premise2 = PremiseCdq(value = CommonValues.premise2)
    val premise3 = PremiseCdq(value = CommonValues.premise3)
    val premise4 = PremiseCdq(value = CommonValues.premise4)
    val premise5 = PremiseCdq(value = CommonValues.premise5)

    val postalDeliveryPoint1 = PostalDeliveryPointCdq(value = CommonValues.postalDeliveryPoint1)
    val postalDeliveryPoint2 = PostalDeliveryPointCdq(value = CommonValues.postalDeliveryPoint2)
    val postalDeliveryPoint3 = PostalDeliveryPointCdq(value = CommonValues.postalDeliveryPoint3)
    val postalDeliveryPoint4 = PostalDeliveryPointCdq(value = CommonValues.postalDeliveryPoint4)
    val postalDeliveryPoint5 = PostalDeliveryPointCdq(value = CommonValues.postalDeliveryPoint5)

    val address1 = AddressCdq(
        id = addressId1,
        externalId = addressId1,
        cdqId = addressId1,
        administrativeAreas = listOf(adminArea1, adminArea2),
        postCodes = listOf(postCode1, postCode2),
        localities = listOf(locality1, locality2),
        thoroughfares = listOf(thoroughfare1, thoroughfare2),
        premises = listOf(premise1, premise2),
        postalDeliveryPoints = listOf(postalDeliveryPoint1, postalDeliveryPoint2)
    )

    val address2 = AddressCdq(
        id = addressId2,
        externalId = addressId2,
        cdqId = addressId2,
        administrativeAreas = listOf(adminArea3, adminArea4),
        postCodes = listOf(postCode3, postCode4),
        localities = listOf(locality3, locality4),
        thoroughfares = listOf(thoroughfare3, thoroughfare4),
        premises = listOf(premise3, premise4),
        postalDeliveryPoints = listOf(postalDeliveryPoint3, postalDeliveryPoint4)
    )

    val address3 = AddressCdq(
        id = addressId3,
        externalId = addressId3,
        cdqId = addressId3,
        administrativeAreas = listOf(adminArea5),
        postCodes = listOf(postCode5),
        localities = listOf(locality5),
        thoroughfares = listOf(thoroughfare5),
        premises = listOf(premise5),
        postalDeliveryPoints = listOf(postalDeliveryPoint5)
    )


    val businessPartner1 = BusinessPartnerCdq(
        id = partnerId1,
        createdAt = createdTime1,
        lastModifiedAt = createdTime1,
        externalId = partnerId1,
        dataSource = datasource1,
        names = listOf(name1, name2),
        legalForm = legalForm1,
        status = status1,
        profile = profile1,
        addresses = listOf(address1)
    )

    val businessPartner2 = BusinessPartnerCdq(
        id = partnerId2,
        createdAt = createdTime1,
        lastModifiedAt = createdTime1,
        externalId = partnerId2,
        dataSource = datasource1,
        names = listOf(name3, name4),
        legalForm = legalForm2,
        status = status2,
        profile = profile2,
        addresses = listOf(address2)
    )

    val businessPartner3 = BusinessPartnerCdq(
        id = partnerId3,
        createdAt = createdTime1,
        lastModifiedAt = createdTime1,
        externalId = partnerId3,
        dataSource = datasource1,
        names = listOf(name5),
        legalForm = legalForm3,
        status = status3,
        profile = profile3,
        addresses = listOf(address3)
    )
}