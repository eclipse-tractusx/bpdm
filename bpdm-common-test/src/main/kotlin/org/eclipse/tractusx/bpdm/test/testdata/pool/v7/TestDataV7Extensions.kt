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

package org.eclipse.tractusx.bpdm.test.testdata.pool.v7

import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.api.model.request.*
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressPartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressPartnerUpdateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityPartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto


fun LegalEntityPartnerCreateRequest.withLegalIdentifiers(identifiers: List<LegalEntityIdentifierDto>) =
    copy(legalEntity = legalEntity.withLegalIdentifiers(identifiers))

fun LegalEntityPartnerCreateRequest.withLegalIdentifiers(vararg identifiers: LegalEntityIdentifierDto) =
    withLegalIdentifiers(identifiers.toList())

fun LegalEntityDto.withLegalIdentifiers(identifiers: List<LegalEntityIdentifierDto>) =
    copy(header = header.copy(identifiers = identifiers))

fun LegalEntityDto.withLegalIdentifiers(vararg identifiers: LegalEntityIdentifierDto) =
    withLegalIdentifiers(identifiers.toList())

fun LegalEntityPartnerCreateRequest.withLegalAddressIdentifiers(identifiers: List<AddressIdentifierDto>) =
    copy(legalEntity = legalEntity.withLegalAddressIdentifiers(identifiers))

fun LegalEntityPartnerCreateRequest.withLegalAddressIdentifiers(vararg identifiers: AddressIdentifierDto) =
    withLegalAddressIdentifiers(identifiers.toList())

fun LegalEntityDto.withLegalAddressIdentifiers(identifiers: List<AddressIdentifierDto>) =
    copy(legalAddress = legalAddress.copy(identifiers = identifiers))

fun LegalEntityDto.withLegalAddressIdentifiers(vararg identifiers: AddressIdentifierDto) =
    withLegalAddressIdentifiers(identifiers.toList())

fun LegalEntityPartnerCreateRequest.withLegalForm(legalForm: String) =
    copy(legalEntity = legalEntity.withLegalForm(legalForm))

fun LegalEntityDto.withLegalForm(legalForm: String) =
    copy(header = header.copy(legalForm = legalForm))

fun LegalEntityPartnerCreateRequest.withPhysicalAdminArea(code: String?) =
    copy(legalEntity = legalEntity.withPhysicalAdminArea(code))

fun LegalEntityDto.withPhysicalAdminArea(code: String?) =
    copy(legalAddress = legalAddress.copy(physicalPostalAddress = legalAddress.physicalPostalAddress.copy(administrativeAreaLevel1 = code)))

fun LegalEntityPartnerCreateRequest.withAlternativeAdminArea(code: String?) =
    copy(legalEntity = legalEntity.withAlternativeAdminArea(code))

fun LegalEntityDto.withAlternativeAdminArea(code: String?) =
    copy(legalAddress = legalAddress.copy(alternativePostalAddress = legalAddress.alternativePostalAddress?.copy(administrativeAreaLevel1 = code)))

fun LegalEntityPartnerCreateRequest.withParticipantData(isParticipantData: Boolean) =
    copy(legalEntity = legalEntity.withParticipantData(isParticipantData))

fun LegalEntityPartnerUpdateRequest.withLegalIdentifiers(identifiers: List<LegalEntityIdentifierDto>) =
    copy(legalEntity = legalEntity.withLegalIdentifiers(identifiers))

fun LegalEntityPartnerUpdateRequest.withLegalIdentifiers(vararg identifiers: LegalEntityIdentifierDto) =
    withLegalIdentifiers(identifiers.toList())

fun LegalEntityPartnerUpdateRequest.withLegalAddressIdentifiers(identifiers: List<AddressIdentifierDto>) =
    copy(legalEntity = legalEntity.withLegalAddressIdentifiers(identifiers))

fun LegalEntityPartnerUpdateRequest.withLegalAddressIdentifiers(vararg identifiers: AddressIdentifierDto) =
    withLegalAddressIdentifiers(identifiers.toList())

fun LegalEntityPartnerUpdateRequest.withLegalForm(legalForm: String) =
    copy(legalEntity = legalEntity.withLegalForm(legalForm))

fun LegalEntityPartnerUpdateRequest.withPhysicalAdminArea(code: String?) =
    copy(legalEntity = legalEntity.withPhysicalAdminArea(code))

fun LegalEntityPartnerUpdateRequest.withAlternativeAdminArea(code: String?) =
    copy(legalEntity = legalEntity.withAlternativeAdminArea(code))

fun LegalEntityPartnerUpdateRequest.withParticipantData(isParticipantData: Boolean) =
    copy(legalEntity = legalEntity.withParticipantData(isParticipantData))

fun LegalEntityDto.withParticipantData(isParticipantData: Boolean) =
    copy(header = header.withParticipantData(isParticipantData), legalAddress = legalAddress.withSharedByOwner(isParticipantData))

fun LegalEntityHeaderDto.withParticipantData(isParticipantData: Boolean) =
    copy(isParticipantData = isParticipantData, confidenceCriteria = confidenceCriteria.copy(sharedByOwner = isParticipantData))

fun LogisticAddressDto.withSharedByOwner(isSharedByOwner: Boolean)  =
    copy(confidenceCriteria = confidenceCriteria.copy(sharedByOwner = isSharedByOwner))

fun LogisticAddressDto.withConfidence(confidence: GivenConfidence) =
    copy(confidenceCriteria = confidenceCriteria.withGivenConfidence(confidence))

fun AddressPartnerCreateRequest.withConfidence(givenConfidence: GivenConfidence) =
    copy(address = address.withConfidence(givenConfidence))

fun AddressPartnerUpdateRequest.withConfidence(givenConfidence: GivenConfidence) =
    copy(address = address.withConfidence(givenConfidence))

fun LegalEntityPartnerCreateVerboseDto.withConfidence(confidence: CalculatedConfidence) =
    copy(legalEntity = legalEntity.withConfidence(confidence))

fun LegalEntityWithLegalAddressVerboseDto.withConfidence(confidence: CalculatedConfidence) =
    copy(header = header.withConfidence(confidence), legalAddress = legalAddress.withConfidence(confidence))

fun LegalEntityHeaderVerboseDto.withConfidence(confidence: CalculatedConfidence) =
    copy(confidenceCriteria = confidenceCriteria.withCalculatedConfidence(confidence))

fun LogisticAddressInvariantVerboseDto.withConfidence(confidence: CalculatedConfidence) =
    copy(confidenceCriteria = confidenceCriteria.withCalculatedConfidence(confidence))

fun ConfidenceCriteriaDto.withCalculatedConfidence(confidence: CalculatedConfidence): ConfidenceCriteriaDto {
    return copy(
        numberOfSharingMembers = confidence.numberOfSharingMembers,
        confidenceLevel = confidence.confidenceLevel
    )
}

fun ConfidenceCriteriaDto.withGivenConfidence(confidence: GivenConfidence): ConfidenceCriteriaDto {
    return copy(
        sharedByOwner = confidence.sharedByOwner,
        checkedByExternalDataSource = confidence.checkedByExternalDataSource
    )
}

fun SitePartnerCreateRequest.withMainAddressIdentifiers(identifiers: List<AddressIdentifierDto>) =
    copy(site = site.copy(mainAddress = site.mainAddress.copy(identifiers = identifiers)))

fun SitePartnerCreateRequest.withMainAddressIdentifiers(vararg identifiers: AddressIdentifierDto) =
    withMainAddressIdentifiers(identifiers.toList())

fun SitePartnerCreateRequest.withPhysicalAdminArea(code: String?) =
    copy(site = site.copy(mainAddress = site.mainAddress.copy(physicalPostalAddress = site.mainAddress.physicalPostalAddress.copy(administrativeAreaLevel1 = code))))

fun SitePartnerCreateRequest.withAlternativeAdminArea(code: String?) =
    copy(site = site.copy(mainAddress = site.mainAddress.copy(alternativePostalAddress = site.mainAddress.alternativePostalAddress?.copy(administrativeAreaLevel1 = code))))

fun SitePartnerUpdateRequest.withMainAddressIdentifiers(identifiers: List<AddressIdentifierDto>) =
    copy(site = site.copy(mainAddress = site.mainAddress.copy(identifiers = identifiers)))

fun SitePartnerUpdateRequest.withMainAddressIdentifiers(vararg identifiers: AddressIdentifierDto) =
    withMainAddressIdentifiers(identifiers.toList())

fun SitePartnerUpdateRequest.withPhysicalAdminArea(code: String?) =
    copy(site = site.copy(mainAddress = site.mainAddress.copy(physicalPostalAddress = site.mainAddress.physicalPostalAddress.copy(administrativeAreaLevel1 = code))))

fun SitePartnerUpdateRequest.withAlternativeAdminArea(code: String?) =
    copy(site = site.copy(mainAddress = site.mainAddress.copy(alternativePostalAddress = site.mainAddress.alternativePostalAddress?.copy(administrativeAreaLevel1 = code))))

fun AddressPartnerCreateRequest.withIdentifiers(identifiers: List<AddressIdentifierDto>) =
    copy(address = address.copy(identifiers = identifiers))

fun AddressPartnerCreateRequest.withIdentifiers(vararg identifiers: AddressIdentifierDto) =
    withIdentifiers(identifiers.toList())

fun AddressPartnerCreateRequest.withPhysicalAdminArea(code: String?) =
    copy(address = address.copy(physicalPostalAddress = address.physicalPostalAddress.copy(administrativeAreaLevel1 = code)))

fun AddressPartnerCreateRequest.withAlternativeAdminArea(code: String?) =
    copy(address = address.copy(alternativePostalAddress = address.alternativePostalAddress?.copy(administrativeAreaLevel1 = code)))

fun AddressPartnerUpdateRequest.withIdentifiers(identifiers: List<AddressIdentifierDto>) =
    copy(address = address.copy(identifiers = identifiers))

fun AddressPartnerUpdateRequest.withIdentifiers(vararg identifiers: AddressIdentifierDto) =
    withIdentifiers(identifiers.toList())

fun AddressPartnerUpdateRequest.withPhysicalAdminArea(code: String?) =
    copy(address = address.copy(physicalPostalAddress = address.physicalPostalAddress.copy(administrativeAreaLevel1 = code)))

fun AddressPartnerUpdateRequest.withAlternativeAdminArea(code: String?) =
    copy(address = address.copy(alternativePostalAddress = address.alternativePostalAddress?.copy(administrativeAreaLevel1 = code)))

fun AddressPartnerCreateVerboseDto.withConfidence(confidence: CalculatedConfidence) =
    copy(address = address.withConfidence(confidence))

fun AddressPartnerUpdateVerboseDto.withConfidence(confidence: CalculatedConfidence) =
    copy(address = address.withConfidence(confidence))

