/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.pool.util

import com.neovisionaries.i18n.CountryCode
import com.neovisionaries.i18n.LanguageCode
import org.eclipse.tractusx.bpdm.common.dto.NameRegioncodeDto
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.CharacterSet
import org.eclipse.tractusx.bpdm.common.model.ClassificationType
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

/**
 * Contains simple test values used to create more complex test values such as DTOs
 */
object CommonValues {

    val now = Instant.now()

    //The BPNs should match the first generated BPNs from the Issuer in order
    val bpnL1 = "BPNL000000000001"
    val bpnL2 = "BPNL0000000001YN"
    val bpnL3 = "BPNL0000000002XY"

    val bpnA1 = "BPNA000000000001"
    val bpnA2 = "BPNA0000000001YN"
    val bpnA3 = "BPNA0000000002XY"

    val bpnS1 = "BPNS000000000001"
    val bpnS2 = "BPNS0000000001YN"
    val bpnS3 = "BPNS0000000002XY"

    val index1 = "1"
    val index2 = "2"
    val index3 = "3"

    val uuid1 = UUID.fromString("e9975a48-b190-4bf1-a7e6-73c6a1744de8")

    val language0 = LanguageCode.undefined
    val language1 = LanguageCode.en
    val language2 = LanguageCode.de
    val language3 = LanguageCode.zh

    val characterSet1 = CharacterSet.UNDEFINED

    val country1 = CountryCode.DE
    val country2 = CountryCode.FR
    val country3 = CountryCode.PL

    val name1 = "Business Partner Name"
    val name2 = "Company ABC AG"
    val name3 = "Another Organisation Corp"
    val name4 = "Catena Test Name"
    val name5 = "好公司  合伙制企业"
    val name6 = "Something Ltd."
    val name7 = "Gmbh XY"
    val name8 = "XX LLC"

    val siteName1 = "Stammwerk A"
    val siteName2 = "Halle B1"
    val siteName3 = "主厂房 C"

    val legalFormTechnicalKey1 = "LF1"
    val legalFormTechnicalKey2 = "LF2"
    val legalFormTechnicalKey3 = "LF3"

    val legalFormName1 = "Limited Liability Company"
    val legalFormName2 = "Gemeinschaft mit beschränkter Haftung"
    val legalFormName3 = "股份有限公司"

    val legalFormAbbreviation1 = "LLC"
    val legalFormAbbreviation2 = "GmbH"
    val legalFormAbbreviation3 = "股份有限"

    val legalFormCategoryName1 = "Limited Company"
    val legalFormCategoryName2 = "Stock Company"
    val legalFormCategoryName3 = "Private Foundation"

    val legalFormCategoryUrl1 = "http://catenax-host/legal-category1"
    val legalFormCategoryUrl2 = "http://catenax-host/legal-category2"
    val legalFormCategoryUrl3 = "http://catenax-host/legal-category3"

    val identifierTypeTechnicalKey1 = "VAT_DE"
    val identifierTypeTechnicalKey2 = "VAT_US"
    val identifierTypeTechnicalKey3 = "VAT_FR"

    val identifierTypeName1 = "Steuernummer"
    val identifierTypeName2 = "VAT USA"
    val identifierTypeName3 = "VAT France"

    val issuingBody1 = "Agency X"
    val issuingBody2 = "Body Y"
    val issuingBody3 = "Official Z"

    val identifierValue1 = "ID-XYZ"
    val identifierValue2 = "Another ID Value"
    val identifierValue3 = "An ID Value"

    val statusType1 = BusinessStateType.ACTIVE
    val statusType2 = BusinessStateType.INACTIVE
    val statusType3 = BusinessStateType.ACTIVE         // TODO unknown?

    val statusDenotation1 = "Active"
    val statusDenotation2 = "Dissolved"
    val statusDenotation3 = "Insolvent"

    val statusValidFrom1 = LocalDateTime.of(2020, 1, 1, 0, 0)
    val statusValidFrom2 = LocalDateTime.of(2019, 1, 1, 0, 0)
    val statusValidFrom3 = LocalDateTime.of(2018, 1, 1, 0, 0)

    val classificationType = ClassificationType.NACE

    val classification1 = "Sale of motor vehicles"
    val classification2 = "Data processing, hosting and related activities"
    val classification3 = "Other information service activities"
    val classification4 = "Financial and insurance activities"
    val classification5 = "Accounting, bookkeeping and auditing activities; tax consultancy"

    // TODO enable regionCodes later
//    val adminAreaLevel1RegionCode_1 = "BW"
//    val adminAreaLevel1Region1 = NameRegioncodeDto(adminAreaLevel1RegionCode_1, "Baden-Württemberg")
//    val adminAreaLevel1RegionCode_2 = "GA"
//    val adminAreaLevel1Region2 = NameRegioncodeDto(adminAreaLevel1RegionCode_2, "Georgia")
//    val adminAreaLevel1RegionCode_3 = "GA"
//    val adminAreaLevel1Region3 = NameRegioncodeDto(adminAreaLevel1RegionCode_3, "Georgia")

    val adminAreaLevel1RegionCode_1: String? = null
    val adminAreaLevel1Region1: NameRegioncodeDto? = null
    val adminAreaLevel1RegionCode_2: String? = null
    val adminAreaLevel1Region2: NameRegioncodeDto? = null
    val adminAreaLevel1RegionCode_3: String? = null
    val adminAreaLevel1Region3: NameRegioncodeDto? = null

    val county1 = "Böblingen"
    val county2 = " Fulton County"
    val county3 = " Fulton County"

    val city1 = "Böblingen"
    val city2 = "Atlanta"
    val city3 = "Atlanta"

    val district1 = "Sindelfingen-Ost"
    val district2 = "District Level 1"
    val district3 = "DL 1"
    
    val street1 = "Bela-Barenyi-Straße"
    val street2 = ""
    val street3 = ""

    val houseNumber1 = ""
    val houseNumber2 = ""
    val houseNumber3 = ""

    val industrialZone1 = "Industrial Zone One"
    val industrialZone2 = "Industrial Zone Two"
    val industrialZone3 = "Industrial Zone Three"

    val building1 = "Gebäude eins"
    val building2 = "Building Two"
    val building3 = "tedifício  três"

    val floor1 = "Stockerk eins"
    val floor2 = "Floor Two"
    val floor3 = "piso três"

    val door1 = "Raum eins"
    val door2 = "Door Two"
    val door3 = "peça três"

    val postCode1 = "71059 "
    val postCode2 = "70547"
    val postCode3 = "30346"
    val postCode4 = "07677-7731"
    val postCode5 = "511464"

    val adminArea1 = "Stuttgart"
    val adminArea2 = "Stuttgart"
    val adminArea3 = "Georgia"
    val adminArea4 = "South Carolina"
    val adminArea5 = "河北省"

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
    val premise6 = "Komplex X"

    val postalDeliveryPoint1 = "Postal Delivery point"
    val postalDeliveryPoint2 = "Mailbox Premise Street"
    val postalDeliveryPoint3 = "Mail Station A"
    val postalDeliveryPoint4 = "Post Office Box 1"
    val postalDeliveryPoint5 = "邮政投递点"
}