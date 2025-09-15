/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.test.testdata.pool

import com.neovisionaries.i18n.CountryCode
import com.neovisionaries.i18n.LanguageCode
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.TypeKeyNameVerboseDto
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.ClassificationType
import org.eclipse.tractusx.bpdm.common.service.toDto
import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.api.model.response.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

/**
 * Test values for response DTOs
 * Numbered values should match with @see SaasValues numbered values for easier testing
 */
object BusinessPartnerVerboseValues {

    val firstBpnL = "BPNL000000000065"
    val secondBpnL = "BPNL00000000015G"
    val thirdBpnl = "BPNL00000000024R"

    val firstBpnA = "BPNA00000000009W"
    val secondBpnA = "BPNA000000000197"
    val thirdBpnA = "BPNA00000000028I"

    val firstBpnS = "BPNS0000000000WN"
    val secondBpnS = "BPNS0000000001VY"
    val thirdBpnS = "BPNS0000000002V9"

    private val createdTime1 = LocalDateTime.of(2020, 1, 1, 1, 1)
    val language0 = TypeKeyNameVerboseDto(LanguageCode.undefined, LanguageCode.undefined.getName())
    val language1 = TypeKeyNameVerboseDto(LanguageCode.en, LanguageCode.en.getName())
    val language2 = TypeKeyNameVerboseDto(LanguageCode.de, LanguageCode.de.getName())
    val language3 = TypeKeyNameVerboseDto(LanguageCode.zh, LanguageCode.zh.getName())

    private val country1 = TypeKeyNameVerboseDto(CountryCode.DE, CountryCode.DE.getName())
    private val country2 = TypeKeyNameVerboseDto(CountryCode.FR, CountryCode.FR.getName())
    private val country3 = TypeKeyNameVerboseDto(CountryCode.PL, CountryCode.PL.getName())

    val identifierType1 = TypeKeyNameVerboseDto("DE_EU_VAT_ID", "European Union Value-added Tax Identification Number")
    val identifierType2 = TypeKeyNameVerboseDto("DE_HR", "Handelsregisternummer")
    val identifierType3 = TypeKeyNameVerboseDto("AT_EU_VAT_ID", "European Union Value-added Tax Identification Number")
    val identifierType4 = TypeKeyNameVerboseDto("DNB_DUNS", "Data Universal Numbering System Number")

    val bpnLRequestMapping = BpnRequestIdentifierMappingDto(UUID.randomUUID().toString(), bpn = secondBpnL)
    val bpnSRequestMapping = BpnRequestIdentifierMappingDto(UUID.randomUUID().toString(), bpn = secondBpnS)
    val bpnARequestMapping = BpnRequestIdentifierMappingDto(UUID.randomUUID().toString(), bpn = secondBpnA)

    val identifierTypeAbbreviation1 = "EU VAT ID (number)"
    val identifierTypeAbbreviation2 = "HR(-nummer)"
    val identifierTypeAbbreviation3 = "EU VAT ID (number)"
    val identifierTypeAbbreviation4 = "DUNS (number)"

    val identifierTypeTransliteratedName1 = ""
    val identifierTypeTransliteratedName2 = ""
    val identifierTypeTransliteratedName3 = ""
    val identifierTypeTransliteratedName4 = ""

    val identifierTypeTransliteratedAbbreviation1 = ""
    val identifierTypeTransliteratedAbbreviation2 = ""
    val identifierTypeTransliteratedAbbreviation3 = ""
    val identifierTypeTransliteratedAbbreviation4 = ""

    val identifierTypeFormat1 = "^DE\\d{8}\\d{1}(-\\d{5})?$"
    val identifierTypeFormat2 = "^([BDFGHKMNPRTUVWXY]{1}\\d{1,4}[VR]?\\.)?((HRA)|(G(n|N)R)|(HRB)|(PR)|(VR)|(G(s|S)R))[1-9]{1}[A-Z0-9]{1,5}$"
    val identifierTypeFormat3 = "^ATU\\d{7}\\d{1}$"

    val identifierTypeCategories1 = sortedSetOf(IdentifierTypeCategory.VAT)
    val identifierTypeCategories2 = sortedSetOf(IdentifierTypeCategory.NBR)
    val identifierTypeCategories3 = sortedSetOf(IdentifierTypeCategory.VAT)

    val addressIdentifierTypeAbbreviation1 = "(numéro) SIRET"
    val addressIdentifierTypeTransliteratedName1 = "Numero du systeme d'identification du repertoire des etablissements"
    val addressIdentifierTypeTransliteratedAbbreviation1 = "(numero) SIRET"


    val identifier1 =
        LegalEntityIdentifierVerboseDto("ID-XYZ", identifierType1, "Agency X")
    val identifier2 =
        LegalEntityIdentifierVerboseDto("Another ID Value", identifierType2, "Body Y")
    val identifier3 =
        LegalEntityIdentifierVerboseDto("An ID Value", identifierType3, "Official Z")

    val legalForm1 = LegalFormDto(
        technicalKey = "EI4J",
        name = "Limited Liability Company",
        abbreviations = "LLC;L.L.C.",
        transliteratedName = "Limited Liability Company",
        country = CountryCode.US,
        language = LanguageCode.en,
        administrativeAreaLevel1 = "US-CA",
        transliteratedAbbreviations = null,
        isActive = true,

    )
    val legalForm2 = LegalFormDto(
        technicalKey = "2HBR",
        name = "Gesellschaft mit beschränkter Haftung",
        abbreviations = "GmbH",
        transliteratedName = "Gesellschaft mit beschränkter Haftung",
        country = CountryCode.DE,
        language = LanguageCode.de,
        administrativeAreaLevel1 = null,
        transliteratedAbbreviations = null,
        isActive = true
    )
    val legalForm3 = LegalFormDto(
        technicalKey = "2M6Y",
        name = "基金会",
        abbreviations = null,
        transliteratedName = "ji jin hui",
        country = CountryCode.CN,
        language = LanguageCode.zh,
        administrativeAreaLevel1 = null,
        transliteratedAbbreviations = null,
        isActive = true
    )

    val statusType1 = TypeKeyNameVerboseDto(BusinessStateType.ACTIVE, BusinessStateType.ACTIVE.getTypeName())
    val statusType2 = TypeKeyNameVerboseDto(BusinessStateType.INACTIVE, BusinessStateType.INACTIVE.getTypeName())
    val statusType3 = TypeKeyNameVerboseDto(BusinessStateType.ACTIVE, BusinessStateType.ACTIVE.getTypeName())

    val leStatus1 = LegalEntityStateVerboseDto(LocalDateTime.of(2020, 1, 1, 0, 0), null, statusType1)
    val leStatus2 = LegalEntityStateVerboseDto(LocalDateTime.of(2019, 1, 1, 0, 0), null, statusType2)
    val leStatus3 = LegalEntityStateVerboseDto(LocalDateTime.of(2018, 1, 1, 0, 0), null, statusType3)

    val siteStatus1 = SiteStateVerboseDto(LocalDateTime.of(2020, 1, 1, 0, 0), null, BusinessStateType.ACTIVE.toDto())
    val siteStatus2 = SiteStateVerboseDto(LocalDateTime.of(2019, 1, 1, 0, 0), null, BusinessStateType.INACTIVE.toDto())
    val siteStatus3 = SiteStateVerboseDto(LocalDateTime.of(2018, 1, 1, 0, 0), null, BusinessStateType.ACTIVE.toDto())

    val classificationType = TypeKeyNameVerboseDto(ClassificationType.NACE, ClassificationType.NACE.name)

    val classification1 = LegalEntityClassificationVerboseDto("Sale of motor vehicles", null, classificationType)
    val classification2 = LegalEntityClassificationVerboseDto("Data processing, hosting and related activities", null, classificationType)
    val classification3 = LegalEntityClassificationVerboseDto("Other information service activities", null, classificationType)
    val classification4 = LegalEntityClassificationVerboseDto("Financial and insurance activities", null, classificationType)
    val classification5 = LegalEntityClassificationVerboseDto("Accounting, bookkeeping and auditing activities; tax consultancy", null, classificationType)

    private val confidenceCriteria1 = ConfidenceCriteriaDto(
        sharedByOwner = true,
        checkedByExternalDataSource = true,
        numberOfSharingMembers = 1,
        lastConfidenceCheckAt = LocalDateTime.of(2023, 10, 10, 10, 10, 10),
        nextConfidenceCheckAt = LocalDateTime.of(2024, 10, 10, 10, 10, 10),
        confidenceLevel = 10
    )

    private val confidenceCriteria2 = ConfidenceCriteriaDto(
        sharedByOwner = false,
        checkedByExternalDataSource = false,
        numberOfSharingMembers = 3,
        lastConfidenceCheckAt = LocalDateTime.of(2022, 10, 10, 10, 10, 10),
        nextConfidenceCheckAt = LocalDateTime.of(2025, 10, 10, 10, 10, 10),
        confidenceLevel = 6
    )

    private val confidenceCriteria3 = ConfidenceCriteriaDto(
        sharedByOwner = true,
        checkedByExternalDataSource = false,
        numberOfSharingMembers = 10,
        lastConfidenceCheckAt = LocalDateTime.of(2021, 10, 10, 10, 10, 10),
        nextConfidenceCheckAt = LocalDateTime.of(2026, 10, 10, 10, 10, 10),
        confidenceLevel = 3
    )

    val address1 = PhysicalPostalAddressVerboseDto(
        geographicCoordinates = null,
        countryVerbose = country1,
        postalCode = "71059 ",
        city = "Böblingen",
        administrativeAreaLevel1Verbose = null,
        administrativeAreaLevel2 = "Böblingen",
        administrativeAreaLevel3 = null,
        district = "Sindelfingen-Ost",
        companyPostalCode = "70547",
        industrialZone = "Industrial Zone One",
        building = "Gebäude eins",
        floor = "Stockerk eins",
        door = "Raum eins",
        street = StreetDto(
            name = "Bela-Barenyi-Straße",
            houseNumber = "1",
            houseNumberSupplement = "A",
            milestone = "milestone 1",
            direction = "direction 1",
            nameSuffix = "name suffix 1",
            namePrefix = "name prefix 1",
            additionalNameSuffix = "add name suffix 1",
            additionalNamePrefix = "add name prefix 1"
        ),
        taxJurisdictionCode = "123"
    )

    val address2 = PhysicalPostalAddressVerboseDto(
        geographicCoordinates = null,
        countryVerbose = country2,
        postalCode = "70547",
        city = "Atlanta",
        administrativeAreaLevel1Verbose = null,
        administrativeAreaLevel2 = " Fulton County",
        administrativeAreaLevel3 = null,
        district = "District Level 1",
        companyPostalCode = null,
        industrialZone = "Industrial Zone Two",
        building = "Building Two",
        floor = "Floor Two",
        door = "Door Two",
        street = StreetDto(
            name = "Street2",
            houseNumber = "2",
            houseNumberSupplement = "B",
            milestone = "milestone 2",
            direction = "direction 2",
            nameSuffix = "name suffix 2",
            namePrefix = "name prefix 2",
            additionalNameSuffix = "add name suffix 2",
            additionalNamePrefix = "add name prefix 2"
        ),
        taxJurisdictionCode = "456"
    )

    val address3 = PhysicalPostalAddressVerboseDto(
        geographicCoordinates = null,
        countryVerbose = country3,
        postalCode = "30346",
        city = "Atlanta",
        administrativeAreaLevel1Verbose = null,
        administrativeAreaLevel2 = " Fulton County",
        administrativeAreaLevel3 = null,
        district = "DL 1",
        companyPostalCode = null,
        industrialZone = "Industrial Zone Three",
        building = "tedifício  três",
        floor = "piso três",
        door = "peça três",
        street = StreetDto(
            name = "Street 3",
            houseNumber = "3",
            houseNumberSupplement = "C",
            milestone = "milestone 3",
            direction = "direction 3",
            nameSuffix = "name suffix 3",
            namePrefix = "name prefix 3",
            additionalNameSuffix = "add name suffix 3",
            additionalNamePrefix = "add name prefix 3"
        ),
        taxJurisdictionCode = "789"
    )

    val addressPartner1 = LogisticAddressVerboseDto(
        bpna = firstBpnA,
        physicalPostalAddress = address1,
        bpnLegalEntity = null,
        bpnSite = null,
        confidenceCriteria = confidenceCriteria1,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        addressType = AddressType.AdditionalAddress,
        isParticipantData = false
    )

    val addressPartner2 = LogisticAddressVerboseDto(
        bpna = secondBpnA,
        physicalPostalAddress = address2,
        bpnLegalEntity = null,
        bpnSite = null,
        confidenceCriteria = confidenceCriteria2,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        addressType = AddressType.AdditionalAddress,
        isParticipantData = false
    )

    val addressPartner3 = LogisticAddressVerboseDto(
        bpna = thirdBpnA,
        physicalPostalAddress = address3,
        bpnLegalEntity = null,
        bpnSite = null,
        confidenceCriteria = confidenceCriteria3,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        addressType = AddressType.AdditionalAddress,
        isParticipantData = false
    )

    val addressPartnerCreate1 = AddressPartnerCreateVerboseDto(
        address = addressPartner1,
        index = "1"
    )

    val addressPartnerCreate2 = AddressPartnerCreateVerboseDto(
        address = addressPartner2,
        index = "2"
    )

    val addressPartnerCreate3 = AddressPartnerCreateVerboseDto(
        address = addressPartner3,
        index = "3"
    )

    val site1 = SiteVerboseDto(
        bpns = firstBpnS,
        name = "Stammwerk A",
        states = listOf(siteStatus1),
        bpnLegalEntity = firstBpnL,
        confidenceCriteria = confidenceCriteria1,
        isParticipantData = false,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    val site2 = SiteVerboseDto(
        bpns = secondBpnS,
        name = "Halle B1",
        states = listOf(siteStatus2),
        bpnLegalEntity = secondBpnL,
        confidenceCriteria = confidenceCriteria2,
        isParticipantData = false,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    val site3 = SiteVerboseDto(
        bpns = thirdBpnS,
        name = "主厂房 C",
        states = listOf(siteStatus3),
        bpnLegalEntity = thirdBpnl,
        confidenceCriteria = confidenceCriteria3,
        isParticipantData = false,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    val siteUpsert1 = SitePartnerCreateVerboseDto(
        site = site1,
        mainAddress = addressPartner1.copy(
            bpnSite = site1.bpns,
            addressType = AddressType.SiteMainAddress
        ),
        index = "1"
    )

    val createSiteLegalReference1 = SitePartnerCreateVerboseDto(
        site = site1,
        mainAddress = addressPartner1.copy(
            bpnSite = site1.bpns,
            addressType = AddressType.LegalAndSiteMainAddress
        ),
        index = "1"
    )

    val siteUpsert2 = SitePartnerCreateVerboseDto(
        site = site2,
        mainAddress = addressPartner2.copy(
            bpnSite = site2.bpns,
            addressType = AddressType.SiteMainAddress
        ),
        index = "2"
    )

    val createSiteLegalReference2 = SitePartnerCreateVerboseDto(
        site = site2,
        mainAddress = addressPartner2.copy(
            bpnSite = site2.bpns,
            addressType = AddressType.LegalAndSiteMainAddress
        ),
        index = "2"
    )

    val siteUpsert3 = SitePartnerCreateVerboseDto(
        site = site3,
        mainAddress = addressPartner3.copy(
            bpnSite = site3.bpns,
            addressType = AddressType.SiteMainAddress
        ),
        index = "3"
    )


    val legalEntity1 = LegalEntityWithLegalAddressVerboseDto(
        LegalEntityVerboseDto(
            bpnl = firstBpnL,
            legalName = "Business Partner Name",
            legalFormVerbose = legalForm1,
            identifiers = listOf(identifier1),
            states = listOf(leStatus1),
            currentness = createdTime1.toInstant(ZoneOffset.UTC),
            confidenceCriteria = confidenceCriteria1,
            isParticipantData = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        ),
        legalAddress = LogisticAddressVerboseDto(
            bpna = firstBpnA,
            physicalPostalAddress = PhysicalPostalAddressVerboseDto(
                geographicCoordinates = null,
                countryVerbose = country1,
                postalCode = null,
                city = "Stuttgart",
                administrativeAreaLevel1Verbose = null,
                administrativeAreaLevel2 = null,
                administrativeAreaLevel3 = null,
                district = null,
                companyPostalCode = null,
                industrialZone = null,
                building = null,
                floor = null,
                door = null,
                street = null,
                taxJurisdictionCode = null
            ),
            bpnLegalEntity = null,
            bpnSite = null,
            confidenceCriteria = confidenceCriteria1,
            isParticipantData = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    )

    val legalEntity2 = LegalEntityWithLegalAddressVerboseDto(
        LegalEntityVerboseDto(
            bpnl = secondBpnL,
            legalName = "Another Organisation Corp",
            legalFormVerbose = legalForm2,
            identifiers = listOf(identifier2),
            states = listOf(leStatus2),
            currentness = createdTime1.toInstant(ZoneOffset.UTC),
            confidenceCriteria = confidenceCriteria2,
            isParticipantData = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        ),
        legalAddress = LogisticAddressVerboseDto(
            bpna = firstBpnA,
            physicalPostalAddress = PhysicalPostalAddressVerboseDto(
                geographicCoordinates = null,
                countryVerbose = country2,
                postalCode = null,
                city = "5th Congressional District",
                administrativeAreaLevel1Verbose = null,
                administrativeAreaLevel2 = null,
                administrativeAreaLevel3 = null,
                district = null,
                companyPostalCode = null,
                industrialZone = null,
                building = null,
                floor = null,
                door = null,
                street = null,
                taxJurisdictionCode = null
            ),
            bpnLegalEntity = null,
            bpnSite = null,
            confidenceCriteria = confidenceCriteria2,
            isParticipantData = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    )

    val legalEntity3 = LegalEntityWithLegalAddressVerboseDto(
        LegalEntityVerboseDto(
            bpnl = thirdBpnl,
            legalName = "好公司  合伙制企业",
            legalFormVerbose = legalForm3,
            identifiers = listOf(identifier3),
            states = listOf(leStatus3),
            currentness = createdTime1.toInstant(ZoneOffset.UTC),
            confidenceCriteria = confidenceCriteria3,
            isParticipantData = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        ),
        legalAddress = LogisticAddressVerboseDto(
            bpna = firstBpnA,
            physicalPostalAddress = PhysicalPostalAddressVerboseDto(
                geographicCoordinates = null,
                countryVerbose = country3,
                postalCode = null,
                city = "北京市",
                administrativeAreaLevel1Verbose = null,
                administrativeAreaLevel2 = null,
                administrativeAreaLevel3 = null,
                district = null,
                companyPostalCode = null,
                industrialZone = null,
                building = null,
                floor = null,
                door = null,
                street = null,
                taxJurisdictionCode = null
            ),
            bpnLegalEntity = null,
            bpnSite = null,
            confidenceCriteria = confidenceCriteria3,
            isParticipantData = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    )

    val legalEntityUpsert1 = LegalEntityPartnerCreateVerboseDto(
        legalEntity = LegalEntityVerboseDto(
            bpnl = firstBpnL,
            legalName = "Business Partner Name",
            legalFormVerbose = legalForm1,
            identifiers = listOf(LegalEntityIdentifierVerboseDto("ID-XYZ", identifierType1, "Agency X")),
            states = listOf(leStatus1),
            currentness = createdTime1.toInstant(ZoneOffset.UTC),
            confidenceCriteria = confidenceCriteria1,
            isParticipantData = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        ),
        legalAddress = addressPartner1.copy(
            bpnLegalEntity = legalEntity1.legalEntity.bpnl,
            addressType = AddressType.LegalAddress
        ),
        index = "1"
    )

    val legalEntityUpsert2 = LegalEntityPartnerCreateVerboseDto(
        legalEntity = LegalEntityVerboseDto(
            bpnl = secondBpnL,
            legalName = "Another Organisation Corp",
            legalFormVerbose = legalForm2,
            identifiers = listOf(LegalEntityIdentifierVerboseDto("Another ID Value", identifierType2, "Body Y")),
            states = listOf(leStatus2),
            currentness = createdTime1.toInstant(ZoneOffset.UTC),
            confidenceCriteria = confidenceCriteria2,
            isParticipantData = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        ),
        legalAddress = addressPartner2.copy(
            bpnLegalEntity = legalEntity2.legalEntity.bpnl,
            addressType = AddressType.LegalAddress
        ),
        index = "2"
    )

    val legalEntityUpsert3 = LegalEntityPartnerCreateVerboseDto(
        legalEntity = LegalEntityVerboseDto(
            bpnl = thirdBpnl,
            legalName = "好公司  合伙制企业",
            legalFormVerbose = legalForm3,
            identifiers = listOf(LegalEntityIdentifierVerboseDto("An ID Value", identifierType3, "Official Z")),
            states = listOf(leStatus3),
            currentness = createdTime1.toInstant(ZoneOffset.UTC),
            confidenceCriteria = confidenceCriteria3,
            isParticipantData = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        ),
        legalAddress = addressPartner3.copy(
            bpnLegalEntity = legalEntity3.legalEntity.bpnl,
            addressType = AddressType.LegalAddress
        ),
        index = "3"
    )

    val legalEntityUpsertMultipleIdentifier = LegalEntityPartnerCreateVerboseDto(
        legalEntity = LegalEntityVerboseDto(
            bpnl = firstBpnL,
            legalName = "Business Partner Name",
            legalFormVerbose = legalForm1,
            identifiers = listOf(
                LegalEntityIdentifierVerboseDto("ID-XYZ", identifierType1, "Agency X"),
                LegalEntityIdentifierVerboseDto("Another ID Value", identifierType2, "Body Y")
            ),
            states = listOf(leStatus1),
            currentness = createdTime1.toInstant(ZoneOffset.UTC),
            confidenceCriteria = confidenceCriteria1,
            isParticipantData = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        ),
        legalAddress = addressPartner1.copy(
            bpnLegalEntity = legalEntity1.legalEntity.bpnl,
        ),
        index = "1"
    )

}