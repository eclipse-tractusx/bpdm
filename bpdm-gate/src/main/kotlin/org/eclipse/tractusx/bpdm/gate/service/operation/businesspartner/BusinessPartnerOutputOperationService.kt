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
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.gate.entity.ChangelogEntryDb
import org.eclipse.tractusx.bpdm.gate.entity.GoldenRecordType
import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.mapper.entity.BusinessPartnerOutputEntityMapper
import org.eclipse.tractusx.bpdm.gate.model.businesspartner.BusinessPartnerChange
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.BusinessPartnerOutputParsed
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.eclipse.tractusx.bpdm.gate.service.operation.changelog.ChangelogCreateOperationService
import org.eclipse.tractusx.bpdm.gate.util.BusinessPartnerComparisonUtil
import org.eclipse.tractusx.bpdm.gate.util.BusinessPartnerCopyUtil
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Writes the output stage of business partners.
 */
@Service
class BusinessPartnerOutputOperationService(
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val changelogCreateOperation: ChangelogCreateOperationService,
    private val entityMapper: BusinessPartnerOutputEntityMapper,
    private val compareUtil: BusinessPartnerComparisonUtil,
    private val copyUtil: BusinessPartnerCopyUtil,
    private val changeLogger: PersistedChangeLogger
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Creates or updates the output of each of [outputs] and returns every output in the same order, leaving those whose
     * data is unchanged untouched.
     */
    @Transactional
    fun write(outputs: List<BusinessPartnerOutputParsed>): List<BusinessPartnerDb> =
        outputs.map(::write)

    private fun write(parsed: BusinessPartnerOutputParsed): BusinessPartnerDb {
        val sharingState = parsed.sharingState
        val storedOutput = parsed.storedOutput
        val incoming = entityMapper.toEntity(parsed.data, sharingState)
        if (storedOutput != null && !compareUtil.hasChanges(incoming, storedOutput)) return storedOutput

        val changeType = if (storedOutput == null) ChangelogType.CREATE else ChangelogType.UPDATE
        val output = storedOutput ?: BusinessPartnerDb.createEmpty(sharingState, StageType.Output)
        changelogCreateOperation.record(
            ChangelogEntryDb(sharingState.externalId, sharingState.tenantBpnl, changeType, StageType.Output, GoldenRecordType.BusinessPartner)
        )
        copyUtil.copyValues(incoming, output)
        businessPartnerRepository.save(output)
        logChange(sharingState, changeType)
        return output
    }

    private fun logChange(sharingState: SharingStateDb, changeType: ChangelogType) {
        val persistedChangeType = when (changeType) {
            ChangelogType.CREATE -> PersistedChangeType.Created
            ChangelogType.UPDATE -> PersistedChangeType.Updated
        }
        changeLogger.log(BusinessPartnerChange(persistedChangeType, sharingState.externalId, sharingState.tenantBpnl, BusinessPartnerChange.Aspect.Output))
        logger.debug { "Wrote business partner output ${sharingState.externalId} as $changeType" }
    }
}
