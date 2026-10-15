/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.bpdm.gate.service.businesspartner.input.operation

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.gate.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.entity.*
import org.eclipse.tractusx.bpdm.gate.entity.generic.*
import org.eclipse.tractusx.bpdm.gate.mapper.record.BusinessPartnerInputContentMapper
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputContent
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputUpsertEntryResult
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputUpsertEntryResult.*
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputUpsertParsed
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputValues
import org.eclipse.tractusx.bpdm.gate.model.value.GeoCoordinate
import org.eclipse.tractusx.bpdm.gate.model.value.Street
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.eclipse.tractusx.bpdm.gate.service.ChangelogCreateService
import org.eclipse.tractusx.bpdm.gate.service.SharingStateService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Writes a parsed business partner into the input stage of its tenant.
 */
@Service
class BusinessPartnerInputUpsertOperationService(
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val sharingStateService: SharingStateService,
    private val changelogCreateService: ChangelogCreateService,
    private val contentMapper: BusinessPartnerInputContentMapper
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Creates or updates the input of [request] for [tenantBpnl] and resets its sharing state, resubmits an unchanged
     * input whose sharing failed, or skips a request that is outdated or changes nothing.
     */
    @Transactional
    fun upsert(request: BusinessPartnerInputUpsertParsed, tenantBpnl: String?): BusinessPartnerInputUpsertEntryResult {
        val sharingState = sharingStateService.getOrCreateState(request.externalId, tenantBpnl)
        val existing = businessPartnerRepository.findBySharingStateAndStage(sharingState, StageType.Input)

        val result = when {
            existing == null -> create(request, sharingState)
            isOutdated(request, existing) -> Skipped(request.externalId, SkipReason.Outdated)
            hasChanged(request, existing) -> update(request, existing)
            sharingState.sharingStateType == SharingStateType.Error -> resubmit(existing)
            else -> Skipped(request.externalId, SkipReason.Unchanged)
        }
        logger.debug { describe(result) }
        return result
    }

    private fun create(request: BusinessPartnerInputUpsertParsed, sharingState: SharingStateDb): Created {
        val input = BusinessPartnerDb.createEmpty(sharingState, StageType.Input)
        write(request, input)
        businessPartnerRepository.save(input)
        recordChange(input, ChangelogType.CREATE)
        return Created(input)
    }

    // The input is managed, so its changes are flushed with the transaction without an explicit save
    private fun update(request: BusinessPartnerInputUpsertParsed, input: BusinessPartnerDb): Updated {
        write(request, input)
        recordChange(input, ChangelogType.UPDATE)
        return Updated(input)
    }

    private fun resubmit(input: BusinessPartnerDb): Resubmitted {
        sharingStateService.setInitial(input.sharingState)
        return Resubmitted(input)
    }

    private fun isOutdated(request: BusinessPartnerInputUpsertParsed, existing: BusinessPartnerDb): Boolean {
        val requestedTimestamp = request.externalSequenceTimestamp ?: return false
        val existingTimestamp = existing.externalSequenceTimestamp ?: return false
        return !requestedTimestamp.isAfter(existingTimestamp)
    }

    private fun hasChanged(request: BusinessPartnerInputUpsertParsed, existing: BusinessPartnerDb) =
        contentMapper.toContent(existing) != request.content

    private fun recordChange(input: BusinessPartnerDb, changeType: ChangelogType) {
        val sharingState = input.sharingState
        changelogCreateService.record(
            ChangelogEntryDb(sharingState.externalId, sharingState.tenantBpnl, changeType, StageType.Input, GoldenRecordType.BusinessPartner)
        )
        sharingStateService.setInitial(sharingState)
    }

    private fun describe(result: BusinessPartnerInputUpsertEntryResult) =
        when (result) {
            is Created -> "Created business partner input ${result.input.toInputLogIdentifier()}"
            is Updated -> "Updated business partner input ${result.input.toInputLogIdentifier()}"
            is Resubmitted -> "Resubmitted unchanged business partner input ${result.input.toInputLogIdentifier()} after a sharing error"
            is Skipped -> "Skipped business partner input ${result.externalId}: ${result.reason}"
        }

    private fun write(request: BusinessPartnerInputUpsertParsed, input: BusinessPartnerDb) {
        val content = request.content
        input.externalSequenceTimestamp = request.externalSequenceTimestamp
        input.nameParts.replace(content.nameParts)
        input.roles.replace(content.roles)
        input.identifiers.replace(content.identifiers.map(::toEntity))
        input.states.replace(content.states.map(::toEntity))
        input.isOwnCompanyData = content.isOwnCompanyData

        input.bpnL = content.legalEntity.bpn
        input.legalName = content.legalEntity.legalName
        input.shortName = content.legalEntity.shortName
        input.legalForm = content.legalEntity.legalForm
        input.ownershipUltimate = content.legalEntity.ownershipUltimate
        input.ultimateOwnerBpnl = content.legalEntity.ultimateOwnerBpnl

        input.bpnS = content.site.bpn
        input.siteName = content.site.name
        input.additionalSites.replace(content.additionalSites.map { AdditionalSiteDb(bpn = it.bpn, name = it.name) })

        input.bpnA = content.address.bpn
        input.addressName = content.address.name
        writeAddress(content.address, input.postalAddress)

        syncScriptVariants(content.scriptVariants, input)
    }

    private fun writeAddress(address: BusinessPartnerInputContent.Address, postalAddress: PostalAddressDb) {
        postalAddress.addressType = address.addressType
        postalAddress.physicalPostalAddress = address.physicalPostalAddress?.let(::toEntity)
        postalAddress.alternativePostalAddress = address.alternativePostalAddress?.let(::toEntity)
    }

    // Updated in place by script code: replacing the list would insert before the orphans are deleted and break the
    // unique (script_code, business_partner_id) constraint
    private fun syncScriptVariants(scriptVariants: Collection<BusinessPartnerInputValues.ScriptVariant>, input: BusinessPartnerDb) {
        val existingByCode = input.scriptVariants.associateBy { it.scriptCode }
        input.scriptVariants.removeIf { existing -> scriptVariants.none { it.scriptCode == existing.scriptCode } }

        scriptVariants.forEach { scriptVariant ->
            val target = existingByCode[scriptVariant.scriptCode]
                ?: BusinessPartnerScriptVariantDb(scriptVariant.scriptCode, input).also { input.scriptVariants.add(it) }
            writeScriptVariant(scriptVariant, target)
        }
    }

    private fun writeScriptVariant(scriptVariant: BusinessPartnerInputValues.ScriptVariant, target: BusinessPartnerScriptVariantDb) {
        target.nameParts.replace(scriptVariant.nameParts)
        target.legalName = scriptVariant.legalName
        target.shortName = scriptVariant.shortName
        target.siteName = scriptVariant.siteName
        target.addressName = scriptVariant.addressName
        target.physicalAddress = with(scriptVariant.physicalAddress) {
            PhysicalPostalAddressScriptVariantDb(
                city = city,
                district = district,
                street = with(street) {
                    StreetScriptVariantDb(
                        name = name,
                        direction = direction,
                        namePrefix = namePrefix,
                        additionalNamePrefix = additionalNamePrefix,
                        nameSuffix = nameSuffix,
                        additionalNameSuffix = additionalNameSuffix
                    )
                },
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door
            )
        }
        target.alternativeAddress = scriptVariant.alternativeAddressCity?.let { AlternativePostalAddressScriptVariantDb(city = it) }
    }

    private fun toEntity(identifier: BusinessPartnerInputContent.Identifier) =
        IdentifierDb(type = identifier.type, value = identifier.value, issuingBody = identifier.issuingBody, BusinessPartnerType.GENERIC)

    private fun toEntity(state: BusinessPartnerInputContent.State) =
        StateDb(type = state.type, validFrom = state.validFrom, validTo = state.validTo, businessPartnerTyp = state.businessPartnerType)

    private fun toEntity(address: BusinessPartnerInputValues.PhysicalPostalAddress) =
        PhysicalPostalAddressDb(
            geographicCoordinates = address.geographicCoordinates?.let(::toEntity),
            country = address.country,
            administrativeAreaLevel1 = address.administrativeAreaLevel1,
            administrativeAreaLevel2 = address.administrativeAreaLevel2,
            administrativeAreaLevel3 = address.administrativeAreaLevel3,
            postalCode = address.postalCode,
            city = address.city,
            district = address.district,
            street = address.street?.let(::toEntity),
            companyPostalCode = address.companyPostalCode,
            industrialZone = address.industrialZone,
            building = address.building,
            floor = address.floor,
            door = address.door,
            taxJurisdictionCode = address.taxJurisdictionCode
        )

    private fun toEntity(address: BusinessPartnerInputValues.AlternativePostalAddress) =
        AlternativePostalAddressDb(
            geographicCoordinates = address.geographicCoordinates?.let(::toEntity),
            country = address.country,
            administrativeAreaLevel1 = address.administrativeAreaLevel1,
            postalCode = address.postalCode,
            city = address.city,
            deliveryServiceType = address.deliveryServiceType,
            deliveryServiceQualifier = address.deliveryServiceQualifier,
            deliveryServiceNumber = address.deliveryServiceNumber
        )

    private fun toEntity(street: Street) =
        StreetDb(
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

    private fun toEntity(coordinate: GeoCoordinate) =
        GeographicCoordinateDb(latitude = coordinate.latitude, longitude = coordinate.longitude, altitude = coordinate.altitude)
}

/**
 * Returns the identifiers a log line names this business partner input by: its external ID and any BPNs it states.
 */
internal fun BusinessPartnerDb.toInputLogIdentifier(): String {
    val bpns = listOfNotNull(bpnL, bpnS, bpnA)
    return if (bpns.isEmpty()) sharingState.externalId else "${sharingState.externalId} (${bpns.joinToString("/")})"
}
