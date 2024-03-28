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

package org.eclipse.tractusx.bpdm.cleaning.service


import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.orchestrator.api.model.*
import java.time.LocalDateTime


val dummyConfidenceCriteria = ConfidenceCriteriaDto(
    sharedByOwner = false,
    numberOfSharingMembers = 1,
    checkedByExternalDataSource = false,
    lastConfidenceCheckAt = LocalDateTime.now(),
    nextConfidenceCheckAt = LocalDateTime.now().plusDays(5),
    confidenceLevel = 0
)

fun BusinessPartnerGenericDto.toLegalEntityDto(bpnReferenceDto: BpnReferenceDto, legalAddress: LogisticAddressDto): LegalEntityDto {
    return LegalEntityDto(
        bpnLReference = bpnReferenceDto,
        hasChanged = address.addressType in setOf(AddressType.LegalAddress, AddressType.LegalAndSiteMainAddress),
        legalName = nameParts.joinToString(" "),
        legalShortName = legalEntity.shortName,
        identifiers = identifiers.mapNotNull { it.toLegalEntityIdentifierDto() },
        legalForm = legalEntity.legalForm,
        states = states.mapNotNull { it.toLegalEntityState() }.plus(legalEntity.states.mapNotNull { it.toLegalEntityState() }),
        legalAddress = legalAddress,
        isCatenaXMemberData = ownerBpnL != null,
        confidenceCriteria = dummyConfidenceCriteria
    )
}

fun BusinessPartnerClassificationDto.toLegalEntityClassificationDto(): LegalEntityClassificationDto {

    return LegalEntityClassificationDto(code = code, type = type, value = value)
}

fun BusinessPartnerIdentifierDto.toLegalEntityIdentifierDto(): LegalEntityIdentifierDto? {

    return value?.let { value ->
        type?.let { type ->
            LegalEntityIdentifierDto(value = value, type = type, issuingBody = issuingBody)
        }
    }

}

fun BusinessPartnerStateDto.toLegalEntityState(): LegalEntityStateDto? {

    return type?.let { LegalEntityStateDto(validFrom, validTo, it) }
}

fun BusinessPartnerStateDto.toSiteState(): SiteStateDto? {

    return type?.let { SiteStateDto(validFrom, validTo, it) }
}

fun BusinessPartnerStateDto.toAddressState(): AddressStateDto? {

    return type?.let { AddressStateDto(validFrom, validTo, it) }
}

fun BusinessPartnerGenericDto.toLogisticAddressDto(bpnReferenceDto: BpnReferenceDto):
        LogisticAddressDto {

    return LogisticAddressDto(
        bpnAReference = bpnReferenceDto,
        hasChanged = address.addressType == AddressType.AdditionalAddress,
        name = address.name,
        states = address.states.mapNotNull { it.toAddressState() },
        identifiers = emptyList(),
        physicalPostalAddress = address.physicalPostalAddress,
        alternativePostalAddress = address.alternativePostalAddress,
        confidenceCriteria = dummyConfidenceCriteria
    )
}

fun BusinessPartnerGenericDto.toSiteDto(bpnReferenceDto: BpnReferenceDto, siteAddressReference: LogisticAddressDto): SiteDto {

    return SiteDto(
        bpnSReference = bpnReferenceDto,
        hasChanged = address.addressType in setOf(AddressType.SiteMainAddress, AddressType.LegalAndSiteMainAddress),
        name = site.name,
        states = site.states.mapNotNull { it.toSiteState() },
        mainAddress = siteAddressReference,
        confidenceCriteria = dummyConfidenceCriteria
    )
}





