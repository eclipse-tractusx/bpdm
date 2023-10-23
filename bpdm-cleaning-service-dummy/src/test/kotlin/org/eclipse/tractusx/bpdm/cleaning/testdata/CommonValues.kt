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

package org.eclipse.tractusx.bpdm.cleaning.testdata

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.cleaning.service.toBusinessPartnerClassificationDto
import org.eclipse.tractusx.bpdm.cleaning.service.toLegalEntityIdentifierDto
import org.eclipse.tractusx.bpdm.cleaning.service.toLegalEntityState
import org.eclipse.tractusx.bpdm.cleaning.service.toSiteState
import org.eclipse.tractusx.bpdm.common.dto.*
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.ClassificationType
import org.eclipse.tractusx.orchestrator.api.model.*
import org.eclipse.tractusx.orchestrator.api.model.LegalEntityDto
import org.eclipse.tractusx.orchestrator.api.model.LogisticAddressDto
import org.eclipse.tractusx.orchestrator.api.model.PhysicalPostalAddressDto
import org.eclipse.tractusx.orchestrator.api.model.SiteDto
import org.eclipse.tractusx.orchestrator.api.model.StreetDto
import java.time.LocalDateTime

/**
 * Contains simple test values used to create more complex test values such as DTOs
 */
object CommonValues {

    val fixedTaskId = "taskid-123123"

    val nameParts = listOf("Part1", "Part2")
    const val shortName = "ShortName"
    val identifiers = listOf(
        BusinessPartnerIdentifierDto(
            type = "Type1",
            value = "Value1",
            issuingBody = "IssuingBody1"
        )
    )
    const val legalForm = "LegalForm"
    val states = listOf(
        BusinessPartnerStateDto(
            validFrom = LocalDateTime.now(),
            validTo = LocalDateTime.now().plusDays(10),
            type = BusinessStateType.ACTIVE,
            description = "ActiveState"
        )
    )
    val classifications = listOf(
        ClassificationDto(
            type = ClassificationType.NACE,
            code = "Code1",
            value = "Value1"
        )
    )
    val roles = listOf(BusinessPartnerRole.SUPPLIER, BusinessPartnerRole.CUSTOMER)
    val physicalPostalAddress = PhysicalPostalAddressDto(
        geographicCoordinates = GeoCoordinateDto(longitude = 12.34f, latitude = 56.78f),
        country = CountryCode.PT,
        administrativeAreaLevel1 = "AdminArea1",
        administrativeAreaLevel2 = "AdminArea2",
        administrativeAreaLevel3 = "AdminArea3",
        postalCode = "PostalCode",
        city = "City",
        district = "District",
        street = StreetDto("StreetName"),
        companyPostalCode = "CompanyPostalCode",
        industrialZone = "IndustrialZone",
        building = "Building",
        floor = "Floor",
        door = "Door"
    )

    val postalAddressForLegalAndSite = PostalAddressDto(
        addressType = AddressType.LegalAndSiteMainAddress,
        physicalPostalAddress = physicalPostalAddress
    )
    val postalAddressForLegal = PostalAddressDto(
        addressType = AddressType.LegalAddress,
        physicalPostalAddress = physicalPostalAddress
    )
    val postalAddressForSite = PostalAddressDto(
        addressType = AddressType.SiteMainAddress,
        physicalPostalAddress = physicalPostalAddress
    )
    val postalAddressForAdditional = PostalAddressDto(
        addressType = AddressType.AdditionalAddress,
        physicalPostalAddress = physicalPostalAddress
    )

    val businessPartnerWithEmptyBpns = BusinessPartnerGenericDto(
        nameParts = nameParts,
        shortName = shortName,
        identifiers = identifiers,
        legalForm = legalForm,
        states = states,
        classifications = classifications,
        roles = roles,
        ownerBpnL = "ownerBpnL2"
    )


    val businessPartnerWithBpnA = businessPartnerWithEmptyBpns.copy(
        postalAddress = postalAddressForAdditional,
        bpnA = "FixedBPNA"
    )


    val businessPartnerWithBpnLAndBpnAAndLegalAddressType = businessPartnerWithEmptyBpns.copy(
        postalAddress = postalAddressForLegal,
        bpnA = "FixedBPNA",
        bpnL = "FixedBPNL"
    )

    val businessPartnerWithEmptyBpnLAndAdditionalAddressType = businessPartnerWithEmptyBpns.copy(
        postalAddress = postalAddressForAdditional,
    )

    val businessPartnerWithBpnSAndBpnAAndLegalAndSiteMainAddressType = businessPartnerWithEmptyBpns.copy(
        postalAddress = postalAddressForLegalAndSite,
        bpnA = "FixedBPNA",
        bpnS = "FixedBPNS"
    )

    val businessPartnerWithEmptyBpnAndSiteMainAddressType = businessPartnerWithEmptyBpns.copy(
        postalAddress = postalAddressForSite
    )

    val expectedLegalEntityDto = LegalEntityDto(
        hasChanged = true,
        legalName = nameParts.joinToString(" "),
        legalShortName = shortName,
        identifiers = identifiers.mapNotNull { it.toLegalEntityIdentifierDto() },
        legalForm = legalForm,
        states = states.mapNotNull { it.toLegalEntityState() },
        classifications = classifications.map { it.toBusinessPartnerClassificationDto() }
    )

    val expectedSiteDto = SiteDto(
        hasChanged = true,
        name = nameParts.joinToString(" "),
        states = states.mapNotNull { it.toSiteState() },
    )

    val expectedLogisticAddressDto = LogisticAddressDto(

        hasChanged = true,
        name = nameParts.joinToString(" "),
        states = emptyList(),
        identifiers = emptyList(),
        physicalPostalAddress = physicalPostalAddress
    )


}