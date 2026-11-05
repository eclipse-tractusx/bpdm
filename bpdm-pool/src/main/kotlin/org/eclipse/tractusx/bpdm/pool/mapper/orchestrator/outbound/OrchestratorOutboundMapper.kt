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

package org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.outbound

import org.eclipse.tractusx.bpdm.common.dto.BusinessStateDates
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.model.error.*
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
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.GoldenRecordTaskError
import org.eclipse.tractusx.bpdm.pool.model.raw.task.BpnReferenceKind
import org.eclipse.tractusx.bpdm.pool.model.raw.task.BusinessStateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.GoldenRecordPassThroughRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.NamePartKind
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedAddressRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedAddressRelationKind
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedGeoCoordinateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedIdentifierRaw
import org.eclipse.tractusx.orchestrator.api.model.*
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
    fun toResultEntry(result: GoldenRecordTaskResultRecord): TaskStepResultEntryDto =
        when (result) {
            is GoldenRecordTaskResultRecord.Resolved -> TaskStepResultEntryDto(result.taskId, toBusinessPartner(result))
            is GoldenRecordTaskResultRecord.Rejected -> toErrorEntry(result.taskId, result.errors.map { toDescription(it) })
            is GoldenRecordTaskResultRecord.Failed -> toErrorEntry(result.taskId, listOf("An unexpected error occurred during Pool update"))
        }

    private fun toErrorEntry(taskId: String, descriptions: List<String>) =
        TaskStepResultEntryDto(
            taskId = taskId,
            businessPartner = BusinessPartner.empty,
            errors = descriptions.map { TaskErrorDto(type = TaskErrorType.Unspecified, description = it) }
        )

    private fun toBusinessPartner(result: GoldenRecordTaskResultRecord.Resolved): BusinessPartner {
        val legalEntity = toLegalEntity(result.legalEntity, result.isLegalEntityWritten)
        val site = result.site?.let { toSite(it, result.isSiteWritten) }
        // A site whose main address is the legal address states no main address of its own; the legal address is reported
        // as the site wrote it, which is newer than the legal entity's view when only the site changed.
        val siteSharesLegalAddress = result.site != null && result.site.mainAddress.bpn == result.legalEntity.legalAddress.bpn

        return BusinessPartner(
            nameParts = result.passThrough.nameParts.map { NamePart(it.name, toNamePartType(it.type)) },
            owningCompany = result.passThrough.owningCompany,
            uncategorized = toUncategorized(result.passThrough),
            legalEntity = if (siteSharesLegalAddress) legalEntity.copy(legalAddress = site!!.siteMainAddress!!) else legalEntity,
            site = if (siteSharesLegalAddress) site!!.copy(siteMainAddress = null) else site,
            additionalAddress = result.additionalAddress?.let { toAddressWithScriptVariants(it) },
            additionalSites = result.additionalSites.map { AdditionalSite(BpnReference(it.bpn, null, BpnReferenceType.Bpn), it.name) }
        )
    }

    private fun toLegalEntity(record: LegalEntityRecord, isWritten: Boolean): LegalEntity =
        LegalEntity(
            bpnReference = toBpnReference(record.bpn),
            legalName = record.legalName,
            legalShortName = record.legalShortName,
            legalForm = record.legalForm?.technicalKey,
            identifiers = record.identifiers.map { Identifier(it.value, it.typeTechnicalKey, it.issuingBody) },
            states = record.states.map { toState(it) },
            confidenceCriteria = toConfidenceCriteria(record.confidenceCriteria),
            isParticipantData = record.isDataSpaceParticipant,
            hasChanged = isWritten,
            ownershipUltimate = record.ownershipUltimate,
            ultimateOwnerBpnl = record.ultimateOwnerBpnl,
            legalAddress = toPostalAddress(record.legalAddress, isWritten),
            scriptVariants = record.scriptVariants.map {
                LegalEntityScriptVariant(it.scriptCode, it.legalName, it.shortName, toPostalScriptVariant(it.legalAddress))
            },
            goldenRecordRelations = record.relations
                .distinctBy { Triple(it.type, it.sourceBpnl, it.targetBpnl) }
                .map { LegalEntityGoldenRecordRelation(toRelationType(it.type), it.sourceBpnl, it.targetBpnl) },
            updatedAt = record.updatedAt
        )

    private fun toSite(record: SiteRecord, isWritten: Boolean): Site =
        Site(
            bpnReference = toBpnReference(record.bpn),
            siteName = record.name,
            states = record.states.map { toState(it) },
            confidenceCriteria = toConfidenceCriteria(record.confidenceCriteria),
            hasChanged = isWritten,
            siteMainAddress = toPostalAddress(record.mainAddress, isWritten),
            scriptVariants = record.scriptVariants.map { SiteScriptVariant(it.scriptCode, it.name, toPostalScriptVariant(it.mainAddress)) },
            goldenRecordRelations = record.relations
                .distinctBy { Triple(it.type, it.sourceBpns, it.targetBpns) }
                .map { SiteGoldenRecordRelation(toRelationType(it.type), it.sourceBpns, it.targetBpns) },
            updatedAt = record.updatedAt
        )

    // An additional address stated by a task is always written.
    private fun toAddressWithScriptVariants(record: AddressRecord): PostalAddressWithScriptVariants =
        PostalAddressWithScriptVariants(
            postalProperties = toPostalAddress(record, isWritten = true),
            scriptVariants = record.scriptVariants.map { PostalAddressScriptVariantWithScriptCode(it.scriptCode, toPostalScriptVariant(it)) }
        )

    private fun toPostalAddress(record: AddressRecord, isWritten: Boolean): PostalAddress =
        PostalAddress(
            bpnReference = toBpnReference(record.bpn),
            addressName = record.name,
            identifiers = record.identifiers.map { Identifier(it.value, it.typeTechnicalKey, null) },
            states = record.states.map { toState(it) },
            confidenceCriteria = toConfidenceCriteria(record.confidenceCriteria),
            physicalAddress = toPhysicalAddress(record.physicalPostalAddress),
            alternativeAddress = record.alternativePostalAddress?.let { toAlternativeAddress(it) },
            hasChanged = isWritten,
            goldenRecordRelations = record.relations
                .distinctBy { Triple(it.type, it.sourceBpna, it.targetBpna) }
                .map { AddressGoldenRecordRelation(toRelationType(it.type), it.sourceBpna, it.targetBpna) },
            updatedAt = record.updatedAt
        )

    private fun toPhysicalAddress(record: PhysicalPostalAddressRecord): PhysicalAddress =
        PhysicalAddress(
            geographicCoordinates = record.geographicCoordinates?.let { toGeoCoordinate(it) } ?: GeoCoordinate.empty,
            country = record.country.countryCode,
            administrativeAreaLevel1 = record.administrativeAreaLevel1?.code,
            administrativeAreaLevel2 = record.administrativeAreaLevel2,
            administrativeAreaLevel3 = record.administrativeAreaLevel3,
            postalCode = record.postalCode,
            city = record.city,
            district = record.district,
            street = record.street?.let {
                Street(
                    it.name, it.houseNumber, it.houseNumberSupplement, it.milestone, it.direction, it.namePrefix,
                    it.additionalNamePrefix, it.nameSuffix, it.additionalNameSuffix
                )
            } ?: Street.empty,
            companyPostalCode = record.companyPostalCode,
            industrialZone = record.industrialZone,
            building = record.building,
            floor = record.floor,
            door = record.door,
            taxJurisdictionCode = record.taxJurisdictionCode
        )

    private fun toAlternativeAddress(record: AlternativePostalAddressRecord): AlternativeAddress =
        AlternativeAddress(
            geographicCoordinates = record.geographicCoordinates?.let { toGeoCoordinate(it) } ?: GeoCoordinate.empty,
            country = record.country.countryCode,
            administrativeAreaLevel1 = record.administrativeAreaLevel1?.code,
            postalCode = record.postalCode,
            city = record.city,
            deliveryServiceType = record.deliveryServiceType,
            deliveryServiceQualifier = record.deliveryServiceQualifier,
            deliveryServiceNumber = record.deliveryServiceNumber
        )

    private fun toGeoCoordinate(record: GeoCoordinateRecord): GeoCoordinate =
        GeoCoordinate(record.longitude, record.latitude, record.altitude)

    private fun toPostalScriptVariant(record: AddressScriptVariantRecord): PostalAddressScriptVariant =
        PostalAddressScriptVariant(
            addressName = record.name,
            physicalAddress = with(record.physicalAddress) {
                PhysicalAddressScriptVariant(
                    city = city,
                    district = district,
                    street = street?.let {
                        StreetScriptVariant(it.name, it.direction, it.namePrefix, it.additionalNamePrefix, it.nameSuffix, it.additionalNameSuffix)
                    } ?: StreetScriptVariant.empty,
                    industrialZone = industrialZone,
                    building = building,
                    floor = floor,
                    door = door
                )
            },
            alternativeAddress = record.alternativeAddress?.let { AlternativeAddressScriptVariant(it.city) }
        )

    private fun toState(record: BusinessStateRecord): BusinessState =
        BusinessState(
            validFrom = BusinessStateDates.reportValidFrom(record.validFrom).toUtcInstant(),
            validTo = BusinessStateDates.reportValidTo(record.validTo).toUtcInstant(),
            type = record.type
        )

    private fun toConfidenceCriteria(record: ConfidenceCriteriaRecord): ConfidenceCriteria =
        ConfidenceCriteria(
            sharedByOwner = record.sharedByOwner,
            checkedByExternalDataSource = record.checkedByExternalDataSource,
            numberOfSharingMembers = record.numberOfSharingMembers,
            lastConfidenceCheckAt = record.lastConfidenceCheckAt.toUtcInstant(),
            nextConfidenceCheckAt = record.nextConfidenceCheckAt.toUtcInstant(),
            confidenceLevel = record.confidenceLevel
        )

    private fun toRelationType(type: LegalEntityRelationTypeV7): LegalEntityGoldenRecordRelationType =
        when (type) {
            LegalEntityRelationTypeV7.IsAlternativeHeadquarterFor -> LegalEntityGoldenRecordRelationType.IsAlternativeHeadquarterFor
            LegalEntityRelationTypeV7.IsManagedBy -> LegalEntityGoldenRecordRelationType.IsManagedBy
            LegalEntityRelationTypeV7.IsOwnedBy -> LegalEntityGoldenRecordRelationType.IsOwnedBy
            LegalEntityRelationTypeV7.IsReplacedBy -> LegalEntityGoldenRecordRelationType.IsReplacedBy
        }

    private fun toRelationType(type: SiteRelationTypeV7): SiteGoldenRecordRelationType =
        when (type) {
            SiteRelationTypeV7.IsReplacedBy -> SiteGoldenRecordRelationType.IsReplacedBy
        }

    private fun toRelationType(type: AddressRelationTypeV7): AddressGoldenRecordRelationType =
        when (type) {
            AddressRelationTypeV7.IsReplacedBy -> AddressGoldenRecordRelationType.IsReplacedBy
        }

    private fun toBpnReference(bpn: String) = BpnReference(bpn, null, BpnReferenceType.Bpn)

    private fun LocalDateTime.toUtcInstant() = toInstant(ZoneOffset.UTC)

    private fun toNamePartType(kind: NamePartKind): NamePartType =
        when (kind) {
            NamePartKind.LegalName -> NamePartType.LegalName
            NamePartKind.ShortName -> NamePartType.ShortName
            NamePartKind.LegalForm -> NamePartType.LegalForm
            NamePartKind.SiteName -> NamePartType.SiteName
            NamePartKind.AddressName -> NamePartType.AddressName
        }

    private fun toUncategorized(passThrough: GoldenRecordPassThroughRaw): UncategorizedProperties =
        with(passThrough.uncategorized) {
            UncategorizedProperties(
                nameParts = nameParts,
                identifiers = identifiers.map { toIdentifier(it) },
                states = states.map { toState(it) },
                address = address?.let { toUncategorizedAddress(it) }
            )
        }

    private fun toIdentifier(raw: UncategorizedIdentifierRaw) = Identifier(raw.value, raw.type, raw.issuingBody)

    private fun toState(raw: BusinessStateRaw) = BusinessState(raw.validFrom, raw.validTo, raw.type)

    private fun toUncategorizedAddress(raw: UncategorizedAddressRaw): PostalAddressWithScriptVariants =
        PostalAddressWithScriptVariants(
            postalProperties = PostalAddress(
                bpnReference = BpnReference(
                    referenceValue = raw.bpnReferenceValue,
                    desiredBpn = raw.bpnReferenceDesiredBpn,
                    referenceType = raw.bpnReferenceKind?.let {
                        when (it) {
                            BpnReferenceKind.Bpn -> BpnReferenceType.Bpn
                            BpnReferenceKind.RequestIdentifier -> BpnReferenceType.BpnRequestIdentifier
                        }
                    }
                ),
                addressName = raw.addressName,
                identifiers = raw.identifiers.map { toIdentifier(it) },
                states = raw.states.map { toState(it) },
                confidenceCriteria = with(raw.confidenceCriteria) {
                    ConfidenceCriteria(sharedByOwner, checkedByExternalDataSource, numberOfSharingMembers, lastConfidenceCheckAt, nextConfidenceCheckAt, confidenceLevel)
                },
                physicalAddress = with(raw.physicalAddress) {
                    PhysicalAddress(
                        geographicCoordinates = toGeoCoordinate(geographicCoordinates),
                        country = country,
                        administrativeAreaLevel1 = administrativeAreaLevel1,
                        administrativeAreaLevel2 = administrativeAreaLevel2,
                        administrativeAreaLevel3 = administrativeAreaLevel3,
                        postalCode = postalCode,
                        city = city,
                        district = district,
                        street = with(street) {
                            Street(name, houseNumber, houseNumberSupplement, milestone, direction, namePrefix, additionalNamePrefix, nameSuffix, additionalNameSuffix)
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
                    AlternativeAddress(
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
                        UncategorizedAddressRelationKind.IsReplacedBy -> AddressGoldenRecordRelationType.IsReplacedBy
                    }
                    AddressGoldenRecordRelation(type, it.sourceBpn, it.targetBpn)
                },
                updatedAt = raw.updatedAt
            ),
            scriptVariants = raw.scriptVariants.map { variant ->
                PostalAddressScriptVariantWithScriptCode(
                    scriptCode = variant.scriptCode,
                    postalProperties = PostalAddressScriptVariant(
                        addressName = variant.addressName,
                        physicalAddress = with(variant.physicalAddress) {
                            PhysicalAddressScriptVariant(
                                city = city,
                                district = district,
                                street = with(street) {
                                    StreetScriptVariant(name, direction, namePrefix, additionalNamePrefix, nameSuffix, additionalNameSuffix)
                                },
                                industrialZone = industrialZone,
                                building = building,
                                floor = floor,
                                door = door
                            )
                        },
                        alternativeAddress = variant.alternativeAddress?.let { AlternativeAddressScriptVariant(it.city) }
                    )
                )
            }
        )

    private fun toGeoCoordinate(raw: UncategorizedGeoCoordinateRaw) = GeoCoordinate(raw.longitude, raw.latitude, raw.altitude)

    private fun toDescription(error: GoldenRecordTaskError): String =
        when (error) {
            GoldenRecordTaskError.StateTypeMissing -> "Business Partner state type is null"
            is GoldenRecordTaskError.UnchangedLegalEntityNotFound -> "Legal entity with specified BPNL ${error.bpn} not found"
            is GoldenRecordTaskError.LegalEntityCreateInvalid -> describeLegalEntityCreateError(error.error)
            is GoldenRecordTaskError.LegalEntityUpdateInvalid -> describeLegalEntityUpdateError(error.error)
            is GoldenRecordTaskError.UnchangedSiteNotFound -> "Main address is null"
            GoldenRecordTaskError.SiteNotInLegalEntity -> "The legal entity is not the parent of the site"
            GoldenRecordTaskError.SiteMainAddressIsLegalAddress -> "The site main address is the legal address and must not be stated separately"
            is GoldenRecordTaskError.SiteCreateInvalid -> describeSiteCreateError(error.error)
            is GoldenRecordTaskError.SiteUpdateInvalid -> describeSiteUpdateError(error.error)
            GoldenRecordTaskError.AdditionalAddressNotInLegalEntity -> "The legal entity is not the parent of the additional address"
            is GoldenRecordTaskError.AdditionalAddressCreateInvalid -> "Errors on creating Address: ${describeAddressContentError(error.error)}"
            is GoldenRecordTaskError.AdditionalAddressUpdateInvalid -> "Errors on updating Address: ${describeAddressUpdateError(error.error)}"
            is GoldenRecordTaskError.AdditionalSiteCreateInvalid -> describeSiteContentError(error.error)
            is GoldenRecordTaskError.AdditionalSiteInvalid -> describeAddressSiteMembershipError(error.error)
            is GoldenRecordTaskError.AdditionalSiteOfOtherLegalEntity -> "Site ${error.siteBpn} does not belong to legal entity of this business partner"
            is GoldenRecordTaskError.CoverageStillNeeded -> describeScriptVariantCoverageError(error.error)
        }

    // Each description is named after the error type it takes rather than overloading one name: the content and coverage
    // errors subtype several node error interfaces each, so overloads would resolve on static type alone and a branch
    // delegating to the wrong one would still compile, silently emitting another node's wording.
    private fun describeAddressSiteMembershipError(error: AddressSiteMembershipParseError): String =
        when (error) {
            is UnresolvableAddress -> "Address ${error.bpn} not found"
            is UnresolvableSite -> "Site ${error.bpn} not found"
            is SiteNotInAddressLegalEntity -> "Site ${error.siteBpn} does not belong to legal entity ${error.legalEntityBpn}"
            is SiteMainAddressOmitted -> "Site ${error.siteBpn} has this address as its main address and must be stated"
        }

    private fun describeAddressUpdateError(error: AddressUpdateParseError): String =
        when (error) {
            is ScriptVariantCoverageParseError -> describeScriptVariantCoverageError(error)
            is UnresolvableAddress -> "Address ${error.bpn} not found"
            is SiteMainAddressOmitted -> "Site ${error.siteBpn} has this address as its main address and must be stated"
            is AddressContentParseError -> describeAddressContentError(error)
            is UnresolvableSite -> "Site parent ${error.bpn} not found"
            is SiteNotInAddressLegalEntity -> "Site ${error.siteBpn} does not belong to legal entity ${error.legalEntityBpn}"
        }

    private fun describeAddressContentError(error: AddressContentParseError): String =
        when (error) {
            is AddressFieldParseError -> describeAddressFieldError(error)
            is AddressMetadataParseError -> describeAddressMetadataError(error)
            is AddressConstraintParseError -> describeAddressConstraintError(error)
            is AddressScriptVariantParseError -> describeAddressScriptVariantError(error)
        }

    private fun describeAddressFieldError(error: AddressFieldParseError): String =
        when (error) {
            AddressFieldParseError.PhysicalCountryMissing -> "Physical Address has no country"
            AddressFieldParseError.PhysicalCityMissing -> "Physical Address has no city"
            AddressFieldParseError.AlternativeCountryMissing -> "Alternative Address has no country"
            AddressFieldParseError.AlternativeCityMissing -> "Alternative Address has no city"
            AddressFieldParseError.AlternativeDeliveryServiceTypeMissing -> "Alternative Address has no delivery service type"
            AddressFieldParseError.AlternativeDeliveryServiceNumberMissing -> "Alternative Address has no delivery service number"
            AddressFieldParseError.ConfidenceCriteriaMissing -> "Logistic address is missing confidence criteria"
            is AddressFieldParseError.IdentifierValueMissing -> "Identifier value is null"
            is AddressFieldParseError.IdentifierTypeMissing -> "Identifier type is null"
            is AddressFieldParseError.StateTypeMissing -> "Business Partner state type is null"
        }

    private fun describeAddressMetadataError(error: AddressMetadataParseError): String =
        when (error) {
            is AddressMetadataParseError.IdentifierTypeNotFound -> "Address identifier type '${error.type}' is not known"
            is AddressMetadataParseError.PhysicalCountryNotFound -> "Country '${error.country}' in physical address is not known"
            is AddressMetadataParseError.AlternativeCountryNotFound -> "Country '${error.country}' in alternative address is not known"
            is AddressMetadataParseError.PhysicalRegionNotFound -> "Region '${error.regionCode}' in physical address is not known"
            is AddressMetadataParseError.AlternativeRegionNotFound -> "Region '${error.regionCode}' in alternative address is not known"
            is AddressMetadataParseError.ScriptCodeNotFound -> "Script code '${error.scriptCode}' is not known"
        }

    private fun describeAddressConstraintError(error: AddressConstraintParseError): String =
        when (error) {
            is AddressConstraintParseError.IdentifiersTooMany -> "Too many identifiers: ${error.count} exceeds the allowed limit"
            is AddressConstraintParseError.DuplicateIdentifier -> "Duplicate identifier of type '${error.type}' with value '${error.value}'"
        }

    private fun describeAddressScriptVariantError(error: AddressScriptVariantParseError): String =
        when (error) {
            is AddressScriptVariantParseError.PhysicalCityMissing -> "Script variant ${error.index} has no city in its physical address"
            is AddressScriptVariantParseError.AlternativeCityMissing -> "Script variant ${error.index} has no city in its alternative address"
            is AddressScriptVariantParseError.DuplicateScriptCode -> "Duplicate address script variant for script code '${error.scriptCode}'"
        }

    private fun describeLegalEntityCreateError(error: LegalEntityCreateParseError): String =
        when (error) {
            is ScriptVariantCoverageParseError -> describeLegalAddressCoverageError(error)
            is LegalEntityContentParseError -> describeLegalEntityContentError(error)
            is AddressContentParseError -> describeAddressContentError(error)
        }

    private fun describeLegalEntityUpdateError(error: LegalEntityUpdateParseError): String =
        when (error) {
            is UnresolvableLegalEntity -> "Legal entity ${error.bpn} not found"
            is MultipleUltimateOwnersInHierarchy ->
                "An ownership hierarchy can have at most one ultimate owner, but these legal entities are also flagged " +
                        "as ultimate owner: ${error.conflictingBpnls.joinToString(", ")}"
            is AlternativeHeadquarterCannotOwnUltimately ->
                "Legal entity ${error.bpnl} cannot carry the ultimate-owner flag because it is an alternative headquarter"
            is ScriptVariantCoverageParseError -> describeLegalAddressCoverageError(error)
            is LegalEntityContentParseError -> describeLegalEntityContentError(error)
            is AddressContentParseError -> describeAddressContentError(error)
        }

    private fun describeLegalEntityContentError(error: LegalEntityContentParseError): String =
        when (error) {
            LegalEntityContentParseError.NameMissing -> "Legal name is null"
            LegalEntityContentParseError.ConfidenceCriteriaMissing -> "Legal Entity has no confidence criteria"
            is LegalEntityContentParseError.LegalFormNotFound -> "Legal form '${error.legalForm}' is not known"
            is LegalEntityContentParseError.IdentifierValueMissing -> "Identifier value is null"
            is LegalEntityContentParseError.IdentifierTypeMissing -> "Identifier type is null"
            is LegalEntityContentParseError.IdentifierTypeNotFound -> "Legal entity identifier type '${error.type}' is not known"
            is LegalEntityContentParseError.IdentifiersTooMany -> "Too many identifiers: ${error.count} exceeds the allowed limit"
            is LegalEntityContentParseError.DuplicateIdentifier -> "Duplicate identifier of type '${error.type}' with value '${error.value}'"
            is LegalEntityContentParseError.ScriptCodeNotFound -> "Script code '${error.scriptCode}' is not known"
            is LegalEntityContentParseError.ScriptVariantLegalNameMissing -> "Script variant ${error.index} has no legal name"
            is LegalEntityContentParseError.ScriptVariantDuplicateScriptCode ->
                "Duplicate legal entity script variant for script code '${error.scriptCode}'"
        }

    private fun describeSiteCreateError(error: SiteCreateParseError): String =
        when (error) {
            is UnresolvableLegalEntity -> "Legal entity ${error.bpn} not found"
            is UnresolvableAddress -> "Address ${error.bpn} not found"
            is LegalAddressAlreadyMainAddress -> "Legal address already is the main address of site ${error.bpnSite}"
            is ScriptVariantCoverageParseError -> describeMainAddressCoverageError(error)
            is SiteContentParseError -> describeSiteContentError(error)
            is AddressContentParseError -> describeAddressContentError(error)
        }

    private fun describeSiteUpdateError(error: SiteUpdateParseError): String =
        when (error) {
            is UnresolvableSite -> "Site ${error.bpn} not found"
            is ScriptVariantCoverageParseError -> describeMainAddressCoverageError(error)
            is SiteContentParseError -> describeSiteContentError(error)
            is AddressContentParseError -> describeAddressContentError(error)
        }

    private fun describeScriptVariantCoverageError(error: ScriptVariantCoverageParseError): String =
        when (error) {
            is ScriptVariantNotCoveredByAddress -> "Script code '${error.scriptCode}' is not covered by the address"
            is ScriptVariantCoverageStillNeeded ->
                "Script code '${error.scriptCode}' must stay covered: business partner ${error.requiredByBpn} is named in that script"
        }

    private fun describeLegalAddressCoverageError(error: ScriptVariantCoverageParseError): String =
        when (error) {
            is ScriptVariantNotCoveredByAddress -> "Script code '${error.scriptCode}' is not covered by the legal address"
            is ScriptVariantCoverageStillNeeded -> describeScriptVariantCoverageError(error)
        }

    private fun describeMainAddressCoverageError(error: ScriptVariantCoverageParseError): String =
        when (error) {
            is ScriptVariantNotCoveredByAddress -> "Script code '${error.scriptCode}' is not covered by the site main address"
            is ScriptVariantCoverageStillNeeded -> describeScriptVariantCoverageError(error)
        }

    private fun describeSiteContentError(error: SiteContentParseError): String =
        when (error) {
            SiteContentParseError.NameMissing -> "Site has no name"
            SiteContentParseError.ConfidenceCriteriaMissing -> "Site is missing confidence criteria"
            is SiteContentParseError.ScriptCodeNotFound -> "Script code '${error.scriptCode}' is not known"
            is SiteContentParseError.ScriptVariantNameMissing -> "Script variant ${error.index} has no site name"
            is SiteContentParseError.ScriptVariantDuplicateScriptCode -> "Duplicate site script variant for script code '${error.scriptCode}'"
        }
}
