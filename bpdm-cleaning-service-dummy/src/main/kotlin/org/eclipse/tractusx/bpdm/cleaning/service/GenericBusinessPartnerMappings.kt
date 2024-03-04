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


val dummyConfidenceCriteria = ConfidenceCriteria(
    sharedByOwner = false,
    numberOfBusinessPartners = 1,
    checkedByExternalDataSource = false,
    lastConfidenceCheckAt = LocalDateTime.now(),
    nextConfidenceCheckAt = LocalDateTime.now().plusDays(5),
    confidenceLevel = 0
)

fun BusinessPartnerGeneric.toLegalEntityDto(bpnReferenceDto: BpnReference, legalAddress: LogisticAddress): LegalEntity {
    return LegalEntity(
        bpnLReference = bpnReferenceDto,
        hasChanged = address.addressType in setOf(AddressType.LegalAddress, AddressType.LegalAndSiteMainAddress),
        legalName = nameParts.joinToString(" "),
        legalShortName = legalEntity.shortName,
        identifiers = identifiers.mapNotNull { it.toLegalEntityIdentifierDto() },
        legalForm = legalEntity.legalForm,
        states = states.mapNotNull { it.toLegalEntityState() },
        classifications = legalEntity.classifications.map { it.toLegalEntityClassificationDto() },
        legalAddress = legalAddress,
        confidenceCriteria = dummyConfidenceCriteria
    )
}

fun BusinessPartnerClassification.toLegalEntityClassificationDto(): LegalEntityClassification {

    return LegalEntityClassification(code = code, type = type, value = value)
}

fun BusinessPartnerIdentifier.toLegalEntityIdentifierDto(): LegalEntityIdentifier? {

    return value?.let { value ->
        type?.let { type ->
            LegalEntityIdentifier(value = value, type = type, issuingBody = issuingBody)
        }
    }

}

fun BusinessPartnerState.toLegalEntityState(): LegalEntityState? {

    return type?.let { LegalEntityState(validFrom, validTo, it) }
}

fun BusinessPartnerState.toSiteState(): SiteState? {

    return type?.let { SiteState(validFrom, validTo, it) }
}

fun BusinessPartnerGeneric.toLogisticAddressDto(bpnReferenceDto: BpnReference):
        LogisticAddress {

    return LogisticAddress(
        bpnAReference = bpnReferenceDto,
        hasChanged = address.addressType == AddressType.AdditionalAddress,
        name = address.name,
        states = emptyList(),
        identifiers = emptyList(),
        physicalPostalAddress = address.physicalPostalAddress,
        alternativePostalAddress = address.alternativePostalAddress,
        confidenceCriteria = dummyConfidenceCriteria
    )
}

fun BusinessPartnerGeneric.toSiteDto(bpnReferenceDto: BpnReference, siteAddressReference: LogisticAddress): Site {

    return Site(
        bpnSReference = bpnReferenceDto,
        hasChanged = address.addressType in setOf(AddressType.SiteMainAddress, AddressType.LegalAndSiteMainAddress),
        name = site.name,
        states = states.mapNotNull { it.toSiteState() },
        mainAddress = siteAddressReference,
        confidenceCriteria = dummyConfidenceCriteria
    )
}





