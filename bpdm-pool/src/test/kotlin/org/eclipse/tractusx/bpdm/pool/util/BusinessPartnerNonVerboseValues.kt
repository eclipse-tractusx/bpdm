/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.api.model.request.*

object BusinessPartnerNonVerboseValues {

    val identifierType1 = BusinessPartnerVerboseValues.identifierType1
    val identifierType2 = BusinessPartnerVerboseValues.identifierType2
    val identifierType3 = BusinessPartnerVerboseValues.identifierType3

    val identifierTypeDto1 =
        IdentifierTypeDto(BusinessPartnerVerboseValues.identifierType1.technicalKey, IdentifierBusinessPartnerType.LEGAL_ENTITY, BusinessPartnerVerboseValues.identifierType1.name)
    val identifierTypeDto2 =
        IdentifierTypeDto(BusinessPartnerVerboseValues.identifierType2.technicalKey, IdentifierBusinessPartnerType.LEGAL_ENTITY, BusinessPartnerVerboseValues.identifierType2.name)
    val identifierTypeDto3 =
        IdentifierTypeDto(BusinessPartnerVerboseValues.identifierType3.technicalKey, IdentifierBusinessPartnerType.LEGAL_ENTITY, BusinessPartnerVerboseValues.identifierType3.name)

    val addressIdentifierTypeDto1 =
        IdentifierTypeDto("ADDR_KEY_ONE", IdentifierBusinessPartnerType.ADDRESS, "IdentNameOne")
    val addressIdentifierTypeDto2 =
        IdentifierTypeDto("ADDR_KEY_TWO", IdentifierBusinessPartnerType.ADDRESS, "IdentNameTwo")
    val addressIdentifierTypeDto3 =
        IdentifierTypeDto("ADDR_KEY_THREE", IdentifierBusinessPartnerType.ADDRESS, "IdentNameThree")


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
        value = addressIdentifierTypeDto1.name,
        type = addressIdentifierTypeDto1.technicalKey,
    )

    val addressIdentifier2 = AddressIdentifierDto(
        value = addressIdentifierTypeDto2.name,
        type = addressIdentifierTypeDto2.technicalKey,
    )


    val addressIdentifier = AddressIdentifierDto(
        value = BusinessPartnerVerboseValues.identifier3.value,
        type = BusinessPartnerVerboseValues.identifierType3.technicalKey,
    )

    val legalForm1 = LegalFormRequest(
        technicalKey = BusinessPartnerVerboseValues.legalForm1.technicalKey,
        name = BusinessPartnerVerboseValues.legalForm1.name,
        abbreviation = BusinessPartnerVerboseValues.legalForm1.abbreviation,
    )
    val legalForm2 = LegalFormRequest(
        technicalKey = BusinessPartnerVerboseValues.legalForm2.technicalKey,
        name = BusinessPartnerVerboseValues.legalForm2.name,
        abbreviation = BusinessPartnerVerboseValues.legalForm2.abbreviation,
    )
    val legalForm3 = LegalFormRequest(
        technicalKey = BusinessPartnerVerboseValues.legalForm3.technicalKey,
        name = BusinessPartnerVerboseValues.legalForm3.name,
        abbreviation = BusinessPartnerVerboseValues.legalForm3.abbreviation,
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

    private val classification1 =
        LegalEntityClassificationDto(
            BusinessPartnerVerboseValues.classificationType.technicalKey,
            BusinessPartnerVerboseValues.classification1.code,
            BusinessPartnerVerboseValues.classification1.value
        )
    private val classification2 =
        LegalEntityClassificationDto(
            BusinessPartnerVerboseValues.classificationType.technicalKey,
            BusinessPartnerVerboseValues.classification2.code,
            BusinessPartnerVerboseValues.classification2.value
        )
    private val classification3 =
        LegalEntityClassificationDto(
            BusinessPartnerVerboseValues.classificationType.technicalKey,
            BusinessPartnerVerboseValues.classification3.code,
            BusinessPartnerVerboseValues.classification3.value
        )
    private val classification4 =
        LegalEntityClassificationDto(
            BusinessPartnerVerboseValues.classificationType.technicalKey,
            BusinessPartnerVerboseValues.classification4.code,
            BusinessPartnerVerboseValues.classification4.value
        )
    private val classification5 =
        LegalEntityClassificationDto(
            BusinessPartnerVerboseValues.classificationType.technicalKey,
            BusinessPartnerVerboseValues.classification5.code,
            BusinessPartnerVerboseValues.classification5.value
        )


    private val postalAddress1 = PhysicalPostalAddressDto(
        geographicCoordinates = BusinessPartnerVerboseValues.address1.geographicCoordinates,
        country = BusinessPartnerVerboseValues.address1.country,
        postalCode = BusinessPartnerVerboseValues.address1.postalCode,
        city = BusinessPartnerVerboseValues.address1.city,
        administrativeAreaLevel1 = BusinessPartnerVerboseValues.address1.administrativeAreaLevel1?.toString(),
        administrativeAreaLevel2 = BusinessPartnerVerboseValues.address1.administrativeAreaLevel2,
        administrativeAreaLevel3 = BusinessPartnerVerboseValues.address1.administrativeAreaLevel3,
        district = BusinessPartnerVerboseValues.address1.district,
        companyPostalCode = BusinessPartnerVerboseValues.address1.companyPostalCode,
        industrialZone = BusinessPartnerVerboseValues.address1.industrialZone,
        building = BusinessPartnerVerboseValues.address1.building,
        floor = BusinessPartnerVerboseValues.address1.floor,
        door = BusinessPartnerVerboseValues.address1.door,
        street = BusinessPartnerVerboseValues.address1.street,
    )

    private val postalAddress2 = PhysicalPostalAddressDto(
        geographicCoordinates = BusinessPartnerVerboseValues.address2.geographicCoordinates,
        country = BusinessPartnerVerboseValues.address2.country,
        postalCode = BusinessPartnerVerboseValues.address2.postalCode,
        city = BusinessPartnerVerboseValues.address2.city,
        administrativeAreaLevel1 = BusinessPartnerVerboseValues.address2.administrativeAreaLevel1?.toString(),
        administrativeAreaLevel2 = BusinessPartnerVerboseValues.address2.administrativeAreaLevel2,
        administrativeAreaLevel3 = BusinessPartnerVerboseValues.address2.administrativeAreaLevel3,
        district = BusinessPartnerVerboseValues.address2.district,
        companyPostalCode = BusinessPartnerVerboseValues.address2.companyPostalCode,
        industrialZone = BusinessPartnerVerboseValues.address2.industrialZone,
        building = BusinessPartnerVerboseValues.address2.building,
        floor = BusinessPartnerVerboseValues.address2.floor,
        door = BusinessPartnerVerboseValues.address2.door,
        street = BusinessPartnerVerboseValues.address2.street,
    )

    private val postalAddress3 = PhysicalPostalAddressDto(
        geographicCoordinates = BusinessPartnerVerboseValues.address3.geographicCoordinates,
        country = BusinessPartnerVerboseValues.address3.country,
        postalCode = BusinessPartnerVerboseValues.address3.postalCode,
        city = BusinessPartnerVerboseValues.address3.city,
        administrativeAreaLevel1 = BusinessPartnerVerboseValues.address3.administrativeAreaLevel1?.toString(),
        administrativeAreaLevel2 = BusinessPartnerVerboseValues.address3.administrativeAreaLevel2,
        administrativeAreaLevel3 = BusinessPartnerVerboseValues.address3.administrativeAreaLevel3,
        district = BusinessPartnerVerboseValues.address3.district,
        companyPostalCode = BusinessPartnerVerboseValues.address3.companyPostalCode,
        industrialZone = BusinessPartnerVerboseValues.address3.industrialZone,
        building = BusinessPartnerVerboseValues.address3.building,
        floor = BusinessPartnerVerboseValues.address3.floor,
        door = BusinessPartnerVerboseValues.address3.door,
        street = BusinessPartnerVerboseValues.address3.street,
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
            classifications = listOf(classification1, classification2),
            confidenceCriteria = BusinessPartnerVerboseValues.legalEntity1.legalEntity.confidenceCriteria
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
            classifications = listOf(classification3, classification4),
            confidenceCriteria = BusinessPartnerVerboseValues.legalEntity2.legalEntity.confidenceCriteria
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
            classifications = listOf(classification5),
            confidenceCriteria = BusinessPartnerVerboseValues.legalEntity3.legalEntity.confidenceCriteria
        ),
        legalAddress = logisticAddress3,
        index = BusinessPartnerVerboseValues.legalEntityUpsert3.index
    )

    val legalEntityCreateMultipleIdentifier = LegalEntityPartnerCreateRequest(
        legalEntity = LegalEntityDto(
            legalName = BusinessPartnerVerboseValues.legalEntityUpsertMultipleIdentifier.legalEntity.legalName,
            legalShortName = null,
            legalForm = BusinessPartnerVerboseValues.legalForm1.technicalKey,
            identifiers = listOf(identifier1, identifier2),
            states = listOf(leStatus1),
            classifications = listOf(classification1, classification2),
            confidenceCriteria = BusinessPartnerVerboseValues.legalEntity1.legalEntity.confidenceCriteria
        ),
        legalAddress = logisticAddress1,
        index = BusinessPartnerVerboseValues.legalEntityUpsertMultipleIdentifier.index
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

    val legalEntityUpdateMultipleIdentifier = LegalEntityPartnerUpdateRequest(
        bpnl = BusinessPartnerVerboseValues.legalEntityUpsertMultipleIdentifier.legalEntity.bpnl,
        legalEntity = legalEntityCreateMultipleIdentifier.legalEntity,
        legalAddress = legalEntityCreateMultipleIdentifier.legalAddress,
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