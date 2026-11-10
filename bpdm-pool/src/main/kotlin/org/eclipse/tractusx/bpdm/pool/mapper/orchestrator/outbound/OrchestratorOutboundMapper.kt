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
 * SPDX-License-IdentifierV7Dto: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.outbound

import org.eclipse.tractusx.bpdm.pool.model.raw.relation.AddressRelationType
import org.eclipse.tractusx.bpdm.pool.model.raw.relation.SiteRelationType
import org.eclipse.tractusx.bpdm.pool.model.raw.relation.LegalEntityRelationType
import org.eclipse.tractusx.bpdm.common.dto.BusinessStateDates
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.*
import org.eclipse.tractusx.bpdm.pool.model.output.address.AddressRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.AddressScriptVariantRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.AlternativePostalAddressRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.GeoCoordinateRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.PhysicalPostalAddressRecord
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.BusinessStateRecord
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.ConfidenceCriteriaRecord
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityRecord
import org.eclipse.tractusx.bpdm.pool.model.output.site.SiteRecord
import org.eclipse.tractusx.bpdm.pool.model.output.task.GoldenRecordTaskResultRecord
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.GoldenRecordTaskError
import org.eclipse.tractusx.bpdm.pool.model.raw.task.BpnReferenceKind
import org.eclipse.tractusx.bpdm.pool.model.raw.task.BusinessStateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.GoldenRecordPassThroughRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.NamePartKind
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedAddressRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedAddressRelationKind
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedGeoCoordinateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedIdentifierRaw
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.AdditionalSiteV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.AddressGoldenRecordRelationV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.AddressGoldenRecordRelationTypeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.AlternativeAddressV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.AlternativeAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BpnReferenceV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BpnReferenceTypeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BusinessPartnerV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BusinessStateV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.ConfidenceCriteriaV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.GeoCoordinateV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.IdentifierV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.LegalEntityV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.LegalEntityGoldenRecordRelationV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.LegalEntityGoldenRecordRelationTypeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.LegalEntityScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.NamePartV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.NamePartTypeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PhysicalAddressV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PhysicalAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PostalAddressV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PostalAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PostalAddressScriptVariantWithScriptCodeV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PostalAddressWithScriptVariantsV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.SiteV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.SiteGoldenRecordRelationV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.SiteGoldenRecordRelationTypeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.SiteScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.StreetV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.StreetScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskErrorV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskErrorTypeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepResultEntryV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.UncategorizedPropertiesV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.UncategorizedScriptVariantV7Dto
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Translates the outcomes of golden record tasks into the results the Orchestrator expects from the Pool.
 */
@Component
class OrchestratorOutboundMapper {

    /**
     * Returns the task step result that reports [result] to the Orchestrator.
     */
    fun toResultEntry(result: GoldenRecordTaskResultRecord): TaskStepResultEntryV7Dto =
        when (result) {
            is GoldenRecordTaskResultRecord.Resolved -> TaskStepResultEntryV7Dto(result.taskId, toBusinessPartner(result))
            is GoldenRecordTaskResultRecord.Rejected -> toErrorEntry(result.taskId, result.errors.map { toDescription(it) })
            is GoldenRecordTaskResultRecord.Failed -> toErrorEntry(result.taskId, listOf(UNEXPECTED_TASK_ERROR))
        }

    /**
     * Returns the task step result that reports the golden record task with [taskId] as failed without a known cause.
     */
    fun toErrorReportEntry(taskId: String): TaskStepResultEntryV7Dto =
        toErrorEntry(taskId, listOf(UNREPORTED_TASK_ERROR))

    private fun toErrorEntry(taskId: String, descriptions: List<String>) =
        TaskStepResultEntryV7Dto(
            taskId = taskId,
            businessPartner = BusinessPartnerV7Dto.empty,
            errors = descriptions.map { TaskErrorV7Dto(type = TaskErrorTypeV7.Unspecified, description = it) }
        )

    private fun toBusinessPartner(result: GoldenRecordTaskResultRecord.Resolved): BusinessPartnerV7Dto {
        val legalEntity = toLegalEntity(result.legalEntity, result.isLegalEntityWritten)
        val site = result.site?.let { toSite(it, result.isSiteWritten) }
        // A site whose main address is the legal address states no main address of its own; the legal address is reported
        // as the site wrote it, which is newer than the legal entity's view when only the site changed.
        val siteSharesLegalAddress = result.site != null && result.site.mainAddress.bpn == result.legalEntity.legalAddress.bpn

        return BusinessPartnerV7Dto(
            nameParts = result.passThrough.nameParts.map { NamePartV7Dto(it.name, toNamePartType(it.type)) },
            owningCompany = result.passThrough.owningCompany,
            uncategorized = toUncategorized(result.passThrough),
            legalEntity = if (siteSharesLegalAddress) legalEntity.copy(legalAddress = site!!.siteMainAddress!!) else legalEntity,
            site = if (siteSharesLegalAddress) site!!.copy(siteMainAddress = null) else site,
            additionalAddress = result.additionalAddress?.let { toAddressWithScriptVariants(it) },
            additionalSites = result.additionalSites.map { AdditionalSiteV7Dto(BpnReferenceV7Dto(it.bpn, null, BpnReferenceTypeV7.Bpn), it.name) }
        )
    }

    private fun toLegalEntity(record: LegalEntityRecord, isWritten: Boolean): LegalEntityV7Dto =
        LegalEntityV7Dto(
            bpnReference = toBpnReference(record.bpn),
            legalName = record.legalName,
            legalShortName = record.legalShortName,
            legalForm = record.legalForm?.technicalKey,
            identifiers = record.identifiers.map { IdentifierV7Dto(it.value, it.typeTechnicalKey, it.issuingBody) },
            states = record.states.map { toState(it) },
            confidenceCriteria = toConfidenceCriteria(record.confidenceCriteria),
            isParticipantData = record.isDataSpaceParticipant,
            hasChanged = isWritten,
            ownershipUltimate = record.ownershipUltimate,
            ultimateOwnerBpnl = record.ultimateOwnerBpnl,
            legalAddress = toPostalAddress(record.legalAddress, isWritten),
            scriptVariants = record.scriptVariants.map {
                LegalEntityScriptVariantV7Dto(it.scriptCode, it.legalName, it.shortName, toPostalScriptVariant(it.legalAddress))
            },
            goldenRecordRelations = record.relations
                .distinctBy { Triple(it.type, it.sourceBpnl, it.targetBpnl) }
                .map { LegalEntityGoldenRecordRelationV7Dto(toRelationType(it.type), it.sourceBpnl, it.targetBpnl) },
            updatedAt = record.updatedAt
        )

    private fun toSite(record: SiteRecord, isWritten: Boolean): SiteV7Dto =
        SiteV7Dto(
            bpnReference = toBpnReference(record.bpn),
            siteName = record.name,
            states = record.states.map { toState(it) },
            confidenceCriteria = toConfidenceCriteria(record.confidenceCriteria),
            hasChanged = isWritten,
            siteMainAddress = toPostalAddress(record.mainAddress, isWritten),
            scriptVariants = record.scriptVariants.map { SiteScriptVariantV7Dto(it.scriptCode, it.name, toPostalScriptVariant(it.mainAddress)) },
            goldenRecordRelations = record.relations
                .distinctBy { Triple(it.type, it.sourceBpns, it.targetBpns) }
                .map { SiteGoldenRecordRelationV7Dto(toRelationType(it.type), it.sourceBpns, it.targetBpns) },
            updatedAt = record.updatedAt
        )

    // An additional address stated by a task is always written.
    private fun toAddressWithScriptVariants(record: AddressRecord): PostalAddressWithScriptVariantsV7Dto =
        PostalAddressWithScriptVariantsV7Dto(
            postalProperties = toPostalAddress(record, isWritten = true),
            scriptVariants = record.scriptVariants.map { PostalAddressScriptVariantWithScriptCodeV7Dto(it.scriptCode, toPostalScriptVariant(it)) }
        )

    private fun toPostalAddress(record: AddressRecord, isWritten: Boolean): PostalAddressV7Dto =
        PostalAddressV7Dto(
            bpnReference = toBpnReference(record.bpn),
            addressName = record.name,
            identifiers = record.identifiers.map { IdentifierV7Dto(it.value, it.typeTechnicalKey, null) },
            states = record.states.map { toState(it) },
            confidenceCriteria = toConfidenceCriteria(record.confidenceCriteria),
            physicalAddress = toPhysicalAddress(record.physicalPostalAddress),
            alternativeAddress = record.alternativePostalAddress?.let { toAlternativeAddress(it) },
            hasChanged = isWritten,
            goldenRecordRelations = record.relations
                .distinctBy { Triple(it.type, it.sourceBpna, it.targetBpna) }
                .map { AddressGoldenRecordRelationV7Dto(toRelationType(it.type), it.sourceBpna, it.targetBpna) },
            updatedAt = record.updatedAt
        )

    private fun toPhysicalAddress(record: PhysicalPostalAddressRecord): PhysicalAddressV7Dto =
        PhysicalAddressV7Dto(
            geographicCoordinates = record.geographicCoordinates?.let { toGeoCoordinate(it) } ?: GeoCoordinateV7Dto.empty,
            country = record.country.countryCode,
            administrativeAreaLevel1 = record.administrativeAreaLevel1?.code,
            administrativeAreaLevel2 = record.administrativeAreaLevel2,
            administrativeAreaLevel3 = record.administrativeAreaLevel3,
            postalCode = record.postalCode,
            city = record.city,
            district = record.district,
            street = record.street?.let {
                StreetV7Dto(
                    it.name, it.houseNumber, it.houseNumberSupplement, it.milestone, it.direction, it.namePrefix,
                    it.additionalNamePrefix, it.nameSuffix, it.additionalNameSuffix
                )
            } ?: StreetV7Dto.empty,
            companyPostalCode = record.companyPostalCode,
            industrialZone = record.industrialZone,
            building = record.building,
            floor = record.floor,
            door = record.door,
            taxJurisdictionCode = record.taxJurisdictionCode
        )

    private fun toAlternativeAddress(record: AlternativePostalAddressRecord): AlternativeAddressV7Dto =
        AlternativeAddressV7Dto(
            geographicCoordinates = record.geographicCoordinates?.let { toGeoCoordinate(it) } ?: GeoCoordinateV7Dto.empty,
            country = record.country.countryCode,
            administrativeAreaLevel1 = record.administrativeAreaLevel1?.code,
            postalCode = record.postalCode,
            city = record.city,
            deliveryServiceType = record.deliveryServiceType,
            deliveryServiceQualifier = record.deliveryServiceQualifier,
            deliveryServiceNumber = record.deliveryServiceNumber
        )

    private fun toGeoCoordinate(record: GeoCoordinateRecord): GeoCoordinateV7Dto =
        GeoCoordinateV7Dto(record.longitude, record.latitude, record.altitude)

    private fun toPostalScriptVariant(record: AddressScriptVariantRecord): PostalAddressScriptVariantV7Dto =
        PostalAddressScriptVariantV7Dto(
            addressName = record.name,
            physicalAddress = with(record.physicalAddress) {
                PhysicalAddressScriptVariantV7Dto(
                    city = city,
                    district = district,
                    street = street?.let {
                        StreetScriptVariantV7Dto(it.name, it.direction, it.namePrefix, it.additionalNamePrefix, it.nameSuffix, it.additionalNameSuffix)
                    } ?: StreetScriptVariantV7Dto.empty,
                    industrialZone = industrialZone,
                    building = building,
                    floor = floor,
                    door = door
                )
            },
            alternativeAddress = record.alternativeAddress?.let { AlternativeAddressScriptVariantV7Dto(it.city) }
        )

    private fun toState(record: BusinessStateRecord): BusinessStateV7Dto =
        BusinessStateV7Dto(
            validFrom = BusinessStateDates.reportValidFrom(record.validFrom).toUtcInstant(),
            validTo = BusinessStateDates.reportValidTo(record.validTo).toUtcInstant(),
            type = record.type
        )

    private fun toConfidenceCriteria(record: ConfidenceCriteriaRecord): ConfidenceCriteriaV7Dto =
        ConfidenceCriteriaV7Dto(
            sharedByOwner = record.sharedByOwner,
            checkedByExternalDataSource = record.checkedByExternalDataSource,
            numberOfSharingMembers = record.numberOfSharingMembers,
            lastConfidenceCheckAt = record.lastConfidenceCheckAt.toUtcInstant(),
            nextConfidenceCheckAt = record.nextConfidenceCheckAt.toUtcInstant(),
            confidenceLevel = record.confidenceLevel
        )

    private fun toRelationType(type: LegalEntityRelationType): LegalEntityGoldenRecordRelationTypeV7 =
        when (type) {
            LegalEntityRelationType.IsAlternativeHeadquarterFor -> LegalEntityGoldenRecordRelationTypeV7.IsAlternativeHeadquarterFor
            LegalEntityRelationType.IsManagedBy -> LegalEntityGoldenRecordRelationTypeV7.IsManagedBy
            LegalEntityRelationType.IsOwnedBy -> LegalEntityGoldenRecordRelationTypeV7.IsOwnedBy
            LegalEntityRelationType.IsReplacedBy -> LegalEntityGoldenRecordRelationTypeV7.IsReplacedBy
        }

    private fun toRelationType(type: SiteRelationType): SiteGoldenRecordRelationTypeV7 =
        when (type) {
            SiteRelationType.IsReplacedBy -> SiteGoldenRecordRelationTypeV7.IsReplacedBy
        }

    private fun toRelationType(type: AddressRelationType): AddressGoldenRecordRelationTypeV7 =
        when (type) {
            AddressRelationType.IsReplacedBy -> AddressGoldenRecordRelationTypeV7.IsReplacedBy
        }

    private fun toBpnReference(bpn: String) = BpnReferenceV7Dto(bpn, null, BpnReferenceTypeV7.Bpn)

    private fun LocalDateTime.toUtcInstant() = toInstant(ZoneOffset.UTC)

    private fun toNamePartType(kind: NamePartKind): NamePartTypeV7 =
        when (kind) {
            NamePartKind.LegalName -> NamePartTypeV7.LegalName
            NamePartKind.ShortName -> NamePartTypeV7.ShortName
            NamePartKind.LegalForm -> NamePartTypeV7.LegalForm
            NamePartKind.SiteName -> NamePartTypeV7.SiteName
            NamePartKind.AddressName -> NamePartTypeV7.AddressName
        }

    private fun toUncategorized(passThrough: GoldenRecordPassThroughRaw): UncategorizedPropertiesV7Dto =
        with(passThrough.uncategorized) {
            UncategorizedPropertiesV7Dto(
                nameParts = nameParts,
                identifiers = identifiers.map { toIdentifier(it) },
                states = states.map { toState(it) },
                address = address?.let { toUncategorizedAddress(it) },
                scriptVariants = scriptVariants.map { UncategorizedScriptVariantV7Dto(it.scriptCode, it.nameParts) }
            )
        }

    private fun toIdentifier(raw: UncategorizedIdentifierRaw) = IdentifierV7Dto(raw.value, raw.type, raw.issuingBody)

    private fun toState(raw: BusinessStateRaw) = BusinessStateV7Dto(raw.validFrom, raw.validTo, raw.type)

    private fun toUncategorizedAddress(raw: UncategorizedAddressRaw): PostalAddressWithScriptVariantsV7Dto =
        PostalAddressWithScriptVariantsV7Dto(
            postalProperties = PostalAddressV7Dto(
                bpnReference = BpnReferenceV7Dto(
                    referenceValue = raw.bpnReferenceValue,
                    desiredBpn = raw.bpnReferenceDesiredBpn,
                    referenceType = raw.bpnReferenceKind?.let {
                        when (it) {
                            BpnReferenceKind.Bpn -> BpnReferenceTypeV7.Bpn
                            BpnReferenceKind.RequestIdentifier -> BpnReferenceTypeV7.BpnRequestIdentifier
                        }
                    }
                ),
                addressName = raw.addressName,
                identifiers = raw.identifiers.map { toIdentifier(it) },
                states = raw.states.map { toState(it) },
                confidenceCriteria = with(raw.confidenceCriteria) {
                    ConfidenceCriteriaV7Dto(sharedByOwner, checkedByExternalDataSource, numberOfSharingMembers, lastConfidenceCheckAt, nextConfidenceCheckAt, confidenceLevel)
                },
                physicalAddress = with(raw.physicalAddress) {
                    PhysicalAddressV7Dto(
                        geographicCoordinates = toGeoCoordinate(geographicCoordinates),
                        country = country,
                        administrativeAreaLevel1 = administrativeAreaLevel1,
                        administrativeAreaLevel2 = administrativeAreaLevel2,
                        administrativeAreaLevel3 = administrativeAreaLevel3,
                        postalCode = postalCode,
                        city = city,
                        district = district,
                        street = with(street) {
                            StreetV7Dto(name, houseNumber, houseNumberSupplement, milestone, direction, namePrefix, additionalNamePrefix, nameSuffix, additionalNameSuffix)
                        },
                        companyPostalCode = companyPostalCode,
                        industrialZone = industrialZone,
                        building = building,
                        floor = floor,
                        door = door,
                        taxJurisdictionCode = taxJurisdictionCode
                    )
                },
                alternativeAddress = raw.alternativeAddress?.let {
                    AlternativeAddressV7Dto(
                        geographicCoordinates = toGeoCoordinate(it.geographicCoordinates),
                        country = it.country,
                        administrativeAreaLevel1 = it.administrativeAreaLevel1,
                        postalCode = it.postalCode,
                        city = it.city,
                        deliveryServiceType = it.deliveryServiceType,
                        deliveryServiceQualifier = it.deliveryServiceQualifier,
                        deliveryServiceNumber = it.deliveryServiceNumber
                    )
                },
                hasChanged = raw.hasChanged,
                goldenRecordRelations = raw.relations.map {
                    val type = when (it.type) {
                        UncategorizedAddressRelationKind.IsReplacedBy -> AddressGoldenRecordRelationTypeV7.IsReplacedBy
                    }
                    AddressGoldenRecordRelationV7Dto(type, it.sourceBpn, it.targetBpn)
                },
                updatedAt = raw.updatedAt
            ),
            scriptVariants = raw.scriptVariants.map { variant ->
                PostalAddressScriptVariantWithScriptCodeV7Dto(
                    scriptCode = variant.scriptCode,
                    postalProperties = PostalAddressScriptVariantV7Dto(
                        addressName = variant.addressName,
                        physicalAddress = with(variant.physicalAddress) {
                            PhysicalAddressScriptVariantV7Dto(
                                city = city,
                                district = district,
                                street = with(street) {
                                    StreetScriptVariantV7Dto(name, direction, namePrefix, additionalNamePrefix, nameSuffix, additionalNameSuffix)
                                },
                                industrialZone = industrialZone,
                                building = building,
                                floor = floor,
                                door = door
                            )
                        },
                        alternativeAddress = variant.alternativeAddress?.let { AlternativeAddressScriptVariantV7Dto(it.city) }
                    )
                )
            }
        )

    private fun toGeoCoordinate(raw: UncategorizedGeoCoordinateRaw) = GeoCoordinateV7Dto(raw.longitude, raw.latitude, raw.altitude)

    private fun toDescription(error: GoldenRecordTaskError): String =
        when (error) {
            GoldenRecordTaskError.StateTypeMissing -> "Business Partner state type is null"
            is GoldenRecordTaskError.UnchangedLegalEntityNotFound -> "Legal entity with specified BPNL ${error.bpn} not found"
            is GoldenRecordTaskError.LegalEntityCreateInvalid -> describeLegalEntityCreateError(error.error)
            is GoldenRecordTaskError.LegalEntityUpdateInvalid -> describeLegalEntityUpdateError(error.error)
            is GoldenRecordTaskError.UnchangedSiteNotFound -> "Main address is null"
            GoldenRecordTaskError.SiteNotInLegalEntity -> "The legal entity is not the parent of the site"
            GoldenRecordTaskError.SiteMainAddressNotInLegalEntity -> "The legal entity is not the parent of the site main address"
            GoldenRecordTaskError.SiteMainAddressIsLegalAddress -> "The site main address is the legal address and must not be stated separately"
            is GoldenRecordTaskError.SiteCreateInvalid -> describeSiteCreateError(error.error)
            is GoldenRecordTaskError.SiteUpdateInvalid -> describeSiteUpdateError(error.error)
            GoldenRecordTaskError.AdditionalAddressNotInLegalEntity -> "The legal entity is not the parent of the additional address"
            is GoldenRecordTaskError.AdditionalAddressCreateInvalid -> "Errors on creating Address: ${describeAddressContentError(error.error)}"
            is GoldenRecordTaskError.AdditionalAddressUpdateInvalid -> "Errors on updating Address: ${describeAddressUpdateError(error.error)}"
            is GoldenRecordTaskError.AdditionalSiteCreateInvalid -> describeSiteContentError(error.error)
            is GoldenRecordTaskError.AdditionalSiteInvalid -> describeAddressSiteMembershipError(error.error)
            is GoldenRecordTaskError.AdditionalSiteOfOtherLegalEntity -> "Site ${error.siteBpn} does not belong to legal entity of this business partner"
        }

    // Each description is named after the error type it takes rather than overloading one name: the content and coverage
    // errors subtype several node error interfaces each, so overloads would resolve on static type alone and a branch
    // delegating to the wrong one would still compile, silently emitting another node's wording.
    private fun describeAddressSiteMembershipError(error: AddressSiteMembershipError): String =
        when (error) {
            is UnresolvableAddress -> "Address ${error.bpn} not found"
            is UnresolvableSite -> "Site ${error.bpn} not found"
            is SiteNotInAddressLegalEntity -> "Site ${error.siteBpn} does not belong to legal entity ${error.legalEntityBpn}"
            is SiteMainAddressOmitted -> "Site ${error.siteBpn} has this address as its main address and must be stated"
        }

    private fun describeAddressUpdateError(error: AddressUpdateError): String =
        when (error) {
            is UnresolvableAddress -> "Address ${error.bpn} not found"
            is SiteMainAddressOmitted -> "Site ${error.siteBpn} has this address as its main address and must be stated"
            is AddressContentError -> describeAddressContentError(error)
            is UnresolvableSite -> "Site parent ${error.bpn} not found"
            is SiteNotInAddressLegalEntity -> "Site ${error.siteBpn} does not belong to legal entity ${error.legalEntityBpn}"
        }

    private fun describeAddressContentError(error: AddressContentError): String =
        when (error) {
            is AddressFieldError -> describeAddressFieldError(error)
            is AddressMetadataError -> describeAddressMetadataError(error)
            is AddressConstraintError -> describeAddressConstraintError(error)
            is AddressScriptVariantError -> describeAddressScriptVariantError(error)
        }

    private fun describeAddressFieldError(error: AddressFieldError): String =
        when (error) {
            AddressFieldError.PhysicalCountryMissing -> "Physical Address has no country"
            AddressFieldError.PhysicalCityMissing -> "Physical Address has no city"
            AddressFieldError.AlternativeCountryMissing -> "Alternative Address has no country"
            AddressFieldError.AlternativeCityMissing -> "Alternative Address has no city"
            AddressFieldError.AlternativeDeliveryServiceTypeMissing -> "Alternative Address has no delivery service type"
            AddressFieldError.AlternativeDeliveryServiceNumberMissing -> "Alternative Address has no delivery service number"
            AddressFieldError.ConfidenceCriteriaMissing -> "Logistic address is missing confidence criteria"
            is AddressFieldError.IdentifierValueMissing -> "Identifier value is null"
            is AddressFieldError.IdentifierTypeMissing -> "Identifier type is null"
            is AddressFieldError.StateTypeMissing -> "Business Partner state type is null"
        }

    private fun describeAddressMetadataError(error: AddressMetadataError): String =
        when (error) {
            is AddressMetadataError.IdentifierTypeNotFound -> "Address identifier type '${error.type}' is not known"
            is AddressMetadataError.PhysicalCountryNotFound -> "Country '${error.country}' in physical address is not known"
            is AddressMetadataError.AlternativeCountryNotFound -> "Country '${error.country}' in alternative address is not known"
            is AddressMetadataError.PhysicalRegionNotFound -> "Region '${error.regionCode}' in physical address is not known"
            is AddressMetadataError.AlternativeRegionNotFound -> "Region '${error.regionCode}' in alternative address is not known"
            is AddressMetadataError.ScriptCodeNotFound -> "Script code '${error.scriptCode}' is not known"
        }

    private fun describeAddressConstraintError(error: AddressConstraintError): String =
        when (error) {
            is AddressConstraintError.IdentifiersTooMany -> "Too many identifiers: ${error.count} exceeds the allowed limit"
            is AddressConstraintError.DuplicateIdentifier -> "Duplicate identifier of type '${error.type}' with value '${error.value}'"
        }

    private fun describeAddressScriptVariantError(error: AddressScriptVariantError): String =
        when (error) {
            is AddressScriptVariantError.PhysicalCityMissing -> "Script variant ${error.index} has no city in its physical address"
            is AddressScriptVariantError.AlternativeCityMissing -> "Script variant ${error.index} has no city in its alternative address"
            is AddressScriptVariantError.DuplicateScriptCode -> "Duplicate address script variant for script code '${error.scriptCode}'"
        }

    private fun describeLegalEntityCreateError(error: LegalEntityCreateError): String =
        when (error) {
            is ScriptVariantNotCoveredByAddress -> describeLegalAddressNotCovered(error)
            is LegalEntityContentError -> describeLegalEntityContentError(error)
            is AddressContentError -> describeAddressContentError(error)
        }

    private fun describeLegalEntityUpdateError(error: LegalEntityUpdateError): String =
        when (error) {
            is UnresolvableLegalEntity -> "Legal entity ${error.bpn} not found"
            is MultipleUltimateOwnersInHierarchy ->
                "An ownership hierarchy can have at most one ultimate owner, but these legal entities are also flagged " +
                        "as ultimate owner: ${error.conflictingBpnls.joinToString(", ")}"
            is AlternativeHeadquarterCannotOwnUltimately ->
                "Legal entity ${error.bpnl} cannot carry the ultimate-owner flag because it is an alternative headquarter"
            is ScriptVariantNotCoveredByAddress -> describeLegalAddressNotCovered(error)
            is LegalEntityContentError -> describeLegalEntityContentError(error)
            is AddressContentError -> describeAddressContentError(error)
        }

    private fun describeLegalEntityContentError(error: LegalEntityContentError): String =
        when (error) {
            LegalEntityContentError.NameMissing -> "Legal name is null"
            LegalEntityContentError.ConfidenceCriteriaMissing -> "Legal Entity has no confidence criteria"
            is LegalEntityContentError.LegalFormNotFound -> "Legal form '${error.legalForm}' is not known"
            is LegalEntityContentError.IdentifierValueMissing -> "Identifier value is null"
            is LegalEntityContentError.IdentifierTypeMissing -> "Identifier type is null"
            is LegalEntityContentError.IdentifierTypeNotFound -> "Legal entity identifier type '${error.type}' is not known"
            is LegalEntityContentError.IdentifiersTooMany -> "Too many identifiers: ${error.count} exceeds the allowed limit"
            is LegalEntityContentError.DuplicateIdentifier -> "Duplicate identifier of type '${error.type}' with value '${error.value}'"
            is LegalEntityContentError.ScriptCodeNotFound -> "Script code '${error.scriptCode}' is not known"
            is LegalEntityContentError.ScriptVariantLegalNameMissing -> "Script variant ${error.index} has no legal name"
            is LegalEntityContentError.ScriptVariantDuplicateScriptCode ->
                "Duplicate legal entity script variant for script code '${error.scriptCode}'"
        }

    private fun describeSiteCreateError(error: SiteCreateError): String =
        when (error) {
            is UnresolvableLegalEntity -> "Legal entity ${error.bpn} not found"
            is UnresolvableAddress -> "Address ${error.bpn} not found"
            is LegalAddressAlreadyMainAddress -> "Legal address already is the main address of site ${error.bpnSite}"
            is ScriptVariantNotCoveredByAddress -> describeMainAddressNotCovered(error)
            is SiteContentError -> describeSiteContentError(error)
            is AddressContentError -> describeAddressContentError(error)
        }

    private fun describeSiteUpdateError(error: SiteUpdateError): String =
        when (error) {
            is UnresolvableSite -> "Site ${error.bpn} not found"
            is ScriptVariantNotCoveredByAddress -> describeMainAddressNotCovered(error)
            is SiteContentError -> describeSiteContentError(error)
            is AddressContentError -> describeAddressContentError(error)
        }

    private fun describeLegalAddressNotCovered(error: ScriptVariantNotCoveredByAddress): String =
        "Script code '${error.scriptCode}' is not covered by the legal address"

    private fun describeMainAddressNotCovered(error: ScriptVariantNotCoveredByAddress): String =
        "Script code '${error.scriptCode}' is not covered by the site main address"

    private fun describeSiteContentError(error: SiteContentError): String =
        when (error) {
            SiteContentError.NameMissing -> "Site has no name"
            SiteContentError.ConfidenceCriteriaMissing -> "Site is missing confidence criteria"
            is SiteContentError.ScriptCodeNotFound -> "Script code '${error.scriptCode}' is not known"
            is SiteContentError.ScriptVariantNameMissing -> "Script variant ${error.index} has no site name"
            is SiteContentError.ScriptVariantDuplicateScriptCode -> "Duplicate site script variant for script code '${error.scriptCode}'"
        }

    companion object {
        internal const val UNREPORTED_TASK_ERROR = "Unknown golden record process error during Pool communication"
        internal const val UNEXPECTED_TASK_ERROR = "An unexpected error occurred during Pool update"
    }
}
