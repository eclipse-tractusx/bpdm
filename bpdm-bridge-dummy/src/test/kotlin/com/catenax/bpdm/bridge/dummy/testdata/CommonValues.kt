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

package com.catenax.bpdm.bridge.dummy.testdata

import com.neovisionaries.i18n.CountryCode
import com.neovisionaries.i18n.LanguageCode
import org.eclipse.tractusx.bpdm.common.dto.NameRegioncodeVerboseDto
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.CharacterSet
import org.eclipse.tractusx.bpdm.common.model.ClassificationType
import org.eclipse.tractusx.bpdm.common.model.NameType
import java.time.Instant
import java.time.LocalDateTime

/**
 * Contains simple test values used to create more complex test values such as DTOs
 */
object CommonValues {

    val now = Instant.now()

    const val externalId1 = "external-1"
    const val externalId2 = "external-2"
    const val externalId3 = "external-3"
    const val externalId4 = "external-4"
    const val externalId5 = "external-5"

    const val externalIdSite1 = "site-external-1"
    const val externalIdSite2 = "site-external-2"

    const val externalIdAddress1 = "address-external-1"
    const val externalIdAddress2 = "address-external-2"

    const val bpn1 = "BPNL0000000000XY"
    const val bpn2 = "BPNL0000000001XZ"
    const val bpn3 = "BPNL0000000002XY"

    const val bpnSite1 = "BPNS0000000001XY"
    const val bpnSite2 = "BPNS0000000002XY"
    const val bpnSite3 = "BPNS0000000003X9"

    const val bpnAddress1 = "BPNA0000000001XY"
    const val bpnAddress2 = "BPNA0000000002XY"
    const val bpnAddress3 = "BPNA0000000003X9"

    val language1 = LanguageCode.de
    val language2 = LanguageCode.en

    val characterSet1 = CharacterSet.WESTERN_LATIN_STANDARD
    val characterSet2 = CharacterSet.GREEK

    val country1 = CountryCode.DE
    val country2 = CountryCode.US

    const val name1 = "Business Partner Name"
    const val name2 = "Company ABC AG"
    const val name3 = "Another Organisation Corp"
    const val name4 = "Catena Test Name"

    const val nameSite1 = "Site A"
    const val nameSite2 = "Site B"

    const val identifierValue1 = "DE123456789"
    const val identifierValue2 = "US123456789"
    const val identifierValue3 = "FR123456789"
    const val identifierValue4 = "NL123456789"

    const val identifierIssuingBodyTechnicalKey1 = "issuing body 1"
    const val identifierIssuingBodyTechnicalKey2 = "issuing body 2"
    const val identifierIssuingBodyTechnicalKey3 = "issuing body 3"
    const val identifierIssuingBodyTechnicalKey4 = "issuing body 4"

    const val identifierIssuingBodyName1 = "Agency X"
    const val identifierIssuingBodyName2 = "Body Y"
    const val identifierIssuingBodyName3 = "Official Z"
    const val identifierIssuingBodyName4 = "Gov A"

    const val identifierIssuingBody1 = "Agency X"
    const val identifierIssuingBody2 = "Body Y"
    const val identifierIssuingBody3 = "Official Z"
    const val identifierIssuingBody4 = "Gov A"

    const val identifierIssuingBodyUrl1 = "http://catenax-host/issuing-body1"
    const val identifierIssuingBodyUrl2 = "http://catenax-host/issuing-body2"
    const val identifierIssuingBodyUrl3 = "http://catenax-host/issuing-body3"
    const val identifierIssuingBodyUrl4 = "http://catenax-host/issuing-body4"

    const val identifierStatusTechnicalKey1 = "ACTIVE"
    const val identifierStatusTechnicalKey2 = "EXPIRED"
    const val identifierStatusTechnicalKey3 = "PENDING"
    const val identifierStatusTechnicalKey4 = "UNKNOWN"

    const val identifierStatusName1 = "Active"
    const val identifierStatusName2 = "Expired"
    const val identifierStatusName3 = "Pending"
    const val identifierStatusName4 = "Unknown Status"

    const val identifierTypeName1 = "Steuernummer"
    const val identifierTypeName2 = "VAT USA"
    const val identifierTypeName3 = "VAT France"
    const val identifierTypeName4 = "VAT Netherlands"

    const val identifierTypeUrl1 = "http://catenax-host/id-type1"
    const val identifierTypeUrl2 = "http://catenax-host/id-type2"
    const val identifierTypeUrl3 = "http://catenax-host/id-type3"
    const val identifierTypeUrl4 = "http://catenax-host/id-type4"

    const val identifierTypeTechnicalKey1 = "VAT_DE"
    const val identifierTypeTechnicalKey2 = "VAT_US"
    const val identifierTypeTechnicalKey3 = "VAT_FR"
    const val identifierTypeTechnicalKey4 = "VAT_NL"

    val nameType1 = NameType.OTHER

    const val shortName1 = "short1"
    const val shortName2 = "short2"
    const val shortName3 = "short3"
    const val shortName4 = "short4"


    const val legalFormTechnicalKey1 = "LF1"
    const val legalFormTechnicalKey2 = "LF2"

    const val legalFormName1 = "Limited Liability Company"
    const val legalFormName2 = "Gemeinschaft mit beschränkter Haftung"

    const val legalFormAbbreviation1 = "LLC"
    const val legalFormAbbreviation2 = "GmbH"

    const val businessStatusOfficialDenotation1 = "Active"
    const val businessStatusOfficialDenotation2 = "Insolvent"

    val businessStatusValidFrom1 = LocalDateTime.of(2020, 1, 1, 0, 0)
    val businessStatusValidFrom2 = LocalDateTime.of(2019, 1, 1, 0, 0)

    val businessStatusValidUntil1 = LocalDateTime.of(2021, 1, 1, 0, 0)
    val businessStatusValidUntil2 = LocalDateTime.of(2022, 1, 1, 0, 0)

    val businessStateType1 = BusinessStateType.ACTIVE
    val businessStateType2 = BusinessStateType.INACTIVE

    val classificationType = ClassificationType.NACE

    const val classificationValue1 = "Sale of motor vehicles"
    const val classificationValue2 = "Data processing, hosting and related activities"
    const val classificationValue3 = "Other information service activities"
    const val classificationValue4 = "Financial and insurance activities"

    const val classificationCode1 = "code1"
    const val classificationCode2 = "code2"
    const val classificationCode3 = "code3"
    const val classificationCode4 = "code4"

    const val careOf1 = "Caring Entity Co"
    const val careOf2 = "Another Caring Entity"

    const val context1 = "Context1"
    const val context2 = "Context2"

    // TODO enable regionCodes later
//    val adminAreaLevel1RegionCode_1 = "BW"
//    val adminAreaLevel1Region1 = NameRegioncodeDto(adminAreaLevel1RegionCode_1, "Baden-Württemberg")
//    val adminAreaLevel1RegionCode_2 = "GA"
//    val adminAreaLevel1Region2 = NameRegioncodeDto(adminAreaLevel1RegionCode_2, "Georgia")

    val adminAreaLevel1RegionCode_1: String? = null
    val adminAreaLevel1Region1: NameRegioncodeVerboseDto? = null
    val adminAreaLevel1RegionCode_2: String? = null
    val adminAreaLevel1Region2: NameRegioncodeVerboseDto? = null

    const val county1 = "Stuttgart"
    const val county2 = " Fulton County"

    const val city1 = "Stuttgart"
    const val city2 = "Atlanta"


    const val district1 = "Vaihingen"
    const val district2 = "TODO"
    const val district3 = "TODO"

    const val street1 = "Mercedesstraße"
    const val street2 = "TODO"
    const val street3 = "TODO"

    const val houseNumber1 = ""
    const val houseNumber2 = ""
    const val houseNumber3 = ""

    const val direction1 = "direction1"
    const val direction2 = "direction1"

    const val industrialZone1 = "Werk 1"
    const val industrialZone2 = "Industrial Zone Two"
    const val industrialZone3 = "Industrial Zone Three"

    const val building1 = "Bauteil A"
    const val building2 = "Building Two"
    const val building3 = "Building Two"

    const val floor1 = "Etage 1"
    const val floor2 = "Floor Two"
    const val floor3 = "Floor Two"

    const val door1 = "Door One"
    const val door2 = "Door Two"
    const val door3 = "Door Two"

    const val postCode1 = "70546 "
    const val postCode2 = "70547"

    val geoCoordinates1 = Triple(0f, 0f, 0f)
    val geoCoordinates2 = Triple(1f, 1f, 0f)

}