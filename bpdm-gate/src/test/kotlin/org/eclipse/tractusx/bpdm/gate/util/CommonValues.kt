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

package org.eclipse.tractusx.bpdm.gate.util

import com.neovisionaries.i18n.CountryCode
import com.neovisionaries.i18n.LanguageCode
import org.eclipse.tractusx.bpdm.common.model.*
import org.eclipse.tractusx.bpdm.gate.api.model.response.LsaType
import org.eclipse.tractusx.bpdm.gate.api.model.response.OptionalLsaType
import java.time.Instant
import java.time.LocalDateTime

/**
 * Contains simple test values used to create more complex test values such as DTOs
 */
object CommonValues {

    val now = Instant.now()

    val externalId1 = "external-1"
    val externalId2 = "external-2"
    val externalId3 = "external-3"
    val externalId4 = "external-4"
    val externalId5 = "external-5"

    val lsaTypeParam = LsaType.Address
    val lsaTypeParamNotFound = LsaType.Site
    val lsaNone = OptionalLsaType.None

    val externalIdSite1 = "site-external-1"
    val externalIdSite2 = "site-external-2"

    val externalIdAddress1 = "address-external-1"
    val externalIdAddress2 = "address-external-2"

    val bpn1 = "BPNL0000000000XY"
    val bpn2 = "BPNL0000000001XZ"
    val bpn3 = "BPNL0000000002XY"

    val bpnSite1 = "BPNS0000000001XY"
    val bpnSite2 = "BPNS0000000002XY"
    val bpnSite3 = "BPNS0000000003X9"

    val bpnAddress1 = "BPNA0000000001XY"
    val bpnAddress2 = "BPNA0000000002XY"
    val bpnAddress3 = "BPNA0000000003X9"

    val language1 = LanguageCode.de
    val language2 = LanguageCode.en

    val characterSet1 = CharacterSet.WESTERN_LATIN_STANDARD
    val characterSet2 = CharacterSet.GREEK

    val country1 = CountryCode.DE
    val country2 = CountryCode.US

    val name1 = "Business Partner Name"
    val name2 = "Company ABC AG"
    val name3 = "Another Organisation Corp"
    val name4 = "Catena Test Name"

    val nameSite1 = "Site A"
    val nameSite2 = "Site B"

    val identifierValue1 = "DE123456789"
    val identifierValue2 = "US123456789"
    val identifierValue3 = "FR123456789"
    val identifierValue4 = "NL123456789"

    val identifierIssuingBodyTechnicalKey1 = "issuing body 1"
    val identifierIssuingBodyTechnicalKey2 = "issuing body 2"
    val identifierIssuingBodyTechnicalKey3 = "issuing body 3"
    val identifierIssuingBodyTechnicalKey4 = "issuing body 4"

    val identifierIssuingBodyName1 = "Agency X"
    val identifierIssuingBodyName2 = "Body Y"
    val identifierIssuingBodyName3 = "Official Z"
    val identifierIssuingBodyName4 = "Gov A"

    val identifierIssuingBody1 = "Agency X"
    val identifierIssuingBody2 = "Body Y"
    val identifierIssuingBody3 = "Official Z"
    val identifierIssuingBody4 = "Gov A"

    val identifierIssuingBodyUrl1 = "http://catenax-host/issuing-body1"
    val identifierIssuingBodyUrl2 = "http://catenax-host/issuing-body2"
    val identifierIssuingBodyUrl3 = "http://catenax-host/issuing-body3"
    val identifierIssuingBodyUrl4 = "http://catenax-host/issuing-body4"

    val identifierStatusTechnicalKey1 = "ACTIVE"
    val identifierStatusTechnicalKey2 = "EXPIRED"
    val identifierStatusTechnicalKey3 = "PENDING"
    val identifierStatusTechnicalKey4 = "UNKNOWN"

    val identifierStatusName1 = "Active"
    val identifierStatusName2 = "Expired"
    val identifierStatusName3 = "Pending"
    val identifierStatusName4 = "Unknown Status"

    val identifierTypeName1 = "Steuernummer"
    val identifierTypeName2 = "VAT USA"
    val identifierTypeName3 = "VAT France"
    val identifierTypeName4 = "VAT Netherlands"

    val identifierTypeUrl1 = "http://catenax-host/id-type1"
    val identifierTypeUrl2 = "http://catenax-host/id-type2"
    val identifierTypeUrl3 = "http://catenax-host/id-type3"
    val identifierTypeUrl4 = "http://catenax-host/id-type4"

    val identifierTypeTechnicalKey1 = "VAT_DE"
    val identifierTypeTechnicalKey2 = "VAT_US"
    val identifierTypeTechnicalKey3 = "VAT_FR"
    val identifierTypeTechnicalKey4 = "VAT_NL"

    val nameType1 = NameType.OTHER

    val shortName1 = "short1"
    val shortName2 = "short2"
    val shortName3 = "short3"
    val shortName4 = "short4"


    val legalFormTechnicalKey1 = "LF1"
    val legalFormTechnicalKey2 = "LF2"

    val legalFormName1 = "Limited Liability Company"
    val legalFormName2 = "Gemeinschaft mit beschränkter Haftung"

    val legalFormUrl1 = "http://catenax-host/legal-form1"
    val legalFormUrl2 = "http://catenax-host/legal-form2"

    val legalFormAbbreviation1 = "LLC"
    val legalFormAbbreviation2 = "GmbH"

    val legalFormCategoryName1 = "Limited Company"
    val legalFormCategoryName2 = "Stock Company"

    val legalFormCategoryUrl1 = "http://catenax-host/legal-category1"
    val legalFormCategoryUrl2 = "http://catenax-host/legal-category2"

    val businessStatusOfficialDenotation1 = "Active"
    val businessStatusOfficialDenotation2 = "Insolvent"

    val businessStatusValidFrom1 = LocalDateTime.of(2020, 1, 1, 0, 0)
    val businessStatusValidFrom2 = LocalDateTime.of(2019, 1, 1, 0, 0)

    val businessStatusValidUntil1 = LocalDateTime.of(2021, 1, 1, 0, 0)
    val businessStatusValidUntil2 = LocalDateTime.of(2022, 1, 1, 0, 0)

    val businessStatusType1 = BusinessStatusType.ACTIVE
    val businessStatusType2 = BusinessStatusType.INACTIVE

    val classificationType = ClassificationType.NACE

    val classificationValue1 = "Sale of motor vehicles"
    val classificationValue2 = "Data processing, hosting and related activities"
    val classificationValue3 = "Other information service activities"
    val classificationValue4 = "Financial and insurance activities"

    val classificationCode1 = "code1"
    val classificationCode2 = "code2"
    val classificationCode3 = "code3"
    val classificationCode4 = "code4"

    val internationalBankIdentifier0 = "11111111"
    val nationalBankIdentifier0 = "22222222"

    val internationalBankAccountIdentifier1 = "DE0000000000000000001"
    val nationalBankAccountIdentifier1 = "000000001"

    val internationalBankAccountIdentifier2 = "DE0000000000000000002"
    val nationalBankAccountIdentifier2 = "000000002"

    val internationalBankAccountIdentifier3 = "DE0000000000000000001"
    val nationalBankAccountIdentifier3 = "000000001"

    val internationalBankAccountIdentifier4 = "DE0000000000000000002"
    val nationalBankAccountIdentifier4 = "000000002"

    val careOf1 = "Caring Entity Co"
    val careOf2 = "Another Caring Entity"

    val context1 = "Context1"
    val context2 = "Context2"

    val adminArea1 = "Baden-Württemberg"
    val adminArea2 = "Stuttgart"

    val adminAreaType1 = AdministrativeAreaType.COUNTY
    val adminAreaType2 = AdministrativeAreaType.REGION

    val postCode1 = "70546"
    val postCode2 = "70547"

    val postCodeType1 = PostCodeType.REGULAR
    val postCodeType2 = PostCodeType.POST_BOX

    val locality1 = "Stuttgart"
    val locality2 = "Vaihingen"

    val localityType1 = LocalityType.CITY
    val localityType2 = LocalityType.DISTRICT

    val thoroughfare1 = "Mercedesstraße 120"
    val thoroughfare2 = "Werk 1"

    val thoroughfareType1 = ThoroughfareType.STREET
    val thoroughfareType2 = ThoroughfareType.INDUSTRIAL_ZONE

    val premise1 = "Bauteil A"
    val premise2 = "Etage 1"

    val premiseType1 = PremiseType.BUILDING
    val premiseType2 = PremiseType.WAREHOUSE

    val postalDeliveryPoint1 = "Postal Delivery point"
    val postalDeliveryPoint2 = "Mailbox Premise Street"

    val postalDeliveryPointType1 = PostalDeliveryPointType.MAILBOX
    val postalDeliveryPointType2 = PostalDeliveryPointType.POST_OFFICE_BOX

    val geoCoordinates1 = Triple(0f, 0f, 0f)
    val geoCoordinates2 = Triple(1f, 1f, 0f)


}