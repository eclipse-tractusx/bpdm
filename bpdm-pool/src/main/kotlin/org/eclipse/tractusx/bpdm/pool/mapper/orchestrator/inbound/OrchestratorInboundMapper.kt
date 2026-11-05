/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.inbound

import org.eclipse.tractusx.bpdm.pool.model.raw.task.AdditionalSiteRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.AddressUpsertRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.BpnReferenceKind
import org.eclipse.tractusx.bpdm.pool.model.raw.task.BpnReferenceRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.BusinessStateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.GoldenRecordPassThroughRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.GoldenRecordRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.GoldenRecordTaskRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.LegalEntityHeaderRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.LegalEntityUpsertRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.NamePartKind
import org.eclipse.tractusx.bpdm.pool.model.raw.task.NamePartRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.SiteHeaderRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.SiteUpsertRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedAddressRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedAddressRelationKind
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedAddressRelationRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedAddressScriptVariantRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedAlternativeAddressRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedAlternativeAddressScriptVariantRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedConfidenceCriteriaRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedGeoCoordinateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedIdentifierRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedPhysicalAddressRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedPhysicalAddressScriptVariantRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedStreetRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedStreetScriptVariantRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UpsertIntent
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityIdentifier
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityScriptVariant
import org.eclipse.tractusx.bpdm.pool.model.request.SiteScriptVariant
import org.eclipse.tractusx.orchestrator.api.model.AddressGoldenRecordRelationType
import org.eclipse.tractusx.orchestrator.api.model.AlternativeAddress
import org.eclipse.tractusx.orchestrator.api.model.BpnReference
import org.eclipse.tractusx.orchestrator.api.model.BpnReferenceType
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartner
import org.eclipse.tractusx.orchestrator.api.model.BusinessState
import org.eclipse.tractusx.orchestrator.api.model.GeoCoordinate
import org.eclipse.tractusx.orchestrator.api.model.Identifier
import org.eclipse.tractusx.orchestrator.api.model.LegalEntity
import org.eclipse.tractusx.orchestrator.api.model.NamePartType
import org.eclipse.tractusx.orchestrator.api.model.PhysicalAddress
import org.eclipse.tractusx.orchestrator.api.model.PostalAddressScriptVariantWithScriptCode
import org.eclipse.tractusx.orchestrator.api.model.PostalAddressWithScriptVariants
import org.eclipse.tractusx.orchestrator.api.model.Site
import org.eclipse.tractusx.orchestrator.api.model.TaskStepReservationEntryDto
import org.springframework.stereotype.Component

/**
 * Translates the golden record tasks the Orchestrator hands to the Pool into the Pool's raw model.
 */
@Component
class OrchestratorInboundMapper(
    private val addressRequestMapper: GoldenRecordTaskAddressRequestMapper
) {

    /**
     * Returns the raw golden record task [task] carries.
     */
    fun toRaw(task: TaskStepReservationEntryDto): GoldenRecordTaskRaw =
        GoldenRecordTaskRaw(
            taskId = task.taskId,
            sharingMemberRecordId = task.recordId,
            goldenRecord = toGoldenRecord(task.businessPartner),
            passThrough = toPassThrough(task.businessPartner)
        )

    private fun toGoldenRecord(businessPartner: BusinessPartner): GoldenRecordRaw =
        GoldenRecordRaw(
            legalEntity = toLegalEntity(businessPartner.legalEntity),
            site = businessPartner.site?.let { toSite(it) },
            additionalAddress = businessPartner.additionalAddress?.let { toAdditionalAddress(it) },
            additionalSites = businessPartner.additionalSites.map { AdditionalSiteRaw(toReference(it.bpnReference), it.siteName) }
        )

    private fun toLegalEntity(legalEntity: LegalEntity): LegalEntityUpsertRaw =
        LegalEntityUpsertRaw(
            reference = toReference(legalEntity.bpnReference),
            header = LegalEntityHeaderRaw(
                legalName = legalEntity.legalName,
                legalShortName = legalEntity.legalShortName,
                legalForm = legalEntity.legalForm,
                identifiers = legalEntity.identifiers.map { LegalEntityIdentifier(it.value, it.type, it.issuingBody) },
                states = legalEntity.states.map { toState(it) },
                confidenceCriteria = addressRequestMapper.toConfidenceRequest(legalEntity.confidenceCriteria),
                isDataSpaceParticipant = legalEntity.isParticipantData,
                ownershipUltimate = legalEntity.ownershipUltimate,
                scriptVariants = legalEntity.scriptVariants.map { LegalEntityScriptVariant(it.scriptCode, it.legalName, it.legalShortName) }
            ),
            legalAddress = AddressUpsertRaw(
                reference = toReference(legalEntity.legalAddress.bpnReference),
                content = addressRequestMapper.toContentRequest(
                    legalEntity.legalAddress,
                    legalEntity.scriptVariants.map { PostalAddressScriptVariantWithScriptCode(it.scriptCode, it.legalAddress) }
                )
            ),
            intent = toIntent(legalEntity.hasChanged)
        )

    private fun toSite(site: Site): SiteUpsertRaw {
        val reference = toReference(site.bpnReference)
        val header = SiteHeaderRaw(
            name = site.siteName,
            states = site.states.map { toState(it) },
            confidenceCriteria = addressRequestMapper.toConfidenceRequest(site.confidenceCriteria),
            scriptVariants = site.scriptVariants.map { SiteScriptVariant(it.scriptCode, it.siteName) }
        )
        val mainAddressScriptVariants = site.scriptVariants.map { PostalAddressScriptVariantWithScriptCode(it.scriptCode, it.mainAddress) }
        val intent = toIntent(site.hasChanged)

        return when (val mainAddress = site.siteMainAddress) {
            null -> SiteUpsertRaw.WithLegalAddressAsMain(
                reference = reference,
                header = header,
                intent = intent,
                mainAddressScriptVariants = mainAddressScriptVariants.map { addressRequestMapper.toScriptVariant(it) }
            )
            else -> SiteUpsertRaw.WithOwnMainAddress(
                reference = reference,
                header = header,
                intent = intent,
                mainAddress = AddressUpsertRaw(toReference(mainAddress.bpnReference), addressRequestMapper.toContentRequest(mainAddress, mainAddressScriptVariants))
            )
        }
    }

    private fun toAdditionalAddress(address: PostalAddressWithScriptVariants): AddressUpsertRaw =
        AddressUpsertRaw(toReference(address.bpnReference), addressRequestMapper.toContentRequest(address))

    private fun toIntent(hasChanged: Boolean?): UpsertIntent =
        if (hasChanged == false) UpsertIntent.WriteOnlyIfAbsent else UpsertIntent.AlwaysWrite

    private fun toReference(reference: BpnReference): BpnReferenceRaw =
        BpnReferenceRaw(
            value = reference.referenceValue,
            kind = reference.referenceType?.let {
                when (it) {
                    BpnReferenceType.Bpn -> BpnReferenceKind.Bpn
                    BpnReferenceType.BpnRequestIdentifier -> BpnReferenceKind.RequestIdentifier
                }
            }
        )

    private fun toState(state: BusinessState): BusinessStateRaw =
        BusinessStateRaw(validFrom = state.validFrom, validTo = state.validTo, type = state.type)

    private fun toPassThrough(businessPartner: BusinessPartner): GoldenRecordPassThroughRaw =
        GoldenRecordPassThroughRaw(
            nameParts = businessPartner.nameParts.map { NamePartRaw(it.name, toNamePartKind(it.type)) },
            owningCompany = businessPartner.owningCompany,
            uncategorized = with(businessPartner.uncategorized) {
                UncategorizedRaw(
                    nameParts = nameParts,
                    identifiers = identifiers.map { toIdentifier(it) },
                    states = states.map { toState(it) },
                    address = address?.let { toUncategorizedAddress(it) }
                )
            }
        )

    private fun toNamePartKind(type: NamePartType): NamePartKind =
        when (type) {
            NamePartType.LegalName -> NamePartKind.LegalName
            NamePartType.ShortName -> NamePartKind.ShortName
            NamePartType.LegalForm -> NamePartKind.LegalForm
            NamePartType.SiteName -> NamePartKind.SiteName
            NamePartType.AddressName -> NamePartKind.AddressName
        }

    private fun toIdentifier(identifier: Identifier): UncategorizedIdentifierRaw =
        UncategorizedIdentifierRaw(identifier.value, identifier.type, identifier.issuingBody)

    private fun toUncategorizedAddress(address: PostalAddressWithScriptVariants): UncategorizedAddressRaw =
        with(address.postalProperties) {
            UncategorizedAddressRaw(
                bpnReferenceValue = bpnReference.referenceValue,
                bpnReferenceDesiredBpn = bpnReference.desiredBpn,
                bpnReferenceKind = toReference(bpnReference).kind,
                addressName = addressName,
                identifiers = identifiers.map { toIdentifier(it) },
                states = states.map { toState(it) },
                confidenceCriteria = with(confidenceCriteria) {
                    UncategorizedConfidenceCriteriaRaw(
                        sharedByOwner, checkedByExternalDataSource, numberOfSharingMembers, lastConfidenceCheckAt, nextConfidenceCheckAt, confidenceLevel
                    )
                },
                physicalAddress = toUncategorizedPhysicalAddress(physicalAddress),
                alternativeAddress = alternativeAddress?.let { toUncategorizedAlternativeAddress(it) },
                hasChanged = hasChanged,
                relations = goldenRecordRelations.map {
                    val type = when (it.relationType) {
                        AddressGoldenRecordRelationType.IsReplacedBy -> UncategorizedAddressRelationKind.IsReplacedBy
                    }
                    UncategorizedAddressRelationRaw(type, it.sourceBpn, it.targetBpn)
                },
                updatedAt = updatedAt,
                scriptVariants = address.scriptVariants.map { toUncategorizedScriptVariant(it) }
            )
        }

    private fun toUncategorizedPhysicalAddress(address: PhysicalAddress): UncategorizedPhysicalAddressRaw =
        with(address) {
            UncategorizedPhysicalAddressRaw(
                geographicCoordinates = toGeoCoordinate(geographicCoordinates),
                country = country,
                administrativeAreaLevel1 = administrativeAreaLevel1,
                administrativeAreaLevel2 = administrativeAreaLevel2,
                administrativeAreaLevel3 = administrativeAreaLevel3,
                postalCode = postalCode,
                city = city,
                district = district,
                street = with(street) {
                    UncategorizedStreetRaw(
                        name, houseNumber, houseNumberSupplement, milestone, direction, namePrefix, additionalNamePrefix, nameSuffix, additionalNameSuffix
                    )
                },
                companyPostalCode = companyPostalCode,
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door,
                taxJurisdictionCode = taxJurisdictionCode
            )
        }

    private fun toUncategorizedAlternativeAddress(address: AlternativeAddress): UncategorizedAlternativeAddressRaw =
        with(address) {
            UncategorizedAlternativeAddressRaw(
                geographicCoordinates = toGeoCoordinate(geographicCoordinates),
                country = country,
                administrativeAreaLevel1 = administrativeAreaLevel1,
                postalCode = postalCode,
                city = city,
                deliveryServiceType = deliveryServiceType,
                deliveryServiceQualifier = deliveryServiceQualifier,
                deliveryServiceNumber = deliveryServiceNumber
            )
        }

    private fun toGeoCoordinate(coordinate: GeoCoordinate): UncategorizedGeoCoordinateRaw =
        UncategorizedGeoCoordinateRaw(coordinate.longitude, coordinate.latitude, coordinate.altitude)

    private fun toUncategorizedScriptVariant(variant: PostalAddressScriptVariantWithScriptCode): UncategorizedAddressScriptVariantRaw =
        with(variant.postalProperties) {
            UncategorizedAddressScriptVariantRaw(
                scriptCode = variant.scriptCode,
                addressName = addressName,
                physicalAddress = with(physicalAddress) {
                    UncategorizedPhysicalAddressScriptVariantRaw(
                        city = city,
                        district = district,
                        street = with(street) {
                            UncategorizedStreetScriptVariantRaw(name, direction, namePrefix, additionalNamePrefix, nameSuffix, additionalNameSuffix)
                        },
                        industrialZone = industrialZone,
                        building = building,
                        floor = floor,
                        door = door
                    )
                },
                alternativeAddress = alternativeAddress?.let { UncategorizedAlternativeAddressScriptVariantRaw(it.city) }
            )
        }
}
