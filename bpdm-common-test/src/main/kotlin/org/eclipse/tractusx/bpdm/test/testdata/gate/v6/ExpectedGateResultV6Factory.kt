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

package org.eclipse.tractusx.bpdm.test.testdata.gate.v6

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.gate.api.v6.model.*
import org.eclipse.tractusx.bpdm.gate.api.v6.model.AlternativePostalAddressV6Dto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.ConfidenceCriteriaV6Dto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.PhysicalPostalAddressV6Dto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.StreetV6Dto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.request.BusinessPartnerInputV6Request
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.BusinessPartnerInputV6Dto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.request.RelationPostV6Request
import org.eclipse.tractusx.bpdm.gate.api.v6.model.request.RelationPutV6Entry
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.*
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.RelationV6Dto
import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteWithMainAddressVerboseDto
import java.time.Instant

class ExpectedGateResultV6Factory {

    private val anyTime = Instant.MIN

    fun buildBusinessPartnerInputCreateResult(request: BusinessPartnerInputV6Request): BusinessPartnerInputV6Dto{
        return with(request){
            BusinessPartnerInputV6Dto(
                externalId = externalId,
                nameParts = nameParts,
                identifiers = identifiers,
                states = states,
                roles = roles,
                isOwnCompanyData = isOwnCompanyData,
                legalEntity = legalEntity,
                site = site,
                address = address,
                externalSequenceTimestamp = externalSequenceTimestamp,
                createdAt = anyTime,
                updatedAt = anyTime
            )
        }
    }

    fun buildBusinessPartnerOutput(input: BusinessPartnerInputV6Dto, goldenRecordLegalEntity: LegalEntityWithLegalAddressVerboseDto, siteGoldenRecord: SiteWithMainAddressVerboseDto, addressGoldenRecord: LogisticAddressInvariantVerboseDto): BusinessPartnerOutputV6Dto{
        return BusinessPartnerOutputV6Dto(
            externalId = input.externalId,
            nameParts = input.nameParts,
            identifiers = addressGoldenRecord.identifiers.map { BusinessPartnerIdentifierV6Dto(it.type, it.value, null) },
            states = addressGoldenRecord.states.map { BusinessPartnerStateV6Dto(it.validFrom, it.validTo, it.type) },
            roles = input.roles,
            isOwnCompanyData = input.isOwnCompanyData,
            legalEntity = buildLegalEntityRepresentation(goldenRecordLegalEntity.header),
            site = buildSiteRepresentation(siteGoldenRecord.site),
            address = buildAddressComponent(addressGoldenRecord, AddressType.AdditionalAddress),
            externalSequenceTimestamp = null,
            createdAt = Instant.MIN,
            updatedAt = Instant.MIN
        )
    }

    fun buildRelationDto(putRequest: RelationPutV6Entry): RelationV6Dto{
        return RelationV6Dto(
            externalId = putRequest.externalId,
            relationType = putRequest.relationType,
            businessPartnerSourceExternalId = putRequest.businessPartnerSourceExternalId,
            businessPartnerTargetExternalId = putRequest.businessPartnerTargetExternalId,
            updatedAt = Instant.now(),
            createdAt = Instant.now()
        )
    }

    fun buildRelationDto(postRequest: RelationPostV6Request): RelationV6Dto{
        return RelationV6Dto(
            externalId = postRequest.externalId!!,
            relationType = postRequest.relationType,
            businessPartnerSourceExternalId = postRequest.businessPartnerSourceExternalId,
            businessPartnerTargetExternalId = postRequest.businessPartnerTargetExternalId,
            updatedAt = Instant.now(),
            createdAt = Instant.now()
        )
    }

    private fun buildLegalEntityRepresentation(legalEntity: LegalEntityHeaderVerboseDto): LegalEntityRepresentationOutputV6Dto{
        return LegalEntityRepresentationOutputV6Dto(
            legalEntityBpn = legalEntity.bpnl,
            legalName = legalEntity.legalName,
            shortName = legalEntity.legalShortName,
            legalForm = legalEntity.legalForm,
            confidenceCriteria = buildConfidence(legalEntity.confidenceCriteria),
            states = legalEntity.states.map { BusinessPartnerStateV6Dto(it.validFrom, it.validTo, it.type) }
        )
    }

    private fun buildAddressComponent(logisticAddress: LogisticAddressInvariantVerboseDto, addressType: AddressType): AddressComponentOutputV6Dto{
        return AddressComponentOutputV6Dto(
            addressBpn = logisticAddress.bpna,
            name = logisticAddress.name,
            addressType = addressType,
            physicalPostalAddress = buildPhysicalAddress(logisticAddress.physicalPostalAddress),
            alternativePostalAddress = logisticAddress.alternativePostalAddress?.let { buildAlternativeAddress(it) },
            confidenceCriteria = buildConfidence(logisticAddress.confidenceCriteria),
            states = logisticAddress.states.map { BusinessPartnerStateV6Dto(it.validFrom, it.validTo, it.type) }
        )
    }

    private fun buildPhysicalAddress(physicalAddress: PhysicalPostalAddressVerboseDto): PhysicalPostalAddressV6Dto{
        return PhysicalPostalAddressV6Dto(
            geographicCoordinates = physicalAddress.geographicCoordinates,
            country = physicalAddress.country,
            administrativeAreaLevel1 = physicalAddress.administrativeAreaLevel1,
            administrativeAreaLevel2 = physicalAddress.administrativeAreaLevel2,
            administrativeAreaLevel3 = physicalAddress.administrativeAreaLevel3,
            postalCode = physicalAddress.postalCode,
            city = physicalAddress.city,
            district = physicalAddress.district,
            street = physicalAddress.street?.let { buildStreet(it) },
            companyPostalCode = physicalAddress.companyPostalCode,
            industrialZone = physicalAddress.industrialZone,
            building = physicalAddress.building,
            floor = physicalAddress.floor,
            door = physicalAddress.door,
            taxJurisdictionCode = physicalAddress.taxJurisdictionCode
        )
    }

    private fun buildAlternativeAddress(alternativeAddress: AlternativePostalAddressVerboseDto): AlternativePostalAddressV6Dto{
        return AlternativePostalAddressV6Dto(
            geographicCoordinates = alternativeAddress.geographicCoordinates,
            country = alternativeAddress.country,
            administrativeAreaLevel1 = alternativeAddress.administrativeAreaLevel1,
            postalCode = alternativeAddress.postalCode,
            city = alternativeAddress.city,
            deliveryServiceType = alternativeAddress.deliveryServiceType,
            deliveryServiceQualifier = alternativeAddress.deliveryServiceQualifier,
            deliveryServiceNumber = alternativeAddress.deliveryServiceNumber
        )
    }

    private fun buildStreet(street: org.eclipse.tractusx.bpdm.pool.api.model.StreetDto): StreetV6Dto{
        return StreetV6Dto(
            name = street.name,
            houseNumber = street.houseNumber,
            houseNumberSupplement = street.houseNumberSupplement,
            milestone = street.milestone,
            direction = street.direction,
            namePrefix = street.namePrefix,
            additionalNamePrefix = street.additionalNamePrefix,
            nameSuffix = street.nameSuffix,
            additionalNameSuffix = street.additionalNameSuffix
        )
    }

    private fun buildSiteRepresentation(site: SiteVerboseDto): SiteRepresentationOutputV6Dto {
        return SiteRepresentationOutputV6Dto(
            siteBpn = site.bpns,
            name = site.name,
            confidenceCriteria = buildConfidence(site.confidenceCriteria),
            states = site.states.map { BusinessPartnerStateV6Dto(it.validFrom, it.validTo, it.type) }
        )
    }

    private fun buildConfidence(confidence: org.eclipse.tractusx.bpdm.pool.api.model.ConfidenceCriteriaDto): ConfidenceCriteriaV6Dto{
        return with(confidence){
            ConfidenceCriteriaV6Dto(
                sharedByOwner = sharedByOwner,
                checkedByExternalDataSource = checkedByExternalDataSource,
                numberOfSharingMembers = numberOfSharingMembers,
                lastConfidenceCheckAt = lastConfidenceCheckAt,
                nextConfidenceCheckAt = nextConfidenceCheckAt,
                confidenceLevel = confidenceLevel
            )
        }
    }
}