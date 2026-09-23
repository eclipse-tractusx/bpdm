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

package org.eclipse.tractusx.bpdm.orchestrator.mapper.v6

import org.eclipse.tractusx.bpdm.orchestrator.mapper.v6.TaskV6Mapper.toV6
import org.springframework.stereotype.Component
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartner as BusinessPartnerV7
import org.eclipse.tractusx.orchestrator.api.model.LegalEntity as LegalEntityV7
import org.eclipse.tractusx.orchestrator.api.model.TaskClientStateDto as TaskClientStateDtoV7
import org.eclipse.tractusx.orchestrator.api.v6.model.AlternativeAddressV6
import org.eclipse.tractusx.orchestrator.api.v6.model.BpnReferenceV6
import org.eclipse.tractusx.orchestrator.api.v6.model.BpnReferenceTypeV6
import org.eclipse.tractusx.orchestrator.api.v6.model.BusinessPartnerV6
import org.eclipse.tractusx.orchestrator.api.v6.model.BusinessStateV6
import org.eclipse.tractusx.orchestrator.api.v6.model.ConfidenceCriteriaV6
import org.eclipse.tractusx.orchestrator.api.v6.model.GeoCoordinateV6
import org.eclipse.tractusx.orchestrator.api.v6.model.IdentifierV6
import org.eclipse.tractusx.orchestrator.api.v6.model.LegalEntityV6
import org.eclipse.tractusx.orchestrator.api.v6.model.NamePartTypeV6
import org.eclipse.tractusx.orchestrator.api.v6.model.NamePartV6
import org.eclipse.tractusx.orchestrator.api.v6.model.PhysicalAddressV6
import org.eclipse.tractusx.orchestrator.api.v6.model.PostalAddressV6
import org.eclipse.tractusx.orchestrator.api.v6.model.ResultStateV6
import org.eclipse.tractusx.orchestrator.api.v6.model.SiteV6
import org.eclipse.tractusx.orchestrator.api.v6.model.StepStateV6
import org.eclipse.tractusx.orchestrator.api.v6.model.StreetV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskClientStateDtoV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskErrorDtoV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskErrorTypeV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskProcessingStateDtoV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskStepV6
import org.eclipse.tractusx.orchestrator.api.v6.model.UncategorizedPropertiesV6

/**
 * Translates the V7 golden record task client state into the reduced V6 shape, dropping the V7-only fields
 * V6 has no room for (`additionalSites`, ultimate owner, script variants, golden record relations).
 */
@Component
class GoldenRecordTaskCreateOutboundMapperV6 {

    fun toClientState(clientState: TaskClientStateDtoV7): TaskClientStateDtoV6 =
        with(clientState) {
            TaskClientStateDtoV6(
                taskId = taskId,
                recordId = recordId,
                businessPartnerResult = toBusinessPartnerV6(businessPartnerResult),
                processingState = toProcessingStateV6(processingState)
            )
        }

    private fun toBusinessPartnerV6(businessPartner: BusinessPartnerV7): BusinessPartnerV6 =
        with(businessPartner) {
            BusinessPartnerV6(
                nameParts = nameParts.map {
                    NamePartV6(
                        name = it.name,
                        type = when (it.type) {
                            org.eclipse.tractusx.orchestrator.api.model.NamePartType.LegalName -> NamePartTypeV6.LegalName
                            org.eclipse.tractusx.orchestrator.api.model.NamePartType.ShortName -> NamePartTypeV6.ShortName
                            org.eclipse.tractusx.orchestrator.api.model.NamePartType.LegalForm -> NamePartTypeV6.LegalForm
                            org.eclipse.tractusx.orchestrator.api.model.NamePartType.SiteName -> NamePartTypeV6.SiteName
                            org.eclipse.tractusx.orchestrator.api.model.NamePartType.AddressName -> NamePartTypeV6.AddressName
                        }
                    )
                },
                owningCompany = owningCompany,
                uncategorized = UncategorizedPropertiesV6(
                    nameParts = uncategorized.nameParts,
                    identifiers = uncategorized.identifiers.map(::toIdentifierV6),
                    states = uncategorized.states.map(::toBusinessStateV6),
                    address = uncategorized.address?.postalProperties?.let(::toPostalAddressV6)
                ),
                legalEntity = toLegalEntityV6(legalEntity),
                site = site?.let {
                    SiteV6(
                        bpnReference = toBpnReferenceV6(it.bpnReference),
                        siteName = it.siteName,
                        states = it.states.map(::toBusinessStateV6),
                        confidenceCriteria = toConfidenceCriteriaV6(it.confidenceCriteria),
                        hasChanged = it.hasChanged,
                        siteMainAddress = it.siteMainAddress?.let(::toPostalAddressV6)
                    )
                },
                additionalAddress = additionalAddress?.toPostalAddress()?.let(::toPostalAddressV6)
            )
        }

    private fun toLegalEntityV6(legalEntity: LegalEntityV7): LegalEntityV6 =
        with(legalEntity) {
            LegalEntityV6(
                bpnReference = toBpnReferenceV6(bpnReference),
                legalName = legalName,
                legalShortName = legalShortName,
                legalForm = legalForm,
                identifiers = identifiers.map(::toIdentifierV6),
                states = states.map(::toBusinessStateV6),
                confidenceCriteria = toConfidenceCriteriaV6(confidenceCriteria),
                isCatenaXMemberData = isParticipantData,
                hasChanged = hasChanged,
                legalAddress = toPostalAddressV6(legalAddress)
            )
        }

    private fun toProcessingStateV6(processingState: org.eclipse.tractusx.orchestrator.api.model.TaskProcessingStateDto) =
        TaskProcessingStateDtoV6(
            resultState = when (processingState.resultState) {
                org.eclipse.tractusx.orchestrator.api.model.ResultState.Pending -> ResultStateV6.Pending
                org.eclipse.tractusx.orchestrator.api.model.ResultState.Success -> ResultStateV6.Success
                org.eclipse.tractusx.orchestrator.api.model.ResultState.Error -> ResultStateV6.Error
            },
            step = processingState.step.toV6(),
            stepState = when (processingState.stepState) {
                org.eclipse.tractusx.orchestrator.api.model.StepState.Queued -> StepStateV6.Queued
                org.eclipse.tractusx.orchestrator.api.model.StepState.Reserved -> StepStateV6.Reserved
                org.eclipse.tractusx.orchestrator.api.model.StepState.Success -> StepStateV6.Success
                org.eclipse.tractusx.orchestrator.api.model.StepState.Error -> StepStateV6.Error
            },
            errors = processingState.errors.map {
                TaskErrorDtoV6(
                    type = it.type.toV6(),
                    description = it.description
                )
            },
            createdAt = processingState.createdAt,
            modifiedAt = processingState.modifiedAt,
            timeout = processingState.timeout
        )

    private fun toIdentifierV6(identifier: org.eclipse.tractusx.orchestrator.api.model.Identifier) =
        IdentifierV6(identifier.value, identifier.type, identifier.issuingBody)

    private fun toBusinessStateV6(state: org.eclipse.tractusx.orchestrator.api.model.BusinessState) =
        BusinessStateV6(state.validFrom, state.validTo, state.type)

    private fun toConfidenceCriteriaV6(confidenceCriteria: org.eclipse.tractusx.orchestrator.api.model.ConfidenceCriteria) =
        ConfidenceCriteriaV6(
            confidenceCriteria.sharedByOwner,
            confidenceCriteria.checkedByExternalDataSource,
            confidenceCriteria.numberOfSharingMembers,
            confidenceCriteria.lastConfidenceCheckAt,
            confidenceCriteria.nextConfidenceCheckAt,
            confidenceCriteria.confidenceLevel
        )

    private fun toBpnReferenceV6(bpnReference: org.eclipse.tractusx.orchestrator.api.model.BpnReference) =
        BpnReferenceV6(
            bpnReference.referenceValue,
            bpnReference.desiredBpn,
            bpnReference.referenceType?.let {
                when (it) {
                    org.eclipse.tractusx.orchestrator.api.model.BpnReferenceType.Bpn -> BpnReferenceTypeV6.Bpn
                    org.eclipse.tractusx.orchestrator.api.model.BpnReferenceType.BpnRequestIdentifier -> BpnReferenceTypeV6.BpnRequestIdentifier
                }
            }
        )

    private fun toPostalAddressV6(postalAddress: org.eclipse.tractusx.orchestrator.api.model.PostalAddress) =
        PostalAddressV6(
            bpnReference = toBpnReferenceV6(postalAddress.bpnReference),
            addressName = postalAddress.addressName,
            identifiers = postalAddress.identifiers.map(::toIdentifierV6),
            states = postalAddress.states.map(::toBusinessStateV6),
            confidenceCriteria = toConfidenceCriteriaV6(postalAddress.confidenceCriteria),
            physicalAddress = PhysicalAddressV6(
                geographicCoordinates = GeoCoordinateV6(
                    postalAddress.physicalAddress.geographicCoordinates.longitude,
                    postalAddress.physicalAddress.geographicCoordinates.latitude,
                    postalAddress.physicalAddress.geographicCoordinates.altitude
                ),
                country = postalAddress.physicalAddress.country,
                administrativeAreaLevel1 = postalAddress.physicalAddress.administrativeAreaLevel1,
                administrativeAreaLevel2 = postalAddress.physicalAddress.administrativeAreaLevel2,
                administrativeAreaLevel3 = postalAddress.physicalAddress.administrativeAreaLevel3,
                postalCode = postalAddress.physicalAddress.postalCode,
                city = postalAddress.physicalAddress.city,
                district = postalAddress.physicalAddress.district,
                street = StreetV6(
                    postalAddress.physicalAddress.street.name,
                    postalAddress.physicalAddress.street.houseNumber,
                    postalAddress.physicalAddress.street.houseNumberSupplement,
                    postalAddress.physicalAddress.street.milestone,
                    postalAddress.physicalAddress.street.direction,
                    postalAddress.physicalAddress.street.namePrefix,
                    postalAddress.physicalAddress.street.additionalNamePrefix,
                    postalAddress.physicalAddress.street.nameSuffix,
                    postalAddress.physicalAddress.street.additionalNameSuffix
                ),
                companyPostalCode = postalAddress.physicalAddress.companyPostalCode,
                industrialZone = postalAddress.physicalAddress.industrialZone,
                building = postalAddress.physicalAddress.building,
                floor = postalAddress.physicalAddress.floor,
                door = postalAddress.physicalAddress.door,
                taxJurisdictionCode = postalAddress.physicalAddress.taxJurisdictionCode
            ),
            alternativeAddress = postalAddress.alternativeAddress?.let {
                AlternativeAddressV6(
                    geographicCoordinates = GeoCoordinateV6(
                        it.geographicCoordinates.longitude,
                        it.geographicCoordinates.latitude,
                        it.geographicCoordinates.altitude
                    ),
                    country = it.country,
                    administrativeAreaLevel1 = it.administrativeAreaLevel1,
                    postalCode = it.postalCode,
                    city = it.city,
                    deliveryServiceType = it.deliveryServiceType,
                    deliveryServiceQualifier = it.deliveryServiceQualifier,
                    deliveryServiceNumber = it.deliveryServiceNumber
                )
            },
            hasChanged = postalAddress.hasChanged
        )
}
