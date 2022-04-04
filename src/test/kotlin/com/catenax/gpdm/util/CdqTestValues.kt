package com.catenax.gpdm.util

import com.catenax.gpdm.component.cdq.dto.*
import java.time.LocalDateTime

object CdqTestValues {

    val partnerId1 = "cdq-1"
    val partnerId2 = "cdq-2"
    val partnerId3 = "cdq-3"

    val datasource1 = "datasource-1"

    val name1 = NameCdq("Business Partner Name")
    val name2 = NameCdq("Company ABC AG")
    val name3 = NameCdq("Another Organisation Corp")
    val name4 = NameCdq("Catena Test Name")
    val name5 = NameCdq("好公司  合伙制企业")

    val legalFormDoc1 = LegalFormCdq(name = "Limited Liability Company", technicalKey = "LF1")
    val legalFormDoc2 = LegalFormCdq(name = "Gemeinschaft mit beschränkter Haftung", technicalKey = "LF2")
    val legalFormDoc3 = LegalFormCdq(name = "股份有限公司", technicalKey = "LF3")

    val statusType1 = TypeKeyNameUrlCdq("ACTIVE")
    val statusType2 = TypeKeyNameUrlCdq("DISSOLVED")
    val statusType3 = TypeKeyNameUrlCdq("INSOLVENCY")

    val status1 = BusinessPartnerStatusCdq(statusType1, "Active", LocalDateTime.now())
    val status2 = BusinessPartnerStatusCdq(statusType2, "Dissolved", LocalDateTime.now())
    val status3 = BusinessPartnerStatusCdq(statusType3, "Insolvent", LocalDateTime.now())

    val classificationType = TypeKeyNameUrlCdq("NACE")

    val classification1 = ClassificationCdq(value = "Sale of motor vehicles", type = classificationType)
    val classification2 = ClassificationCdq(value = "Data processing, hosting and related activities", type = classificationType)
    val classification3 = ClassificationCdq(value = "Other information service activities", type = classificationType)
    val classification4 = ClassificationCdq(value = "Financial and insurance activities", type = classificationType)
    val classification5 = ClassificationCdq(value = "Accounting, bookkeeping and auditing activities; tax consultancy", type = classificationType)

    val profile1 = PartnerProfileCdq(classifications = listOf(classification1, classification2))
    val profile2 = PartnerProfileCdq(classifications = listOf(classification3, classification4))
    val profile3 = PartnerProfileCdq(classifications = listOf(classification5))

    val addressId1 = "address-cdq-1"
    val addressId2 = "address-cdq-2"
    val addressId3 = "address-cdq-3"

    val adminArea1 = AdministrativeAreaCdq("Baden-Württemberg")
    val adminArea2 = AdministrativeAreaCdq("Stuttgart")
    val adminArea3 = AdministrativeAreaCdq("Georgia")
    val adminArea4 = AdministrativeAreaCdq("South Carolina")
    val adminArea5 = AdministrativeAreaCdq("河北省")

    val postCode1 = PostCodeCdq("70546")
    val postCode2 = PostCodeCdq("70547")
    val postCode3 = PostCodeCdq("30346")
    val postCode4 = PostCodeCdq("07677-7731")
    val postCode5 = PostCodeCdq("511464")

    val locality1 = LocalityCdq(value = "Stuttgart")
    val locality2 = LocalityCdq(value = "Vaihingen")
    val locality3 = LocalityCdq(value = "5th Congressional District")
    val locality4 = LocalityCdq(value = "Woodcliff Lake")
    val locality5 = LocalityCdq(value = "北京市")

    val thoroughfare1 = ThoroughfareCdq(value = "Mercedesstraße 120")
    val thoroughfare2 = ThoroughfareCdq(value = "Werk 1")
    val thoroughfare3 = ThoroughfareCdq(value = "300 Chestnut Ridge Road")
    val thoroughfare4 = ThoroughfareCdq(value = "Factory 1")
    val thoroughfare5 = ThoroughfareCdq(value = "工人体育场东路")

    val premise1 = PremiseCdq(value = "Bauteil A")
    val premise2 = PremiseCdq(value = "Etage 1")
    val premise3 = PremiseCdq(value = "Building 1")
    val premise4 = PremiseCdq(value = "First Floor")
    val premise5 = PremiseCdq(value = "主楼")

    val postalDeliveryPoint1 = PostalDeliveryPointCdq(value = "Postal Delivery point")
    val postalDeliveryPoint2 = PostalDeliveryPointCdq(value = "Mailbox Premise Street")
    val postalDeliveryPoint3 = PostalDeliveryPointCdq(value = "Mail Station A")
    val postalDeliveryPoint4 = PostalDeliveryPointCdq(value = "Post Office Box 1")
    val postalDeliveryPoint5 = PostalDeliveryPointCdq(value = "邮政投递点")

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
        createdAt = LocalDateTime.now(),
        lastModifiedAt = LocalDateTime.now(),
        externalId = partnerId1,
        dataSource = datasource1,
        names = listOf(name1, name2),
        legalForm = legalFormDoc1,
        status = status1,
        profile = profile1,
        addresses = listOf(address1)
    )

    val businessPartner2 = BusinessPartnerCdq(
        id = partnerId2,
        createdAt = LocalDateTime.now(),
        lastModifiedAt = LocalDateTime.now(),
        externalId = partnerId2,
        dataSource = datasource1,
        names = listOf(name3, name4),
        legalForm = legalFormDoc2,
        status = status2,
        profile = profile2,
        addresses = listOf(address2)
    )

    val businessPartner3 = BusinessPartnerCdq(
        id = partnerId3,
        createdAt = LocalDateTime.now(),
        lastModifiedAt = LocalDateTime.now(),
        externalId = partnerId3,
        dataSource = datasource1,
        names = listOf(name5),
        legalForm = legalFormDoc3,
        status = status3,
        profile = profile3,
        addresses = listOf(address3)
    )
}