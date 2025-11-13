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

import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.api.model.request.*

object BusinessPartnerNonVerboseValues {

    val identifierType1 = BusinessPartnerVerboseValues.identifierType1
    val identifierType2 = BusinessPartnerVerboseValues.identifierType2
    val identifierType3 = BusinessPartnerVerboseValues.identifierType3

    val identifierTypeDto1 =
        IdentifierTypeDto(BusinessPartnerVerboseValues.identifierType1.technicalKey, IdentifierBusinessPartnerType.LEGAL_ENTITY, BusinessPartnerVerboseValues.identifierType1.name, BusinessPartnerVerboseValues.identifierTypeAbbreviation1, BusinessPartnerVerboseValues.identifierTypeTransliteratedName1, BusinessPartnerVerboseValues.identifierTypeTransliteratedAbbreviation1, BusinessPartnerVerboseValues.identifierTypeFormat1, BusinessPartnerVerboseValues.identifierTypeCategories1)

    val addressIdentifierTypeDto1 =
        IdentifierTypeDto(
            "FR_SIRET",
            IdentifierBusinessPartnerType.ADDRESS,
            "Numéro du système d'identification du répertoire des établissements",
            BusinessPartnerVerboseValues.addressIdentifierTypeAbbreviation1,
            BusinessPartnerVerboseValues.addressIdentifierTypeTransliteratedName1,
            BusinessPartnerVerboseValues.addressIdentifierTypeTransliteratedAbbreviation1,
            "^\\d{8}\\d{1}\\d{5}$",
            sortedSetOf(IdentifierTypeCategory.NBR)
        )
    val addressIdentifierTypeDto2 = addressIdentifierTypeDto1

    val identifier1 = LegalEntityIdentifierDto(
        value = BusinessPartnerVerboseValues.identifier1.value,
        type = BusinessPartnerVerboseValues.identifierType1.technicalKey,
        issuingBody = BusinessPartnerVerboseValues.identifier1.issuingBody,
    )

    val identifier2 = LegalEntityIdentifierDto(
        value = BusinessPartnerVerboseValues.identifier2.value,
        type = BusinessPartnerVerboseValues.identifierType2.technicalKey,
        issuingBody = BusinessPartnerVerboseValues.identifier2.issuingBody,
    )

    val identifier3 = LegalEntityIdentifierDto(
        value = BusinessPartnerVerboseValues.identifier3.value,
        type = BusinessPartnerVerboseValues.identifierType3.technicalKey,
        issuingBody = BusinessPartnerVerboseValues.identifier3.issuingBody,
    )

    val addressIdentifier1 = AddressIdentifierDto(
        value = "Address Identifier 1",
        type = addressIdentifierTypeDto1.technicalKey,
    )

    val addressIdentifier2 = AddressIdentifierDto(
        value = "Address Identifier 2",
        type = addressIdentifierTypeDto2.technicalKey,
    )


    val addressIdentifier = AddressIdentifierDto(
        value = BusinessPartnerVerboseValues.identifier3.value,
        type = addressIdentifierTypeDto1.technicalKey,
    )

    val legalForm1 = LegalFormRequest(
        technicalKey = BusinessPartnerVerboseValues.legalForm1.technicalKey,
        name = BusinessPartnerVerboseValues.legalForm1.name,
        transliteratedName = BusinessPartnerVerboseValues.legalForm1.transliteratedName,
        abbreviations = BusinessPartnerVerboseValues.legalForm1.abbreviations,
        transliteratedAbbreviations = BusinessPartnerVerboseValues.legalForm1.transliteratedAbbreviations,
        country = BusinessPartnerVerboseValues.legalForm1.country,
        language = BusinessPartnerVerboseValues.legalForm1.language,
        administrativeAreaLevel1 = BusinessPartnerVerboseValues.legalForm1.administrativeAreaLevel1,
        isActive = BusinessPartnerVerboseValues.legalForm1.isActive
    )
    val legalForm2 = LegalFormRequest(
        technicalKey = BusinessPartnerVerboseValues.legalForm2.technicalKey,
        name = BusinessPartnerVerboseValues.legalForm2.name,
        transliteratedName = BusinessPartnerVerboseValues.legalForm2.transliteratedName,
        abbreviations = BusinessPartnerVerboseValues.legalForm2.abbreviations,
        transliteratedAbbreviations = BusinessPartnerVerboseValues.legalForm2.transliteratedAbbreviations,
        country = BusinessPartnerVerboseValues.legalForm2.country,
        language = BusinessPartnerVerboseValues.legalForm2.language,
        administrativeAreaLevel1 = BusinessPartnerVerboseValues.legalForm2.administrativeAreaLevel1,
        isActive = BusinessPartnerVerboseValues.legalForm2.isActive
    )

    private val leStatus1 = LegalEntityStateDto(
        BusinessPartnerVerboseValues.leStatus1.validFrom,
        BusinessPartnerVerboseValues.leStatus1.validTo,
        BusinessPartnerVerboseValues.statusType1.technicalKey
    )
    private val leStatus2 = LegalEntityStateDto(
        BusinessPartnerVerboseValues.leStatus2.validFrom,
        BusinessPartnerVerboseValues.leStatus2.validTo,
        BusinessPartnerVerboseValues.statusType2.technicalKey
    )
    private val leStatus3 = LegalEntityStateDto(
        BusinessPartnerVerboseValues.leStatus3.validFrom,
        BusinessPartnerVerboseValues.leStatus3.validTo,
        BusinessPartnerVerboseValues.statusType3.technicalKey
    )

    val siteStatus1 = SiteStateDto(
        BusinessPartnerVerboseValues.siteStatus1.validFrom,
        BusinessPartnerVerboseValues.siteStatus1.validTo,
        BusinessPartnerVerboseValues.statusType1.technicalKey
    )
    private val siteStatus2 = SiteStateDto(
        BusinessPartnerVerboseValues.siteStatus2.validFrom,
        BusinessPartnerVerboseValues.siteStatus2.validTo,
        BusinessPartnerVerboseValues.statusType2.technicalKey
    )
    private val siteStatus3 = SiteStateDto(
        BusinessPartnerVerboseValues.siteStatus3.validFrom,
        BusinessPartnerVerboseValues.siteStatus3.validTo,
        BusinessPartnerVerboseValues.statusType3.technicalKey
    )

    private val postalAddress1 = PhysicalPostalAddressDto(
        geographicCoordinates = BusinessPartnerVerboseValues.address1.geographicCoordinates,
        country = BusinessPartnerVerboseValues.address1.country,
        postalCode = BusinessPartnerVerboseValues.address1.postalCode,
        city = BusinessPartnerVerboseValues.address1.city,
        administrativeAreaLevel1 = BusinessPartnerVerboseValues.address1.administrativeAreaLevel1,
        administrativeAreaLevel2 = BusinessPartnerVerboseValues.address1.administrativeAreaLevel2,
        administrativeAreaLevel3 = BusinessPartnerVerboseValues.address1.administrativeAreaLevel3,
        district = BusinessPartnerVerboseValues.address1.district,
        companyPostalCode = BusinessPartnerVerboseValues.address1.companyPostalCode,
        industrialZone = BusinessPartnerVerboseValues.address1.industrialZone,
        building = BusinessPartnerVerboseValues.address1.building,
        floor = BusinessPartnerVerboseValues.address1.floor,
        door = BusinessPartnerVerboseValues.address1.door,
        street = BusinessPartnerVerboseValues.address1.street,
        taxJurisdictionCode = BusinessPartnerVerboseValues.address1.taxJurisdictionCode
    )

    private val postalAddress2 = PhysicalPostalAddressDto(
        geographicCoordinates = BusinessPartnerVerboseValues.address2.geographicCoordinates,
        country = BusinessPartnerVerboseValues.address2.country,
        postalCode = BusinessPartnerVerboseValues.address2.postalCode,
        city = BusinessPartnerVerboseValues.address2.city,
        administrativeAreaLevel1 = BusinessPartnerVerboseValues.address2.administrativeAreaLevel1,
        administrativeAreaLevel2 = BusinessPartnerVerboseValues.address2.administrativeAreaLevel2,
        administrativeAreaLevel3 = BusinessPartnerVerboseValues.address2.administrativeAreaLevel3,
        district = BusinessPartnerVerboseValues.address2.district,
        companyPostalCode = BusinessPartnerVerboseValues.address2.companyPostalCode,
        industrialZone = BusinessPartnerVerboseValues.address2.industrialZone,
        building = BusinessPartnerVerboseValues.address2.building,
        floor = BusinessPartnerVerboseValues.address2.floor,
        door = BusinessPartnerVerboseValues.address2.door,
        street = BusinessPartnerVerboseValues.address2.street,
        taxJurisdictionCode = BusinessPartnerVerboseValues.address2.taxJurisdictionCode
    )

    private val postalAddress3 = PhysicalPostalAddressDto(
        geographicCoordinates = BusinessPartnerVerboseValues.address3.geographicCoordinates,
        country = BusinessPartnerVerboseValues.address3.country,
        postalCode = BusinessPartnerVerboseValues.address3.postalCode,
        city = BusinessPartnerVerboseValues.address3.city,
        administrativeAreaLevel1 = BusinessPartnerVerboseValues.address3.administrativeAreaLevel1,
        administrativeAreaLevel2 = BusinessPartnerVerboseValues.address3.administrativeAreaLevel2,
        administrativeAreaLevel3 = BusinessPartnerVerboseValues.address3.administrativeAreaLevel3,
        district = BusinessPartnerVerboseValues.address3.district,
        companyPostalCode = BusinessPartnerVerboseValues.address3.companyPostalCode,
        industrialZone = BusinessPartnerVerboseValues.address3.industrialZone,
        building = BusinessPartnerVerboseValues.address3.building,
        floor = BusinessPartnerVerboseValues.address3.floor,
        door = BusinessPartnerVerboseValues.address3.door,
        street = BusinessPartnerVerboseValues.address3.street,
        taxJurisdictionCode = BusinessPartnerVerboseValues.address3.taxJurisdictionCode
    )

    val logisticAddress1 = LogisticAddressDto(
        physicalPostalAddress = postalAddress1,
        confidenceCriteria = BusinessPartnerVerboseValues.addressPartner1.confidenceCriteria
    )

    val logisticAddress2 = LogisticAddressDto(
        physicalPostalAddress = postalAddress2,
        confidenceCriteria = BusinessPartnerVerboseValues.addressPartner2.confidenceCriteria
    )

    val logisticAddress3 = LogisticAddressDto(
        physicalPostalAddress = postalAddress3,
        confidenceCriteria = BusinessPartnerVerboseValues.addressPartner3.confidenceCriteria
    )
    val logisticAddress4 = LogisticAddressDto(
        physicalPostalAddress = postalAddress1,
        name = BusinessPartnerVerboseValues.legalEntityUpsert1.legalEntity.legalName,
        confidenceCriteria = BusinessPartnerVerboseValues.addressPartner1.confidenceCriteria
    )

    val logisticAddress5 = LogisticAddressDto(
        physicalPostalAddress = postalAddress1,
        identifiers = listOf(addressIdentifier),
        confidenceCriteria = BusinessPartnerVerboseValues.addressPartner1.confidenceCriteria
    )

    val legalEntityCreate1 = LegalEntityPartnerCreateRequest(
        legalEntity = LegalEntityDto(
            legalName = BusinessPartnerVerboseValues.legalEntityUpsert1.legalEntity.legalName,
            legalShortName = null,
            legalForm = BusinessPartnerVerboseValues.legalForm1.technicalKey,
            identifiers = listOf(identifier1),
            states = listOf(leStatus1),
            confidenceCriteria = BusinessPartnerVerboseValues.legalEntity1.legalEntity.confidenceCriteria,
            isParticipantData = false
        ),
        legalAddress = logisticAddress1,
        index = BusinessPartnerVerboseValues.legalEntityUpsert1.index
    )

    val legalEntityCreate2 = LegalEntityPartnerCreateRequest(
        legalEntity = LegalEntityDto(
            legalName = BusinessPartnerVerboseValues.legalEntityUpsert2.legalEntity.legalName,
            legalShortName = null,
            legalForm = BusinessPartnerVerboseValues.legalForm2.technicalKey,
            identifiers = listOf(identifier2),
            states = listOf(leStatus2),
            confidenceCriteria = BusinessPartnerVerboseValues.legalEntity2.legalEntity.confidenceCriteria,
            isParticipantData = false
        ),
        legalAddress = logisticAddress2,
        index = BusinessPartnerVerboseValues.legalEntityUpsert2.index
    )

    val legalEntityCreate3 = LegalEntityPartnerCreateRequest(
        legalEntity = LegalEntityDto(
            legalName = BusinessPartnerVerboseValues.legalEntityUpsert3.legalEntity.legalName,
            legalShortName = null,
            legalForm = BusinessPartnerVerboseValues.legalForm3.technicalKey,
            identifiers = listOf(identifier3),
            states = listOf(leStatus3),
            confidenceCriteria = BusinessPartnerVerboseValues.legalEntity3.legalEntity.confidenceCriteria,
            isParticipantData = false
        ),
        legalAddress = logisticAddress3,
        index = BusinessPartnerVerboseValues.legalEntityUpsert3.index
    )

    val legalEntityUpdate1 = LegalEntityPartnerUpdateRequest(
        bpnl = BusinessPartnerVerboseValues.legalEntityUpsert1.legalEntity.bpnl,
        legalEntity = legalEntityCreate1.legalEntity,
        legalAddress = legalEntityCreate1.legalAddress,
    )

    val legalEntityUpdate2 = LegalEntityPartnerUpdateRequest(
        bpnl = BusinessPartnerVerboseValues.legalEntityUpsert2.legalEntity.bpnl,
        legalEntity = legalEntityCreate2.legalEntity,
        legalAddress = legalEntityCreate2.legalAddress,
    )

    val legalEntityUpdate3 = LegalEntityPartnerUpdateRequest(
        bpnl = BusinessPartnerVerboseValues.legalEntityUpsert3.legalEntity.bpnl,
        legalEntity = legalEntityCreate3.legalEntity,
        legalAddress = legalEntityCreate3.legalAddress,
    )

    val siteCreate1 = SitePartnerCreateRequest(
        site = SiteDto(
            name = BusinessPartnerVerboseValues.siteUpsert1.site.name,
            states = listOf(siteStatus1),
            mainAddress = logisticAddress1,
            confidenceCriteria = BusinessPartnerVerboseValues.site1.confidenceCriteria
        ),
        index = BusinessPartnerVerboseValues.siteUpsert1.index,
        bpnlParent = legalEntityUpdate1.bpnl
    )

    val siteLegalReferenceUpsert1=SiteCreateRequestWithLegalAddressAsMain(
        name = BusinessPartnerVerboseValues.siteUpsert1.site.name,
        bpnLParent = legalEntityUpdate1.bpnl,
        confidenceCriteria = BusinessPartnerVerboseValues.site1.confidenceCriteria,
        states = listOf(siteStatus1)
    )

    val siteLegalReferenceUpsert2=SiteCreateRequestWithLegalAddressAsMain(
        name = BusinessPartnerVerboseValues.siteUpsert2.site.name,
        bpnLParent = legalEntityUpdate2.bpnl,
        confidenceCriteria = BusinessPartnerVerboseValues.site2.confidenceCriteria,
        states = listOf(siteStatus2)
    )

    val siteCreate2 = SitePartnerCreateRequest(
        site = SiteDto(
            name = BusinessPartnerVerboseValues.siteUpsert2.site.name,
            states = listOf(siteStatus2),
            mainAddress = logisticAddress2,
            confidenceCriteria = BusinessPartnerVerboseValues.site2.confidenceCriteria
        ),
        index = BusinessPartnerVerboseValues.siteUpsert2.index,
        bpnlParent = legalEntityUpdate2.bpnl
    )

    val siteCreate3 = SitePartnerCreateRequest(
        site = SiteDto(
            name = BusinessPartnerVerboseValues.siteUpsert3.site.name,
            states = listOf(siteStatus3),
            mainAddress = logisticAddress3,
            confidenceCriteria = BusinessPartnerVerboseValues.site3.confidenceCriteria
        ),
        index = BusinessPartnerVerboseValues.siteUpsert3.index,
        bpnlParent = legalEntityUpdate3.bpnl
    )

    val siteUpdate1 = SitePartnerUpdateRequest(
        bpns = BusinessPartnerVerboseValues.siteUpsert1.site.bpns,
        site = siteCreate1.site
    )

    val siteUpdate2 = SitePartnerUpdateRequest(
        bpns = BusinessPartnerVerboseValues.siteUpsert2.site.bpns,
        site = siteCreate2.site
    )

    val siteUpdate3 = SitePartnerUpdateRequest(
        bpns = BusinessPartnerVerboseValues.siteUpsert3.site.bpns,
        site = siteCreate3.site
    )

    val addressPartnerCreate1 = AddressPartnerCreateRequest(
        address = logisticAddress1,
        bpnParent = legalEntityUpdate1.bpnl,
        index = BusinessPartnerVerboseValues.addressPartnerCreate1.index
    )

    val addressPartnerCreate2 = AddressPartnerCreateRequest(
        address = logisticAddress2,
        bpnParent = legalEntityUpdate2.bpnl,
        index = BusinessPartnerVerboseValues.addressPartnerCreate2.index
    )

    val addressPartnerCreate3 = AddressPartnerCreateRequest(
        address = logisticAddress3,
        bpnParent = legalEntityUpdate3.bpnl,
        index = BusinessPartnerVerboseValues.addressPartnerCreate3.index
    )

    val addressPartnerCreate4 = AddressPartnerCreateRequest(
        address = logisticAddress4,
        bpnParent = legalEntityUpdate3.bpnl,
        index = BusinessPartnerVerboseValues.addressPartnerCreate3.index
    )

    val addressPartnerCreate5 = AddressPartnerCreateRequest(
        address = logisticAddress5,
        bpnParent = legalEntityUpdate3.bpnl,
        index = BusinessPartnerVerboseValues.addressPartnerCreate3.index
    )


    val addressPartnerUpdate1 = AddressPartnerUpdateRequest(
        bpna = BusinessPartnerVerboseValues.addressPartner1.bpna,
        address = logisticAddress1
    )

    val addressPartnerUpdate2 = AddressPartnerUpdateRequest(
        bpna = BusinessPartnerVerboseValues.addressPartner2.bpna,
        address = logisticAddress2
    )

    val addressPartnerUpdate3 = AddressPartnerUpdateRequest(
        bpna = BusinessPartnerVerboseValues.addressPartner3.bpna,
        address = logisticAddress3
    )

    val partnerStructure1 = LegalEntityStructureRequest(
        legalEntityCreate1,
        listOf(SiteStructureRequest(siteCreate1, listOf(addressPartnerCreate1)))
    )
}