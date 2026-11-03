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

package org.eclipse.tractusx.bpdm.gate.service.operation.businesspartner

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.logging.PersistedChangeLogger
import org.eclipse.tractusx.bpdm.common.logging.PersistedChangeType
import org.eclipse.tractusx.bpdm.common.model.DbTimestamp
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.gate.entity.ChangelogEntryDb
import org.eclipse.tractusx.bpdm.gate.entity.GoldenRecordType
import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.PostalAddressDb
import org.eclipse.tractusx.bpdm.gate.mapper.DbToParsedMapper
import org.eclipse.tractusx.bpdm.gate.mapper.ParsedToDbMapper
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerOutputContentParsed
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerOutputParsed
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.BusinessPartnerChangeRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.changelog.ChangelogType
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.eclipse.tractusx.bpdm.gate.service.operation.changelog.ChangelogCreateOperationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Writes the output stage of business partners.
 */
@Service
class BusinessPartnerOutputOperationService(
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val changelogCreateOperation: ChangelogCreateOperationService,
    private val dbToParsedMapper: DbToParsedMapper,
    private val parsedToDbMapper: ParsedToDbMapper,
    private val changeLogger: PersistedChangeLogger
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Creates or updates the output of each of [outputs] and returns every output in the same order, leaving those whose
     * data is unchanged untouched; the caller ensures that no two of [outputs] share a sharing state.
     */
    @Transactional
    fun write(outputs: List<BusinessPartnerOutputParsed>): List<BusinessPartnerDb> =
        outputs.map(::write)

    private fun write(parsed: BusinessPartnerOutputParsed): BusinessPartnerDb {
        val storedOutput = parsed.storedOutput
        return when {
            storedOutput == null -> create(parsed.data, parsed.sharingState)
            hasChanged(parsed.data, storedOutput) -> update(parsed.data, storedOutput)
            else -> storedOutput
        }
    }

    private fun create(content: BusinessPartnerOutputContentParsed, sharingState: SharingStateDb): BusinessPartnerDb {
        val output = BusinessPartnerDb.createEmpty(sharingState, StageType.Output)
        write(content, output)
        persist(output, ChangelogType.CREATE)
        return output
    }

    private fun update(content: BusinessPartnerOutputContentParsed, output: BusinessPartnerDb): BusinessPartnerDb {
        write(content, output)
        output.updatedAt = DbTimestamp.now()
        persist(output, ChangelogType.UPDATE)
        return output
    }

    private fun hasChanged(content: BusinessPartnerOutputContentParsed, storedOutput: BusinessPartnerDb) =
        dbToParsedMapper.toUpsertData(storedOutput) != content

    private fun persist(output: BusinessPartnerDb, changeType: ChangelogType) {
        val sharingState = output.sharingState
        businessPartnerRepository.save(output)
        changelogCreateOperation.record(
            ChangelogEntryDb(sharingState.externalId, sharingState.tenantBpnl, changeType, StageType.Output, GoldenRecordType.BusinessPartner)
        )
        logChange(sharingState, changeType)
    }

    private fun logChange(sharingState: SharingStateDb, changeType: ChangelogType) {
        val persistedChangeType = when (changeType) {
            ChangelogType.CREATE -> PersistedChangeType.Created
            ChangelogType.UPDATE -> PersistedChangeType.Updated
        }
        changeLogger.log(BusinessPartnerChangeRaw(persistedChangeType, sharingState.externalId, sharingState.tenantBpnl, BusinessPartnerChangeRaw.Aspect.Output))
        logger.debug { "Wrote business partner output ${sharingState.externalId} as $changeType" }
    }

    private fun write(content: BusinessPartnerOutputContentParsed, output: BusinessPartnerDb) {
        output.nameParts.replace(content.nameParts)
        output.roles.replace(content.roles)
        output.identifiers.replace(content.identifiers.map(parsedToDbMapper::toEntity))
        output.states.replace(content.states.map(parsedToDbMapper::toEntity))
        output.isOwnCompanyData = content.isOwnCompanyData

        output.bpnL = content.legalEntityBpn
        output.legalName = content.legalName
        output.shortName = content.shortName
        output.legalForm = content.legalForm
        output.ownershipUltimate = content.ownershipUltimate
        output.ultimateOwnerBpnl = content.ultimateOwnerBpnl
        output.legalEntityConfidence = parsedToDbMapper.toEntity(content.legalEntityConfidence)
        output.legalEntityUpdatedAt = content.legalEntityUpdatedAt
        output.legalEntityGoldenRecordRelations.replace(content.legalEntityGoldenRecordRelations.map(parsedToDbMapper::toLegalEntityGoldenRecordRelation))

        output.bpnS = content.siteBpn
        output.siteName = content.siteName
        output.siteConfidence = content.siteConfidence?.let(parsedToDbMapper::toEntity)
        output.siteUpdatedAt = content.siteUpdatedAt
        output.siteGoldenRecordRelations.replace(content.siteGoldenRecordRelations.map(parsedToDbMapper::toSiteGoldenRecordRelation))
        output.additionalSites.replace(content.additionalSites.map(parsedToDbMapper::toEntity))

        output.bpnA = content.addressBpn
        output.addressName = content.addressName
        output.addressConfidence = parsedToDbMapper.toEntity(content.addressConfidence)
        output.addressUpdatedAt = content.addressUpdatedAt
        output.addressGoldenRecordRelations.replace(content.addressGoldenRecordRelations.map(parsedToDbMapper::toAddressGoldenRecordRelation))
        writeAddress(content, output.postalAddress)

        parsedToDbMapper.writeScriptVariants(content.scriptVariants, output)
    }

    private fun writeAddress(content: BusinessPartnerOutputContentParsed, postalAddress: PostalAddressDb) {
        postalAddress.addressType = content.addressType
        postalAddress.physicalPostalAddress = parsedToDbMapper.toEntity(content.physicalPostalAddress)
        postalAddress.alternativePostalAddress = content.alternativePostalAddress?.let(parsedToDbMapper::toEntity)
    }
}
