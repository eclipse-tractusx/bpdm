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
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BusinessPartnerV7Dto as BusinessPartnerV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.LegalEntityV7Dto as LegalEntityV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskClientStateV7Dto as TaskClientStateDtoV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.AlternativeAddressV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.BpnReferenceV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.BpnReferenceTypeV6
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.BusinessPartnerV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.BusinessStateV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.ConfidenceCriteriaV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.GeoCoordinateV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.IdentifierV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.LegalEntityV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.NamePartTypeV6
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.NamePartV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.PhysicalAddressV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.PostalAddressV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.ResultStateV6
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.SiteV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.StepStateV6
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.StreetV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskClientStateV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskErrorV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskProcessingStateV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.UncategorizedPropertiesV6Dto

/**
 * Translates the V7 golden record task client state into the reduced V6 shape, dropping the V7-only fields
 * V6 has no room for (`additionalSites`, ultimate owner, script variants, golden record relations).
 */
@Component
class GoldenRecordTaskCreateOutboundMapperV6 {

    fun toClientState(clientState: TaskClientStateDtoV7): TaskClientStateV6Dto =
        with(clientState) {
            TaskClientStateV6Dto(
                taskId = taskId,
                recordId = recordId,
                businessPartnerResult = toBusinessPartnerV6(businessPartnerResult),
                processingState = toProcessingStateV6(processingState)
            )
        }

    private fun toBusinessPartnerV6(businessPartner: BusinessPartnerV7): BusinessPartnerV6Dto =
        with(businessPartner) {
            BusinessPartnerV6Dto(
                nameParts = nameParts.map {
                    NamePartV6Dto(
                        name = it.name,
                        type = when (it.type) {
                            org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.NamePartTypeV7.LegalName -> NamePartTypeV6.LegalName
                            org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.NamePartTypeV7.ShortName -> NamePartTypeV6.ShortName
                            org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.NamePartTypeV7.LegalForm -> NamePartTypeV6.LegalForm
                            org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.NamePartTypeV7.SiteName -> NamePartTypeV6.SiteName
                            org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.NamePartTypeV7.AddressName -> NamePartTypeV6.AddressName
                        }
                    )
                },
                owningCompany = owningCompany,
                uncategorized = UncategorizedPropertiesV6Dto(
                    nameParts = uncategorized.nameParts,
                    identifiers = uncategorized.identifiers.map(::toIdentifierV6),
                    states = uncategorized.states.map(::toBusinessStateV6),
                    address = uncategorized.address?.postalProperties?.let(::toPostalAddressV6)
                ),
                legalEntity = toLegalEntityV6(legalEntity),
                site = site?.let {
                    SiteV6Dto(
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

    private fun toLegalEntityV6(legalEntity: LegalEntityV7): LegalEntityV6Dto =
        with(legalEntity) {
            LegalEntityV6Dto(
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

    private fun toProcessingStateV6(processingState: org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskProcessingStateV7Dto) =
        TaskProcessingStateV6Dto(
            resultState = when (processingState.resultState) {
                org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.ResultStateV7.Pending -> ResultStateV6.Pending
                org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.ResultStateV7.Success -> ResultStateV6.Success
                org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.ResultStateV7.Error -> ResultStateV6.Error
            },
            step = processingState.step.toV6(),
            stepState = when (processingState.stepState) {
                org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.StepStateV7.Queued -> StepStateV6.Queued
                org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.StepStateV7.Reserved -> StepStateV6.Reserved
                org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.StepStateV7.Success -> StepStateV6.Success
                org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.StepStateV7.Error -> StepStateV6.Error
            },
            errors = processingState.errors.map {
                TaskErrorV6Dto(
                    type = it.type.toV6(),
                    description = it.description
                )
            },
            createdAt = processingState.createdAt,
            modifiedAt = processingState.modifiedAt,
            timeout = processingState.timeout
        )

    private fun toIdentifierV6(identifier: org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.IdentifierV7Dto) =
        IdentifierV6Dto(identifier.value, identifier.type, identifier.issuingBody)

    private fun toBusinessStateV6(state: org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BusinessStateV7Dto) =
        BusinessStateV6Dto(state.validFrom, state.validTo, state.type)

    private fun toConfidenceCriteriaV6(confidenceCriteria: org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.ConfidenceCriteriaV7Dto) =
        ConfidenceCriteriaV6Dto(
            confidenceCriteria.sharedByOwner,
            confidenceCriteria.checkedByExternalDataSource,
            confidenceCriteria.numberOfSharingMembers,
            confidenceCriteria.lastConfidenceCheckAt,
            confidenceCriteria.nextConfidenceCheckAt,
            confidenceCriteria.confidenceLevel
        )

    private fun toBpnReferenceV6(bpnReference: org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BpnReferenceV7Dto) =
        BpnReferenceV6Dto(
            bpnReference.referenceValue,
            bpnReference.desiredBpn,
            bpnReference.referenceType?.let {
                when (it) {
                    org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BpnReferenceTypeV7.Bpn -> BpnReferenceTypeV6.Bpn
                    org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BpnReferenceTypeV7.BpnRequestIdentifier -> BpnReferenceTypeV6.BpnRequestIdentifier
                }
            }
        )

    private fun toPostalAddressV6(postalAddress: org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PostalAddressV7Dto) =
        PostalAddressV6Dto(
            bpnReference = toBpnReferenceV6(postalAddress.bpnReference),
            addressName = postalAddress.addressName,
            identifiers = postalAddress.identifiers.map(::toIdentifierV6),
            states = postalAddress.states.map(::toBusinessStateV6),
            confidenceCriteria = toConfidenceCriteriaV6(postalAddress.confidenceCriteria),
            physicalAddress = PhysicalAddressV6Dto(
                geographicCoordinates = GeoCoordinateV6Dto(
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
                street = StreetV6Dto(
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
                AlternativeAddressV6Dto(
                    geographicCoordinates = GeoCoordinateV6Dto(
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
