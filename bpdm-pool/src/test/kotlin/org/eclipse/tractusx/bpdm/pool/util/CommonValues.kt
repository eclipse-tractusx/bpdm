/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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
    val bpnA4 = "BPNA0000000003A1"


    val bpnS1 = "BPNS0000000000XY"
    val bpnS2 = "BPNS0000000001XZ"
    val bpnS3 = "BPNS0000000002ZZ"

    val uuid1 = UUID.fromString("e9975a48-b190-4bf1-a7e6-73c6a1744de8")

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

    val legalFormUrl1 = "http://catenax-host/legal-form1"
    val legalFormUrl2 = "http://catenax-host/legal-form2"
    val legalFormUrl3 = "http://catenax-host/legal-form3"

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

    val identiferTypeName1 = "Steuernummer"
    val identiferTypeName2 = "VAT USA"
    val identiferTypeName3 = "VAT France"

    val identifierTypeUrl1 = "http://catenax-host/id-type1"
    val identifierTypeUrl2 = "http://catenax-host/id-type2"
    val identifierTypeUrl3 = "http://catenax-host/id-type3"

    val identifierStatusKey1 = "ACTIVE"
    val identifierStatusKey2 = "EXPIRED"
    val identifierStatusKey3 = "UNKNOWN"

    val identifierStatusName1 = "Active"
    val identifierStatusName2 = "Expired"
    val identifierStatusName3 = "Unknown Status"

    val issuingBodyKey1 = "AGENCYX"
    val issuingBodyKey2 = "BODYY"
    val issuingBodyKey3 = "OFFICIALZ"

    val issuingBodyName1 = "Agency X"
    val issuingBodyName2 = "Body Y"
    val issuingBodyName3 = "Official Z"

    val issuingBodyUrl1 = "http://catenax-host/issuing-body1"
    val issuingBodyUrl2 = "http://catenax-host/issuing-body2"
    val issuingBodyUrl3 = "http://catenax-host/issuing-body3"


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
    val premise6 = "Komplex X"

    val postalDeliveryPoint1 = "Postal Delivery point"
    val postalDeliveryPoint2 = "Mailbox Premise Street"
    val postalDeliveryPoint3 = "Mail Station A"
    val postalDeliveryPoint4 = "Post Office Box 1"
    val postalDeliveryPoint5 = "邮政投递点"
}