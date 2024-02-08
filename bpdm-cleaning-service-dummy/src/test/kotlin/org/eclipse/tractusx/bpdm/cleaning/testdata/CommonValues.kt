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

package org.eclipse.tractusx.bpdm.cleaning.testdata

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.cleaning.service.*
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.ClassificationType
import org.eclipse.tractusx.orchestrator.api.model.*
import java.time.LocalDateTime

/**
 * Contains simple test values used to create more complex test values such as DTOs
 */
object CommonValues {

    const val fixedTaskId = "taskid-123123"

    private val nameParts = listOf("Part1", "Part2")
    private val siteName = "Site Name"
    private val addressName = "Address Name"
    private const val shortName = "ShortName"
    private val identifiers = listOf(
        BusinessPartnerIdentifierDto(
            type = "Type1",
            value = "Value1",
            issuingBody = "IssuingBody1"
        )
    )
    private const val legalForm = "LegalForm"
    private val states = listOf(
        BusinessPartnerStateDto(
            validFrom = LocalDateTime.now(),
            validTo = LocalDateTime.now().plusDays(10),
            type = BusinessStateType.ACTIVE
        )
    )
    private val classifications = listOf(
        BusinessPartnerClassificationDto(
            type = ClassificationType.NACE,
            code = "Code1",
            value = "Value1"
        )
    )
    private val roles = listOf(BusinessPartnerRole.SUPPLIER, BusinessPartnerRole.CUSTOMER)
    private val physicalPostalAddress = PhysicalPostalAddressDto(
        geographicCoordinates = GeoCoordinateDto(longitude = 12.34f, latitude = 56.78f),
        country = CountryCode.PT,
        administrativeAreaLevel1 = "AdminArea1",
        administrativeAreaLevel2 = "AdminArea2",
        administrativeAreaLevel3 = "AdminArea3",
        postalCode = "PostalCode",
        city = "City",
        district = "District",
        street = StreetDto("StreetName", houseNumberSupplement = "House Number Supplement"),
        companyPostalCode = "CompanyPostalCode",
        industrialZone = "IndustrialZone",
        building = "Building",
        floor = "Floor",
        door = "Door"
    )

    private val postalAddressForLegalAndSite = PostalAddressDto(
        addressType = AddressType.LegalAndSiteMainAddress,
        physicalPostalAddress = physicalPostalAddress
    )
    private val postalAddressForLegal = PostalAddressDto(
        addressType = AddressType.LegalAddress,
        physicalPostalAddress = physicalPostalAddress
    )
    private val postalAddressForSite = PostalAddressDto(
        addressType = AddressType.SiteMainAddress,
        physicalPostalAddress = physicalPostalAddress
    )
    private val postalAddressForAdditional = PostalAddressDto(
        addressType = AddressType.AdditionalAddress,
        physicalPostalAddress = physicalPostalAddress
    )

    private val businessPartnerWithEmptyBpns = BusinessPartnerGenericDto(
        nameParts = nameParts,
        identifiers = identifiers,
        states = states,
        roles = roles,
        ownerBpnL = "ownerBpnL2",
        legalEntity = LegalEntityRepresentation(
            shortName = shortName,
            legalForm = legalForm,
            classifications = classifications,
            confidenceCriteria = dummyConfidenceCriteria
        ),
        site = SiteRepresentation(name = siteName),
        address = AddressRepresentation(name = addressName)
    )


    val businessPartnerWithBpnA = with(businessPartnerWithEmptyBpns) {
        copy(
            address = address.copy(
                addressBpn = "FixedBPNA",
                addressType = postalAddressForAdditional.addressType,
                physicalPostalAddress = postalAddressForAdditional.physicalPostalAddress,
                alternativePostalAddress = postalAddressForAdditional.alternativePostalAddress
            )
        )
    }


    val businessPartnerWithBpnLAndBpnAAndLegalAddressType = with(businessPartnerWithEmptyBpns) {
        copy(
            address = address.copy(
                addressBpn = "FixedBPNA",
                addressType = postalAddressForLegal.addressType,
                physicalPostalAddress = postalAddressForLegal.physicalPostalAddress,
                alternativePostalAddress = postalAddressForLegal.alternativePostalAddress
            ),
            legalEntity = legalEntity.copy(
                legalEntityBpn = "FixedBPNL"
            )
        )
    }

    val businessPartnerWithEmptyBpnLAndAdditionalAddressType = with(businessPartnerWithEmptyBpns) {
        copy(
            address = address.copy(
                addressType = postalAddressForAdditional.addressType,
                physicalPostalAddress = postalAddressForAdditional.physicalPostalAddress,
                alternativePostalAddress = postalAddressForAdditional.alternativePostalAddress
            )
        )
    }

    val businessPartnerWithBpnSAndBpnAAndLegalAndSiteMainAddressType = with(businessPartnerWithEmptyBpns) {
        copy(
            address = address.copy(
                addressBpn = "FixedBPNA",
                addressType = postalAddressForLegalAndSite.addressType,
                physicalPostalAddress = postalAddressForLegalAndSite.physicalPostalAddress,
                alternativePostalAddress = postalAddressForLegalAndSite.alternativePostalAddress
            ),
            site = site.copy(
                siteBpn = "FixedBPNS"
            )
        )
    }

    val businessPartnerWithEmptyBpnAndSiteMainAddressType = with(businessPartnerWithEmptyBpns) {
        copy(
            address = address.copy(
                addressType = postalAddressForSite.addressType,
                physicalPostalAddress = postalAddressForSite.physicalPostalAddress,
                alternativePostalAddress = postalAddressForSite.alternativePostalAddress
            )
        )
    }

    val expectedLegalEntityDto = LegalEntityDto(
        hasChanged = true,
        legalName = nameParts.joinToString(" "),
        legalShortName = shortName,
        identifiers = identifiers.mapNotNull { it.toLegalEntityIdentifierDto() },
        legalForm = legalForm,
        states = states.mapNotNull { it.toLegalEntityState() },
        classifications = classifications.map { it.toLegalEntityClassificationDto() },
        confidenceCriteria = dummyConfidenceCriteria
    )

    val expectedSiteDto = SiteDto(
        hasChanged = true,
        name = siteName,
        states = states.mapNotNull { it.toSiteState() },
        confidenceCriteria = dummyConfidenceCriteria
    )

    val expectedLogisticAddressDto = LogisticAddressDto(

        hasChanged = true,
        name = addressName,
        states = emptyList(),
        identifiers = emptyList(),
        physicalPostalAddress = physicalPostalAddress,
        confidenceCriteria = dummyConfidenceCriteria
    )


}