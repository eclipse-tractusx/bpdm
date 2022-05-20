package com.catenax.gpdm.util

import java.time.LocalDateTime
import java.util.*

/**
 * Contains simple test values used to create more complex test values such as DTOs
 */
object CommonValues {

    val bpn1 = "BPNL0000000000XY"
    val bpn2 = "BPNL0000000001XZ"
    val bpn3 = "BPNL0000000002ZZ"

    val bpnA1 = "BPNA0000000000XY"
    val bpnA2 = "BPNA0000000001XZ"
    val bpnA3 = "BPNA0000000002ZZ"

    val uuid1 = UUID.fromString("e9975a48-b190-4bf1-a7e6-73c6a1744de8")

    val name1 = "Business Partner Name"
    val name2 = "Company ABC AG"
    val name3 = "Another Organisation Corp"
    val name4 = "Catena Test Name"
    val name5 = "好公司  合伙制企业"

    val legalFormTechnicalKey1 = "LF1"
    val legalFormTechnicalKey2 = "LF2"
    val legalFormTechnicalKey3 = "LF3"

    val legalFormName1 = "Limited Liability Company"
    val legalFormName2 = "Gemeinschaft mit beschränkter Haftung"
    val legalFormName3 = "股份有限公司"

    val statusType1 = "ACTIVE"
    val statusType2 = "DISSOLVED"
    val statusType3 = "INSOLVENCY"

    val statusDenotation1 = "Active"
    val statusDenotation2 = "Dissolved"
    val statusDenotation3 = "Insolvent"

    val statusValidFrom1 = LocalDateTime.of(2020, 1, 1, 0, 0)
    val statusValidFrom2 = LocalDateTime.of(2019, 1, 1, 0, 0)
    val statusValidFrom3 = LocalDateTime.of(2018, 1, 1, 0, 0)

    val classificationType = "NACE"

    val classification1 = "Sale of motor vehicles"
    val classification2 = "Data processing, hosting and related activities"
    val classification3 = "Other information service activities"
    val classification4 = "Financial and insurance activities"
    val classification5 = "Accounting, bookkeeping and auditing activities; tax consultancy"

    val adminArea1 = "Baden-Württemberg"
    val adminArea2 = "Stuttgart"
    val adminArea3 = "Georgia"
    val adminArea4 = "South Carolina"
    val adminArea5 = "河北省"

    val postCode1 = "70546"
    val postCode2 = "70547"
    val postCode3 = "30346"
    val postCode4 = "07677-7731"
    val postCode5 = "511464"

    val locality1 = "Stuttgart"
    val locality2 = "Vaihingen"
    val locality3 = "5th Congressional District"
    val locality4 = "Woodcliff Lake"
    val locality5 = "北京市"

    val thoroughfare1 = "Mercedesstraße 120"
    val thoroughfare2 = "Werk 1"
    val thoroughfare3 = "300 Chestnut Ridge Road"
    val thoroughfare4 = "Factory 1"
    val thoroughfare5 = "工人体育场东路"

    val premise1 = "Bauteil A"
    val premise2 = "Etage 1"
    val premise3 = "Building 1"
    val premise4 = "First Floor"
    val premise5 = "主楼"

    val postalDeliveryPoint1 = "Postal Delivery point"
    val postalDeliveryPoint2 = "Mailbox Premise Street"
    val postalDeliveryPoint3 = "Mail Station A"
    val postalDeliveryPoint4 = "Post Office Box 1"
    val postalDeliveryPoint5 = "邮政投递点"




}